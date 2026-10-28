package com.tasteindia

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.tasteindia.data.MealRepository
import com.tasteindia.data.favourites.DataStoreFavouritesStore
import com.tasteindia.data.favourites.FavouritesStore
import com.tasteindia.data.remote.TheMealDbService

/** Manual dependency injection: swap this (or the ViewModel constructor args) in tests. */
interface AppContainer {
    val repository: MealRepository
    val favourites: FavouritesStore
}

private val Context.favouritesDataStore by preferencesDataStore(name = "favourites")

class DefaultAppContainer(context: Context) : AppContainer {
    override val repository: MealRepository = MealRepository(TheMealDbService.create())
    override val favourites: FavouritesStore = DataStoreFavouritesStore(context.applicationContext.favouritesDataStore)
}
