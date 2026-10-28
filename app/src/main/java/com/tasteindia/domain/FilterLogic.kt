package com.tasteindia.domain

private val byNameIgnoreCase = Comparator<MealSummary> { a, b ->
    String.CASE_INSENSITIVE_ORDER.compare(a.name, b.name)
}

/**
 * Pure filtering + sorting.
 *
 * The Indian boundary is guaranteed by construction: we only ever iterate over [indianBase]
 * (filter.php?a=Indian). [categoryIds] / [ingredientIds] are *membership sets* from other
 * endpoints; ids in them that are not in the base set can never appear in the result.
 * A null set means "that filter is not active".
 */
fun applyFilters(
    indianBase: List<MealSummary>,
    filters: FilterState,
    favouriteIds: Set<String>,
    categoryIds: Set<String>?,
    ingredientIds: Set<String>?,
): List<MealSummary> {
    val query = filters.query.trim()
    val matches = indianBase.filter { meal ->
        (query.isEmpty() || meal.name.contains(query, ignoreCase = true)) &&
            (categoryIds == null || meal.id in categoryIds) &&
            (ingredientIds == null || meal.id in ingredientIds) &&
            (!filters.favouritesOnly || meal.id in favouriteIds)
    }
    return when (filters.sort) {
        SortOrder.NameAsc -> matches.sortedWith(byNameIgnoreCase)
        SortOrder.NameDesc -> matches.sortedWith(byNameIgnoreCase.reversed())
    }
}
