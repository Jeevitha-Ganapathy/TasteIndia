package com.tasteindia.data

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * In-memory cache + in-flight de-duplication.
 *  - Successful results are cached for the process lifetime.
 *  - Concurrent callers for the same key share ONE load.
 *  - The load runs in [scope], so one caller being cancelled (e.g. a stale search) does not cancel others.
 *  - Failures are not cached; the next call retries.
 */
class DedupLoader<K : Any, V : Any>(
    private val scope: CoroutineScope,
    private val load: suspend (K) -> V,
) {
    private val mutex = Mutex()
    private val values = ConcurrentHashMap<K, V>()
    private val inFlight = HashMap<K, Deferred<V>>()

    fun peek(key: K): V? = values[key]

    suspend fun get(key: K): V {
        values[key]?.let { return it }
        val deferred = mutex.withLock {
            values[key]?.let { return it }
            inFlight.getOrPut(key) {
                scope.async {
                    try {
                        load(key).also { values[key] = it }
                    } finally {
                        withContext(NonCancellable) { mutex.withLock { inFlight.remove(key) } }
                    }
                }
            }
        }
        return deferred.await()
    }
}
