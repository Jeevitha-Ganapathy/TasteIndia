package com.tasteindia.domain

/** Minimal meal used in lists and persisted as a favourite. Identity is [id] (idMeal). */
data class MealSummary(val id: String, val name: String, val thumbUrl: String?)

data class Ingredient(val name: String, val measure: String?)

/** Full, normalised recipe. Only non-blank values are populated. */
data class MealDetail(
    val id: String,
    val name: String,
    val thumbUrl: String?,
    val category: String?,
    val area: String?,
    val ingredients: List<Ingredient>,
    val instructions: List<String>,
    val tags: List<String>,
    val sourceUrl: String?,
    val videoUrl: String?,
    val imageSourceUrl: String?,
    val alternateDrink: String?,
) {
    fun toSummary() = MealSummary(id, name, thumbUrl)
}

enum class SortOrder { NameAsc, NameDesc }

data class FilterState(
    val query: String = "",
    val category: String? = null,
    val ingredient: String? = null,
    val favouritesOnly: Boolean = false,
    val sort: SortOrder = SortOrder.NameAsc,
) {
    /** Sort order is a view preference, not a filter, so it is not counted here. */
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() || category != null || ingredient != null || favouritesOnly
}
