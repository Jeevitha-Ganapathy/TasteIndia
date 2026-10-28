package com.tasteindia.data.remote

import com.tasteindia.domain.MealDetail
import com.tasteindia.domain.MealSummary
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit

class TheMealDbService(
    private val api: TheMealDbApi,
    private val parser: ResponseParser = ResponseParser(),
) : MealService {

    override suspend fun indianMeals(): List<MealSummary> {
        // The provider has used both "Indian" and "India" as the area value.
        for (area in INDIAN_AREA_NAMES) {
            val meals = read(api.filterByArea(area)) { parser.summaries(it) }
            if (meals.isNotEmpty()) return meals
        }
        return indianViaLetterSearch()
    }

    /** Fallback: search.php?f=<letter>, keeping only meals whose strArea is Indian/India. */
    private suspend fun indianViaLetterSearch(): List<MealSummary> = coroutineScope {
        val gate = Semaphore(4)
        ('a'..'z').map { letter ->
            async {
                gate.withPermit { read(api.searchByFirstLetter(letter.toString())) { parser.details(it) } }
            }
        }.awaitAll()
            .flatten()
            .filter { meal -> meal.area?.trim()?.lowercase() in INDIAN_AREA_NAMES.map { it.lowercase() } }
            .distinctBy { it.id }
            .map { it.toSummary() }
    }

    override suspend fun mealsByCategory(category: String): List<MealSummary> =
        read(api.filterByCategory(category)) { parser.summaries(it) }

    override suspend fun mealsByIngredient(ingredient: String): List<MealSummary> =
        read(api.filterByIngredient(ingredient.trim().replace(' ', '_'))) { parser.summaries(it) }

    override suspend fun categories(): List<String> =
        read(api.listCategories()) { parser.names(it, "strCategory") }

    override suspend fun ingredients(): List<String> =
        read(api.listIngredients()) { parser.names(it, "strIngredient") }

    override suspend fun detail(id: String): MealDetail? =
        read(api.lookup(id)) { parser.details(it).firstOrNull() }

    private suspend fun <T> read(response: Response<ResponseBody>, parse: (String) -> T): T =
        withContext(Dispatchers.IO) {
            if (!response.isSuccessful) {
                response.errorBody()?.close()
                throw ApiException.Http(response.code())
            }
            parse(response.body()?.string().orEmpty())
        }

    companion object {
        val INDIAN_AREA_NAMES = listOf("Indian", "India")

        fun create(): TheMealDbService {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(25, TimeUnit.SECONDS)
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(TheMealDbApi.BASE_URL)
                .client(client)
                .build()
            return TheMealDbService(retrofit.create(TheMealDbApi::class.java))
        }
    }
}