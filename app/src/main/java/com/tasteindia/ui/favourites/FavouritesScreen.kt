@file:OptIn(ExperimentalMaterial3Api::class)

package com.tasteindia.ui.favourites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasteindia.ui.components.EmptyView
import com.tasteindia.ui.components.LoadingView
import com.tasteindia.ui.components.MealRow
import com.tasteindia.ui.recipes.MealRowUi

@Composable
fun FavouritesRoute(viewModel: FavouritesViewModel, onOpenMeal: (String) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Favourites") }) }) { padding ->
        when {
            !state.isLoaded -> LoadingView(Modifier.padding(padding), "Loading favourites…")
            state.meals.isEmpty() -> EmptyView(
                title = "No favourites yet",
                message = "Tap the heart on any recipe to save it here. Favourites work offline.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.meals, key = { it.id }) { meal ->
                    MealRow(
                        row = MealRowUi(meal, category = null, isFavourite = true),
                        onClick = { onOpenMeal(meal.id) },
                        onToggleFavourite = { viewModel.onToggleFavourite(meal) },
                    )
                }
            }
        }
    }
}
