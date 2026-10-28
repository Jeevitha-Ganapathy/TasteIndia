package com.tasteindia.ui.navigation

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tasteindia.AppContainer
import com.tasteindia.TasteIndiaApplication
import com.tasteindia.ui.details.DetailsViewModel
import com.tasteindia.ui.favourites.FavouritesViewModel
import com.tasteindia.ui.recipes.RecipesViewModel

private fun CreationExtras.container(): AppContainer =
    (this[APPLICATION_KEY] as TasteIndiaApplication).container

object AppViewModels {
    val Recipes = viewModelFactory {
        initializer { container().let { RecipesViewModel(it.repository, it.favourites, createSavedStateHandle()) } }
    }
    val Details = viewModelFactory {
        initializer { container().let { DetailsViewModel(it.repository, it.favourites, createSavedStateHandle()) } }
    }
    val Favourites = viewModelFactory {
        initializer { FavouritesViewModel(container().favourites) }
    }
}
