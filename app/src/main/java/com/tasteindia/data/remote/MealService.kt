package com.tasteindia.data.remote

import com.tasteindia.domain.MealDetail
import com.tasteindia.domain.MealSummary

/** Seam between the app and TheMealDB. Tests replace this with a deterministic fake. */
interface MealService {
    suspend fun indianMeals(): List<MealSummary>
    suspend fun mealsByCategory(category: String): List<MealSummary>
    suspend fun mealsByIngredient(ingredient: String): List<MealSummary>
    suspend fun categories(): List<String>
    suspend fun ingredients(): List<String>
    /** Null when the API has no meal for [id]. */
    suspend fun detail(id: String): MealDetail?
}
