package com.tasteindia.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasteindia.data.MealRepository
import com.tasteindia.data.favourites.FavouritesStore
import com.tasteindia.data.remote.toUserMessage
import com.tasteindia.domain.MealDetail
import com.tasteindia.ui.navigation.Destinations
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data class Error(val message: String) : DetailsUiState
    data class Content(val detail: MealDetail, val isFavourite: Boolean) : DetailsUiState
}

class DetailsViewModel(
    private val repository: MealRepository,
    private val favouritesStore: FavouritesStore,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val mealId: String = checkNotNull(savedState[Destinations.ARG_MEAL_ID]) { "mealId route argument missing" }

    private val loadState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    private var loadJob: Job? = null

    val uiState: StateFlow<DetailsUiState> = combine(loadState, favouritesStore.favourites) { state, favourites ->
        if (state is DetailsUiState.Content) state.copy(isFavourite = favourites.any { it.id == mealId }) else state
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DetailsUiState.Loading)

    init {
        load()
    }

    fun onRetry() = load()

    fun onToggleFavourite() {
        val detail = (uiState.value as? DetailsUiState.Content)?.detail ?: return
        viewModelScope.launch { favouritesStore.toggle(detail.toSummary()) }
    }

    private fun load() {
        loadJob?.cancel()
        loadState.value = DetailsUiState.Loading
        loadJob = viewModelScope.launch {
            loadState.value = try {
                DetailsUiState.Content(repository.detail(mealId), isFavourite = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DetailsUiState.Error(e.toUserMessage())
            }
        }
    }
}
