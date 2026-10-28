package com.tasteindia.domain

import java.net.URI
import java.net.URISyntaxException

/** Returns [raw] (trimmed) only if it is a well-formed http(s) URL with a host; otherwise null. */
fun safeHttpUrl(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return null
    return try {
        val uri = URI(value)
        val schemeOk = uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true)
        if (schemeOk && !uri.host.isNullOrBlank()) value else null
    } catch (e: URISyntaxException) {
        null
    }
}
