@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.tasteindia

import com.tasteindia.data.remote.ResponseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Protects against: blank ingredient rows, raw whitespace, malformed URLs, crashes on {"meals":null}. */
class ResponseParserTest {
    private val parser = ResponseParser()

    @Test
    fun ingredientsAreNormalisedOrderedAndBlankPairsDropped() {
        val detail = parser.details(fixture("lookup_1001.json")).single()

        assertEquals(
            listOf("Chicken" to "1.2 kg", "Onion" to "5 thinly sliced", "Salt" to null, "Ginger" to null, "Garam Masala" to "1 tsp"),
            detail.ingredients.map { it.name to it.measure },
        )
        // measure without ingredient (slot 6) must not produce a row
        assertTrue(detail.ingredients.none { it.measure == "2 cups" })
    }

    @Test
    fun instructionsTagsAndLinksAreCleaned() {
        val detail = parser.details(fixture("lookup_1001.json")).single()

        assertEquals(listOf("Heat the oil.", "Add onions and cook.", "Add chicken and simmer."), detail.instructions)
        assertEquals(listOf("Curry", "Spicy", "Main"), detail.tags)
        assertEquals("https://www.youtube.com/watch?v=abc", detail.videoUrl)
        assertNull(detail.sourceUrl) // "not a url" is rejected
        assertNull(detail.alternateDrink)
    }

    @Test
    fun partialRecordOmitsEmptyFieldsSafely() {
        val detail = parser.details(fixture("lookup_minimal.json")).single()

        assertEquals(1, detail.ingredients.size)
        assertTrue(detail.tags.isEmpty())
        assertNull(detail.videoUrl)
        assertNull(detail.category)
        assertNull(detail.thumbUrl)
    }

    @Test
    fun summariesSkipRowsWithoutUsableNameAndNullMealsIsEmpty() {
        assertEquals(listOf("1001", "1002", "1003", "1004"), parser.summaries(fixture("filter_indian.json")).map { it.id })
        assertTrue(parser.summaries(fixture("empty_meals.json")).isEmpty())
        assertTrue(parser.summaries("").isEmpty())
        assertEquals(listOf("Beef", "Chicken", "Vegetarian"), parser.names(fixture("list_categories.json"), "strCategory"))
    }
}
