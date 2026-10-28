package com.tasteindia.ui.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasteindia.data.MealRepository
import com.tasteindia.data.favourites.FavouritesStore
import com.tasteindia.data.remote.toUserMessage
import com.tasteindia.domain.FilterState
import com.tasteindia.domain.MealSummary
import com.tasteindia.domain.SortOrder
import com.tasteindia.domain.applyFilters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MealRowUi(val meal: MealSummary, val category: String?, val isFavourite: Boolean)

enum class ListStatus { Loading, Error, Content }

data class RecipesUiState(
    val status: ListStatus = ListStatus.Loading,
    val errorMessage: String? = null,
    val rows: List<MealRowUi> = emptyList(),
    val totalCount: Int = 0,
    val filterLoading: Boolean = false,
    val filterError: String? = null,
    val categories: List<String> = emptyList(),
    val ingredients: List<String> = emptyList(),
    val ingredientsLoading: Boolean = false,
    val ingredientsError: String? = null,
)

private data class FilterOptions(
    val categories: List<String> = emptyList(),
    val ingredients: List<String> = emptyList(),
    val ingredientsLoading: Boolean = false,
    val ingredientsError: String? = null,
)

private sealed interface BaseState {
    data object Loading : BaseState
    data class Error(val message: String) : BaseState
    data class Loaded(val meals: List<MealSummary>) : BaseState
}

private sealed interface RemoteIds {
    data object None : RemoteIds
    data object Loading : RemoteIds
    data class Ready(val categoryIds: Set<String>?, val ingredientIds: Set<String>?) : RemoteIds
    data class Failed(val message: String) : RemoteIds
}

private data class Inputs(
    val base: BaseState,
    val filters: FilterState,
    val remote: RemoteIds,
    val favouriteIds: Set<String>,
    val categoryByMeal: Map<String, String>,
)

/**
 * Source of truth for the Recipes screen.
 *  - [filters] (query/category/ingredient/favourites-only/sort) lives here and is mirrored to
 *    [SavedStateHandle] so it survives process death; it survives navigation because this ViewModel
 *    is scoped to the Recipes back-stack entry.
 *  - Remote category/ingredient id sets are fetched with flatMapLatest, so only the latest selection wins.
 */
@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
class RecipesViewModel(
    private val repository: MealRepository,
    private val favouritesStore: FavouritesStore,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _filters = MutableStateFlow(restoreFilters())
    /** Exposed separately and synchronously so the search TextField never lags behind typing. */
    val filters: StateFlow<FilterState> = _filters.asStateFlow()

    private val base = MutableStateFlow<BaseState>(BaseState.Loading)
    private val options = MutableStateFlow(FilterOptions())
    private val categoryByMeal = MutableStateFlow<Map<String, String>>(emptyMap())
    private val remoteRetry = MutableStateFlow(0)
    private var baseJob: Job? = null
    private var enrichJob: Job? = null
    private var ingredientsJob: Job? = null

    private val remoteIds: Flow<RemoteIds> = combine(
        _filters.map { it.category to it.ingredient }.distinctUntilChanged(),
        remoteRetry,
    ) { selection, _ -> selection }
        .flatMapLatest { (category, ingredient) -> remoteIdsFlow(category, ingredient) }

    val uiState: StateFlow<RecipesUiState> = combine(
        base, _filters, remoteIds, favouritesStore.favourites, categoryByMeal,
    ) { b, f, r, favourites, categories ->
        Inputs(b, f, r, favourites.mapTo(HashSet()) { it.id }, categories)
    }
        .combine(options) { inputs, opts -> buildState(inputs, opts) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RecipesUiState())

    init {
        loadAll()
    }

    // ---- UI events ----

    fun onQueryChange(query: String) = updateFilters { it.copy(query = query) }
    fun onCategorySelected(category: String?) = updateFilters { it.copy(category = category) }
    fun onIngredientSelected(ingredient: String?) = updateFilters { it.copy(ingredient = ingredient) }
    fun onFavouritesOnlyChange(enabled: Boolean) = updateFilters { it.copy(favouritesOnly = enabled) }
    fun onToggleSort() = updateFilters {
        it.copy(sort = if (it.sort == SortOrder.NameAsc) SortOrder.NameDesc else SortOrder.NameAsc)
    }

    /** Resets every filter but keeps the chosen sort order. */
    fun onClearAll() = updateFilters { FilterState(sort = it.sort) }

    fun onRetry() = loadAll()
    fun onRetryFilters() { remoteRetry.update { it + 1 } }

    fun onToggleFavourite(meal: MealSummary) {
        viewModelScope.launch { favouritesStore.toggle(meal) }
    }

    fun onIngredientPickerOpened() {
        if (options.value.ingredients.isNotEmpty() || ingredientsJob?.isActive == true) return
        loadIngredients()
    }

    // ---- loading ----

    private fun loadAll() {
        baseJob?.cancel()
        base.value = BaseState.Loading
        baseJob = viewModelScope.launch {
            try {
                val meals = repository.indianMeals()
                base.value = BaseState.Loaded(meals)
                enrichCategories(meals)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                base.value = BaseState.Error(e.toUserMessage())
            }
        }
        viewModelScope.launch {
            try {
                val categories = repository.categories()
                options.update { it.copy(categories = categories) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Category menu simply shows "unavailable"; retry re-attempts.
            }
        }
    }

    private fun loadIngredients() {
        options.update { it.copy(ingredientsLoading = true, ingredientsError = null) }
        ingredientsJob = viewModelScope.launch {
            try {
                val list = repository.ingredients()
                options.update { it.copy(ingredients = list, ingredientsLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                options.update { it.copy(ingredientsLoading = false, ingredientsError = e.toUserMessage()) }
            }
        }
    }

    fun onRetryIngredients() = loadIngredients()

    /**
     * Best-effort category enrichment through the repository's cached, de-duplicated, concurrency-bounded
     * lookups. Runs once per list load from the ViewModel, never from row composables.
     */
    private fun enrichCategories(meals: List<MealSummary>) {
        enrichJob?.cancel()
        enrichJob = viewModelScope.launch {
            meals.forEach { meal ->
                launch {
                    try {
                        repository.detail(meal.id).category?.let { category ->
                            categoryByMeal.update { it + (meal.id to category) }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Enrichment is optional; the row simply shows no category.
                    }
                }
            }
        }
    }

    private fun remoteIdsFlow(category: String?, ingredient: String?): Flow<RemoteIds> =
        if (category == null && ingredient == null) {
            flowOf(RemoteIds.None)
        } else {
            flow {
                emit(RemoteIds.Loading)
                val result: RemoteIds = try {
                    coroutineScope {
                        val c = category?.let { async { repository.idsForCategory(it) } }
                        val i = ingredient?.let { async { repository.idsForIngredient(it) } }
                        RemoteIds.Ready(c?.await(), i?.await())
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    RemoteIds.Failed(e.toUserMessage())
                }
                emit(result)
            }
        }

    // ---- filter persistence ----

    private fun updateFilters(transform: (FilterState) -> FilterState) {
        val next = transform(_filters.value)
        _filters.value = next
        savedState[KEY_QUERY] = next.query
        savedState[KEY_CATEGORY] = next.category
        savedState[KEY_INGREDIENT] = next.ingredient
        savedState[KEY_FAVOURITES_ONLY] = next.favouritesOnly
        savedState[KEY_SORT] = next.sort.name
    }

    private fun restoreFilters() = FilterState(
        query = savedState[KEY_QUERY] ?: "",
        category = savedState[KEY_CATEGORY],
        ingredient = savedState[KEY_INGREDIENT],
        favouritesOnly = savedState[KEY_FAVOURITES_ONLY] ?: false,
        sort = SortOrder.entries.firstOrNull { it.name == savedState.get<String>(KEY_SORT) } ?: SortOrder.NameAsc,
    )

    companion object {
        const val KEY_QUERY = "query"
        const val KEY_CATEGORY = "category"
        const val KEY_INGREDIENT = "ingredient"
        const val KEY_FAVOURITES_ONLY = "favouritesOnly"
        const val KEY_SORT = "sort"
    }
}

private fun buildState(inputs: Inputs, options: FilterOptions): RecipesUiState {
    val loaded = (inputs.base as? BaseState.Loaded)?.meals.orEmpty()
    val ready = inputs.remote as? RemoteIds.Ready
    val visible = when (inputs.remote) {
        RemoteIds.None, is RemoteIds.Ready ->
            applyFilters(loaded, inputs.filters, inputs.favouriteIds, ready?.categoryIds, ready?.ingredientIds)
        else -> emptyList()
    }
    return RecipesUiState(
        status = when (inputs.base) {
            BaseState.Loading -> ListStatus.Loading
            is BaseState.Error -> ListStatus.Error
            is BaseState.Loaded -> ListStatus.Content
        },
        errorMessage = (inputs.base as? BaseState.Error)?.message,
        rows = visible.map { MealRowUi(it, inputs.categoryByMeal[it.id], it.id in inputs.favouriteIds) },
        totalCount = loaded.size,
        filterLoading = inputs.remote == RemoteIds.Loading,
        filterError = (inputs.remote as? RemoteIds.Failed)?.message,
        categories = options.categories,
        ingredients = options.ingredients,
        ingredientsLoading = options.ingredientsLoading,
        ingredientsError = options.ingredientsError,
    )
}
