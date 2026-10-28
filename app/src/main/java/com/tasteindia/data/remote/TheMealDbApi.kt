package com.tasteindia.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** Public TheMealDB V1 endpoints (test key "1" is part of the base URL). Query values are URL-encoded by Retrofit. */
interface TheMealDbApi {
    @GET("filter.php") suspend fun filterByArea(@Query("a") area: String): Response<ResponseBody>
    @GET("filter.php") suspend fun filterByCategory(@Query("c") category: String): Response<ResponseBody>
    @GET("filter.php") suspend fun filterByIngredient(@Query("i") ingredient: String): Response<ResponseBody>
    @GET("lookup.php") suspend fun lookup(@Query("i") id: String): Response<ResponseBody>
    @GET("search.php") suspend fun searchByFirstLetter(@Query("f") letter: String): Response<ResponseBody>
    @GET("list.php") suspend fun listCategories(@Query("c") list: String = "list"): Response<ResponseBody>
    @GET("list.php") suspend fun listIngredients(@Query("i") list: String = "list"): Response<ResponseBody>

    companion object {
        /** Educational test key from the public docs; not a secret. */
        const val API_KEY = "1"
        const val BASE_URL = "https://www.themealdb.com/api/json/v1/$API_KEY/"
    }
}