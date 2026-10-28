@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.tasteindia

import com.tasteindia.domain.FilterState
import com.tasteindia.domain.SortOrder
import com.tasteindia.domain.applyFilters
import org.junit.Assert.assertEquals
import org.junit.Test

/** Protects against: non-Indian meals leaking in via category/ingredient endpoints; wrong sort/search. */
class FilterLogicTest {

    private fun names(filters: FilterState, favs: Set<String> = emptySet(), cat: Set<String>? = null, ing: Set<String>? = null) =
        applyFilters(indianBase, filters, favs, cat, ing).map { it.name }

    @Test
    fun categoryIdsOutsideIndianBaseNeverAppear() {
        val chickenIds = setOf("1001", "1004", "9999") // 9999 is not in the Indian set
        assertEquals(listOf("Butter Chicken", "Chicken Handi"), names(FilterState(), cat = chickenIds))
    }

    @Test
    fun categoryAndIngredientAreIntersected() {
        val result = names(FilterState(), cat = setOf("1001", "1004"), ing = setOf("1004", "1002"))
        assertEquals(listOf("Butter Chicken"), result)
    }

    @Test
    fun searchIsCaseInsensitiveAndCombinesWithFavourites() {
        assertEquals(listOf("Butter Chicken", "Chicken Handi"), names(FilterState(query = "CHICKEN")))
        assertEquals(listOf("Chicken Handi"), names(FilterState(query = "chicken", favouritesOnly = true), favs = setOf("1001", "1002")))
    }

    @Test
    fun sortsBothDirectionsAndNoMatchesIsEmpty() {
        assertEquals(listOf("Butter Chicken", "Chicken Handi", "Dal Makhani", "Palak Paneer"), names(FilterState()))
        assertEquals(listOf("Palak Paneer", "Dal Makhani", "Chicken Handi", "Butter Chicken"), names(FilterState(sort = SortOrder.NameDesc)))
        assertEquals(emptyList<String>(), names(FilterState(query = "pizza")))
    }
}
