package com.tasteindia.data.favourites

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tasteindia.domain.MealSummary
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Favourites are stored locally with name + thumbnail so they never depend on the network. */
interface FavouritesStore {
    val favourites: Flow<List<MealSummary>>
    suspend fun toggle(meal: MealSummary)
}

@Serializable
private data class FavouriteEntity(val id: String, val name: String, val thumbUrl: String? = null)

class DataStoreFavouritesStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : FavouritesStore {

    private val key = stringPreferencesKey("favourites_v1")
    private val serializer = ListSerializer(FavouriteEntity.serializer())

    override val favourites: Flow<List<MealSummary>> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { decode(it[key]) }
        .distinctUntilChanged()

    override suspend fun toggle(meal: MealSummary) {
        dataStore.edit { prefs ->
            val current = decode(prefs[key]).toMutableList()
            if (!current.removeAll { it.id == meal.id }) current.add(meal)
            prefs[key] = json.encodeToString(serializer, current.map { FavouriteEntity(it.id, it.name, it.thumbUrl) })
        }
    }

    private fun decode(raw: String?): List<MealSummary> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(serializer, raw).map { MealSummary(it.id, it.name, it.thumbUrl) }
        } catch (e: SerializationException) {
            emptyList()
        }
    }
}
