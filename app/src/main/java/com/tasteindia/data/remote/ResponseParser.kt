package com.tasteindia.data.remote

import com.tasteindia.domain.Ingredient
import com.tasteindia.domain.MealDetail
import com.tasteindia.domain.MealSummary
import com.tasteindia.domain.safeHttpUrl
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

val DefaultJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

/** filter.php row. Fields are nullable so a partial row degrades instead of failing the whole list. */
@Serializable
data class MealSummaryDto(
    val idMeal: String? = null,
    val strMeal: String? = null,
    val strMealThumb: String? = null,
)

/**
 * lookup.php returns ~50 flat keys, including strIngredient1..20 / strMeasure1..20.
 * Decoding to a key/value map keeps every field available without 40 hand-written properties.
 */
typealias MealDetailDto = Map<String, String?>

private val detailSerializer = MapSerializer(String.serializer(), String.serializer().nullable)
private val summaryListSerializer = ListSerializer(MealSummaryDto.serializer())

private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

fun MealSummaryDto.toDomain(): MealSummary? {
    val id = idMeal.clean() ?: return null
    val name = strMeal.clean() ?: return null
    return MealSummary(id, name, safeHttpUrl(strMealThumb))
}

/** Normalises the 20 ingredient/measure pairs and drops blank values. */
fun MealDetailDto.toMealDetail(): MealDetail? {
    fun v(key: String) = this[key].clean()
    val id = v("idMeal") ?: return null
    val name = v("strMeal") ?: return null
    val ingredients = (1..20).mapNotNull { i ->
        val ingredient = v("strIngredient$i") ?: return@mapNotNull null
        Ingredient(ingredient, v("strMeasure$i"))
    }
    return MealDetail(
        id = id,
        name = name,
        thumbUrl = safeHttpUrl(v("strMealThumb")),
        category = v("strCategory"),
        area = v("strArea"),
        ingredients = ingredients,
        instructions = v("strInstructions").orEmpty()
            .split(Regex("\\r?\\n")).map { it.trim() }.filter { it.isNotEmpty() },
        tags = v("strTags").orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() },
        sourceUrl = safeHttpUrl(v("strSource")),
        videoUrl = safeHttpUrl(v("strYoutube")),
        imageSourceUrl = safeHttpUrl(v("strImageSource")),
        alternateDrink = v("strDrinkAlternate"),
    )
}

/** Turns raw response text into domain objects. TheMealDB returns {"meals":null} for "no results". */
class ResponseParser(private val json: Json = DefaultJson) {

    fun summaries(body: String): List<MealSummary> =
        json.decodeFromJsonElement(summaryListSerializer, mealsArray(body)).mapNotNull { it.toDomain() }

    fun details(body: String): List<MealDetail> =
        mealsArray(body).mapNotNull { element ->
            (element as? JsonObject)?.let { json.decodeFromJsonElement(detailSerializer, it).toMealDetail() }
        }

    /** For list.php responses, e.g. key = "strCategory" or "strIngredient". */
    fun names(body: String, key: String): List<String> =
        mealsArray(body).mapNotNull { element ->
            ((element as? JsonObject)?.get(key) as? JsonPrimitive)?.contentOrNull.clean()
        }.distinct()

    private fun mealsArray(body: String): JsonArray {
        if (body.isBlank()) return JsonArray(emptyList())
        val root = json.parseToJsonElement(body) as? JsonObject
        return root?.get("meals") as? JsonArray ?: JsonArray(emptyList())
    }
}
