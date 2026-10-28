package com.tasteindia.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasteindia.data.favourites.FavouritesStore
import com.tasteindia.domain.MealSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavouritesUiState(val isLoaded: Boolean = false, val meals: List<MealSummary> = emptyList())

class FavouritesViewModel(private val store: FavouritesStore) : ViewModel() {

    val uiState: StateFlow<FavouritesUiState> = store.favourites
        .map { list -> FavouritesUiState(true, list.sortedBy { it.name.lowercase() }) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FavouritesUiState())

    fun onToggleFavourite(meal: MealSummary) {
        viewModelScope.launch { store.toggle(meal) }
    }
}
