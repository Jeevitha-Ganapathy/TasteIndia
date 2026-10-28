@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.tasteindia

import com.tasteindia.data.favourites.FavouritesStore
import com.tasteindia.data.remote.MealService
import com.tasteindia.domain.MealDetail
import com.tasteindia.domain.MealSummary
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

fun fixture(name: String): String =
    checkNotNull(object {}.javaClass.classLoader?.getResource("fixtures/$name")) { "missing fixture $name" }.readText()

val handi = MealSummary("1001", "Chicken Handi", null)
val dal = MealSummary("1002", "Dal Makhani", null)
val palak = MealSummary("1003", "Palak Paneer", null)
val butter = MealSummary("1004", "Butter Chicken", null)
val indianBase = listOf(handi, dal, palak, butter)

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** Repository work runs on the test scheduler so virtual time controls it. */
fun TestScope.backgroundRepoScope() = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

class FakeMealService : MealService {
    var indian: List<MealSummary> = indianBase
    var indianError: Throwable? = null
    val byCategory = mutableMapOf<String, List<MealSummary>>()
    val categoryDelayMs = mutableMapOf<String, Long>()
    val categoryNames = listOf("Chicken", "Vegetarian")
    var detailDelayMs = 0L
    val detailCalls = AtomicInteger()
    private val running = AtomicInteger()
    val maxConcurrentDetails = AtomicInteger()

    override suspend fun indianMeals(): List<MealSummary> {
        indianError?.let { throw it }
        return indian
    }

    override suspend fun mealsByCategory(category: String): List<MealSummary> {
        delay(categoryDelayMs[category] ?: 0)
        return byCategory[category].orEmpty()
    }

    override suspend fun mealsByIngredient(ingredient: String): List<MealSummary> = emptyList()
    override suspend fun categories(): List<String> = categoryNames
    override suspend fun ingredients(): List<String> = listOf("Chicken", "Lentils")

    override suspend fun detail(id: String): MealDetail? {
        detailCalls.incrementAndGet()
        val now = running.incrementAndGet()
        maxConcurrentDetails.updateAndGet { maxOf(it, now) }
        try {
            delay(detailDelayMs)
        } finally {
            running.decrementAndGet()
        }
        val name = indianBase.firstOrNull { it.id == id }?.name ?: "Meal $id"
        return MealDetail(id, name, null, "Chicken", "Indian", emptyList(), emptyList(), emptyList(), null, null, null, null)
    }
}

class InMemoryFavouritesStore(initial: List<MealSummary> = emptyList()) : FavouritesStore {
    private val state = MutableStateFlow(initial)
    override val favourites = state
    override suspend fun toggle(meal: MealSummary) {
        state.value = if (state.value.any { it.id == meal.id }) state.value.filterNot { it.id == meal.id } else state.value + meal
    }
}
