package com.howdy.echowave.data.remote.innertube

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * visitorData bootstrap without login (donor: sw.js_data + VISITOR_DATA_REGEX).
 * Every InnerTube response also echoes visitorData, which VisitorStore picks
 * up — this only fills the fresh-install gap before the first response.
 */
object VisitorBootstrap {
    // YouTube visitor ids look like Cgt... (base64url). Value never logged.
    private val VISITOR_RE = Regex("Cgt[A-Za-z0-9_\\-]{20,}={0,2}")

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun fetch(): String? {
        return try {
            val req = Request.Builder()
                .url("https://music.youtube.com/sw.js_data")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) EchoWave/1.0")
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val text = if (body.length > 5) body.substring(5) else body
                VISITOR_RE.find(text)?.value ?: findStructured(text)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun findStructured(text: String): String? {
        return try {
            val outer: JsonArray = innerTubeJson.parseToJsonElement(text).jsonArray
            val inner: JsonArray = outer[0].jsonArray[2].jsonArray
            inner.mapNotNull { el: JsonElement ->
                (el as? JsonPrimitive)?.takeIf { p -> p.isString }?.content
            }.firstOrNull { c -> VISITOR_RE.containsMatchIn(c) }
        } catch (_: Exception) {
            null
        }
    }
}
