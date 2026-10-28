package com.tasteindia.data.remote

import java.io.InterruptedIOException
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import kotlinx.serialization.SerializationException

sealed class ApiException(message: String) : Exception(message) {
    class Http(val code: Int) : ApiException("HTTP $code")
    class NotFound(val id: String) : ApiException("Meal $id not found")
}

/** Human-readable, non-technical message for the UI. */
fun Throwable.toUserMessage(): String = when (this) {
    is UnknownHostException, is ConnectException ->
        "You appear to be offline. Check your connection and try again."
    is InterruptedIOException -> // includes SocketTimeoutException and OkHttp call timeouts
        "TheMealDB took too long to respond. Please try again."
    is ApiException.Http -> when (code) {
        429 -> "Too many requests were sent. Please wait a moment and retry."
        in 500..599 -> "TheMealDB is having problems right now (HTTP $code). Please retry shortly."
        else -> "TheMealDB returned an unexpected response (HTTP $code)."
    }
    is ApiException.NotFound -> "That recipe could not be found."
    is SerializationException -> "TheMealDB returned data this app could not read."
    is IOException -> "A network problem occurred. Please try again."
    else -> "Something went wrong. Please try again."
}
