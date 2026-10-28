package com.tasteindia.ui.navigation

import android.net.Uri

/** Central route definitions. Details receives only the meal id, never a serialized model. */
object Destinations {
    const val RECIPES = "recipes"
    const val FAVOURITES = "favourites"
    const val ARG_MEAL_ID = "mealId"
    const val DETAILS = "details/{$ARG_MEAL_ID}"

    fun details(mealId: String) = "details/${Uri.encode(mealId)}"
}
