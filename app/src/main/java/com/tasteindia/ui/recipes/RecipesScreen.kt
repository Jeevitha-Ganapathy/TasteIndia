@file:OptIn(ExperimentalMaterial3Api::class)

package com.tasteindia.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasteindia.domain.FilterState
import com.tasteindia.domain.MealSummary
import com.tasteindia.ui.components.EmptyView
import com.tasteindia.ui.components.ErrorView
import com.tasteindia.ui.components.LoadingView
import com.tasteindia.ui.components.MealRow

/** All user intents of the Recipes screen as plain lambdas (no ViewModel/NavController inside composables). */
class RecipesActions(
    val onQueryChange: (String) -> Unit,
    val onCategorySelected: (String?) -> Unit,
    val onIngredientSelected: (String?) -> Unit,
    val onIngredientPickerOpened: () -> Unit,
    val onRetryIngredients: () -> Unit,
    val onFavouritesOnlyChange: (Boolean) -> Unit,
    val onToggleSort: () -> Unit,
    val onClearAll: () -> Unit,
    val onRetry: () -> Unit,
    val onRetryFilters: () -> Unit,
    val onToggleFavourite: (MealSummary) -> Unit,
    val onOpenMeal: (String) -> Unit,
)

@Composable
fun RecipesRoute(viewModel: RecipesViewModel, onOpenMeal: (String) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val actions = remember(viewModel, onOpenMeal) {
        RecipesActions(
            onQueryChange = viewModel::onQueryChange,
            onCategorySelected = viewModel::onCategorySelected,
            onIngredientSelected = viewModel::onIngredientSelected,
            onIngredientPickerOpened = viewModel::onIngredientPickerOpened,
            onRetryIngredients = viewModel::onRetryIngredients,
            onFavouritesOnlyChange = viewModel::onFavouritesOnlyChange,
            onToggleSort = viewModel::onToggleSort,
            onClearAll = viewModel::onClearAll,
            onRetry = viewModel::onRetry,
            onRetryFilters = viewModel::onRetryFilters,
            onToggleFavourite = viewModel::onToggleFavourite,
            onOpenMeal = onOpenMeal,
        )
    }
    RecipesScreen(state, filters, actions)
}

@Composable
fun RecipesScreen(state: RecipesUiState, filters: FilterState, actions: RecipesActions) {
    // rememberLazyListState is saveable, so scroll position is restored when coming back from Details.
    val listState = rememberLazyListState()
    var showIngredientPicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("TasteIndia · Indian recipes") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchField(
                query = filters.query,
                onQueryChange = actions.onQueryChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            FilterBar(
                filters = filters,
                categories = state.categories,
                actions = actions,
                onOpenIngredientPicker = {
                    actions.onIngredientPickerOpened()
                    showIngredientPicker = true
                },
            )
            when (state.status) {
                ListStatus.Loading -> LoadingView()
                ListStatus.Error -> ErrorView(state.errorMessage.orEmpty(), actions.onRetry)
                ListStatus.Content -> {
                    ResultSummary(state.rows.size, state.totalCount, filters, actions.onClearAll, actions)
                    when {
                        state.filterLoading -> LoadingView(label = "Applying filters…")
                        state.filterError != null -> ErrorView(state.filterError, actions.onRetryFilters)
                        state.rows.isEmpty() && filters.hasActiveFilters -> EmptyView(
                            title = "No recipes match",
                            message = "No Indian recipes match your search and filters.",
                            actionLabel = "Clear all",
                            onAction = actions.onClearAll,
                        )
                        state.rows.isEmpty() -> EmptyView(
                            title = "No recipes available",
                            message = "TheMealDB returned no Indian recipes.",
                            actionLabel = "Retry",
                            onAction = actions.onRetry,
                        )
                        else -> LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.rows, key = { it.meal.id }) { row ->
                                MealRow(
                                    row = row,
                                    onClick = { actions.onOpenMeal(row.meal.id) },
                                    onToggleFavourite = { actions.onToggleFavourite(row.meal) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showIngredientPicker) {
        IngredientPickerDialog(
            ingredients = state.ingredients,
            loading = state.ingredientsLoading,
            error = state.ingredientsError,
            selected = filters.ingredient,
            onSelect = {
                actions.onIngredientSelected(it)
                showIngredientPicker = false
            },
            onRetry = actions.onRetryIngredients,
            onDismiss = { showIngredientPicker = false },
        )
    }
}
