@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.tasteindia

import com.tasteindia.data.DedupLoader
import com.tasteindia.data.MealRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Protects against: N+1 request storms, duplicate in-flight lookups, caching failures forever. */
class DedupLoaderTest {

    @Test
    fun concurrentCallersForSameKeyShareOneLoad() = runTest {
        var loads = 0
        val loader = DedupLoader<String, String>(backgroundRepoScope()) { delay(100); loads++; "value" }

        val results = (1..5).map { async { loader.get("a") } }.awaitAll()

        assertEquals(List(5) { "value" }, results)
        assertEquals(1, loads)
        assertEquals("value", loader.get("a")) // cached
        assertEquals(1, loads)
    }

    @Test
    fun failuresAreNotCached() = runTest {
        var attempts = 0
        val loader = DedupLoader<String, String>(backgroundRepoScope()) {
            attempts++
            if (attempts == 1) error("first fails") else "ok"
        }
        try {
            loader.get("a"); fail("expected failure")
        } catch (e: IllegalStateException) { /* expected */ }

        assertEquals("ok", loader.get("a"))
    }

    @Test
    fun detailLookupsAreBoundedAndDeduplicated() = runTest {
        val service = FakeMealService().apply { detailDelayMs = 50 }
        val repo = MealRepository(service, backgroundRepoScope(), maxConcurrentLookups = 3)

        val ids = (1..12).map { "id$it" } + (1..12).map { "id$it" } // every id requested twice
        ids.map { async { repo.detail(it) } }.awaitAll()
        advanceUntilIdle()

        assertEquals(12, service.detailCalls.get())               // de-duplicated
        assertTrue(service.maxConcurrentDetails.get() <= 3)       // bounded
        assertEquals(3, service.maxConcurrentDetails.get())
    }
}
