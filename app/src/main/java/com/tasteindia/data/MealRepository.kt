package com.tasteindia.data

import com.tasteindia.data.remote.ApiException
import com.tasteindia.data.remote.MealService
import com.tasteindia.domain.MealDetail
import com.tasteindia.domain.MealSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Single entry point for recipe data.
 * Cache policy: in-memory only (see README). Detail lookups are de-duplicated and capped at
 * [maxConcurrentLookups] simultaneous network calls.
 */
class MealRepository(
    private val service: MealService,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    maxConcurrentLookups: Int = 4,
) {
    private val lookupPermits = Semaphore(maxConcurrentLookups)

    private val details = DedupLoader<String, MealDetail>(scope) { id ->
        lookupPermits.withPermit { service.detail(id) ?: throw ApiException.NotFound(id) }
    }
    private val categoryIds = DedupLoader<String, Set<String>>(scope) { category ->
        service.mealsByCategory(category).mapTo(HashSet()) { it.id }
    }
    private val ingredientIds = DedupLoader<String, Set<String>>(scope) { ingredient ->
        service.mealsByIngredient(ingredient).mapTo(HashSet()) { it.id }
    }
    private val categoryList = DedupLoader<Unit, List<String>>(scope) { service.categories() }
    private val ingredientList = DedupLoader<Unit, List<String>>(scope) { service.ingredients() }

    /** Authoritative Indian base set. Not cached here: the ViewModel owns it and retry re-fetches. */
    suspend fun indianMeals(): List<MealSummary> = service.indianMeals()

    suspend fun detail(id: String): MealDetail = details.get(id)

    fun cachedDetail(id: String): MealDetail? = details.peek(id)

    suspend fun idsForCategory(category: String): Set<String> = categoryIds.get(category)

    suspend fun idsForIngredient(ingredient: String): Set<String> = ingredientIds.get(ingredient)

    suspend fun categories(): List<String> = categoryList.get(Unit)

    suspend fun ingredients(): List<String> = ingredientList.get(Unit)
}
