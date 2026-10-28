@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.tasteindia

import androidx.lifecycle.SavedStateHandle
import com.tasteindia.data.MealRepository
import com.tasteindia.domain.SortOrder
import com.tasteindia.ui.recipes.ListStatus
import com.tasteindia.ui.recipes.RecipesViewModel
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipesViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val service = FakeMealService().apply {
        byCategory["Chicken"] = listOf(handi, butter, handi.copy(id = "9999", name = "Chicken Karaage"))
        byCategory["Vegetarian"] = listOf(dal, palak)
        categoryDelayMs["Chicken"] = 1_000
    }
    private val favourites = InMemoryFavouritesStore()

    private fun newVm(scope: kotlinx.coroutines.test.TestScope, handle: SavedStateHandle = SavedStateHandle()) =
        RecipesViewModel(MealRepository(service, scope.backgroundRepoScope()), favourites, handle)

    @Test
    fun loadsIndianBaseSetWithCountAndEnrichedCategories() = runTest {
        val vm = newVm(this)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(ListStatus.Content, state.status)
        assertEquals(4, state.totalCount)
        assertEquals(listOf("Butter Chicken", "Chicken Handi", "Dal Makhani", "Palak Paneer"), state.rows.map { it.meal.name })
        assertEquals("Chicken", state.rows.first().category) // legitimately loaded via cached detail lookup
    }

    @Test
    fun onlyLatestFilterSelectionUpdatesResults() = runTest {
        val vm = newVm(this)
        advanceUntilIdle()

        vm.onCategorySelected("Chicken")      // slow (1s)
        advanceTimeBy(100)
        vm.onCategorySelected("Vegetarian")   // fast
        advanceUntilIdle()

        assertEquals("Vegetarian", vm.filters.value.category)
        assertEquals(listOf("Dal Makhani", "Palak Paneer"), vm.uiState.value.rows.map { it.meal.name })
    }

    @Test
    fun categoryFilterStaysInsideIndianBoundaryAndClearAllKeepsSort() = runTest {
        val vm = newVm(this)
        advanceUntilIdle()
        vm.onToggleSort()
        vm.onCategorySelected("Chicken")
        advanceUntilIdle()

        assertEquals(listOf("Chicken Handi", "Butter Chicken"), vm.uiState.value.rows.map { it.meal.name })

        vm.onClearAll()
        advanceUntilIdle()
        assertEquals(4, vm.uiState.value.rows.size)
        assertEquals(SortOrder.NameDesc, vm.filters.value.sort)
        assertTrue(!vm.filters.value.hasActiveFilters)
    }

    @Test
    fun filtersAreRestoredFromSavedStateAndFavouritesPersistAcrossViewModels() = runTest {
        val handle = SavedStateHandle()
        val first = newVm(this, handle)
        first.onQueryChange("dal")
        first.onFavouritesOnlyChange(true)
        first.onToggleFavourite(dal)
        advanceUntilIdle()

        // "Recreated" ViewModel gets the same SavedStateHandle and the same favourites store.
        val second = newVm(this, handle)
        advanceUntilIdle()

        assertEquals("dal", second.filters.value.query)
        assertTrue(second.filters.value.favouritesOnly)
        assertEquals(listOf("Dal Makhani"), second.uiState.value.rows.map { it.meal.name })
        assertTrue(second.uiState.value.rows.single().isFavourite)
    }

    @Test
    fun providerErrorShowsMessageAndRetryRecovers() = runTest {
        service.indianError = IOException("boom")
        val vm = newVm(this)
        advanceUntilIdle()
        assertEquals(ListStatus.Error, vm.uiState.value.status)

        service.indianError = null
        vm.onRetry()
        advanceUntilIdle()
        assertEquals(ListStatus.Content, vm.uiState.value.status)
        assertEquals(4, vm.uiState.value.rows.size)
    }
}
