package com.howdy.echowave.data.remote.potoken

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * Pure BotGuard/poToken byte codecs. Port of Echo-Music's
 * utils/potoken/JavaScriptUtil.kt (GPL-3.0, see CREDITS.md).
 * Uses java.util.Base64 (needs API 26+; our minSdk follows the donor at 26).
 */
object JsCodec {
    private val json = Json { ignoreUnknownKeys = true }

    /** Raw `jnn/Create` challenge -> embeddable JS object literal. */
    fun parseChallengeData(raw: String): String {
        val scrambled = json.parseToJsonElement(raw).jsonArray
        if (scrambled.isEmpty()) throw PoTokenException("Empty challenge")
        val challengeData =
            if (scrambled.size > 1 && scrambled[1].jsonPrimitive.isString) {
                val descrambled = descramble(scrambled[1].jsonPrimitive.content)
                json.parseToJsonElement(descrambled).jsonArray
            } else {
                scrambled[0].jsonArray
            }
        if (challengeData.size <= 7) throw PoTokenException("Truncated challenge")
        fun idx(i: Int): String {
            if (i !in challengeData.indices) throw PoTokenException("Truncated challenge field $i")
            return challengeData[i].jsonPrimitive.content
        }
        val safeScript = challengeData[1].takeIf { it !is JsonNull }
            ?.jsonArray?.firstOrNull { it.jsonPrimitive.isString }
            ?: JsonNull
        val trustedUrl = challengeData[2].takeIf { it !is JsonNull }
            ?.jsonArray?.firstOrNull { it.jsonPrimitive.isString }
            ?: JsonNull
        return Json.encodeToString(
            JsonObject.serializer(),
            JsonObject(
                mapOf(
                    "messageId" to JsonPrimitive(idx(0)),
                    "interpreterJavascript" to JsonObject(
                        mapOf(
                            "privateDoNotAccessOrElseSafeScriptWrappedValue" to safeScript,
                            "privateDoNotAccessOrElseTrustedResourceUrlWrappedValue" to trustedUrl,
                        ),
                    ),
                    "interpreterHash" to JsonPrimitive(idx(3)),
                    "program" to JsonPrimitive(idx(4)),
                    "globalName" to JsonPrimitive(idx(5)),
                    "clientExperimentsStateBlob" to JsonPrimitive(idx(7)),
                ),
            ),
        )
    }

    /** Raw `jnn/GenerateIT` response -> (JS Uint8Array literal, expiry seconds). */
    fun parseIntegrityTokenData(raw: String): Pair<String, Long> {
        val arr = json.parseToJsonElement(raw).jsonArray
        if (arr.size < 2) throw PoTokenException("Truncated integrity token")
        return base64ToU8(arr[0].jsonPrimitive.content) to arr[1].jsonPrimitive.long
    }

    /** Identifier string -> JS `new Uint8Array([...])` literal. */
    fun stringToU8(identifier: String): String = newUint8Array(identifier.toByteArray())

    /** JS `Uint8Array.toString()` ("97,98,99") -> URL-safe base64 poToken. */
    fun u8ToBase64(commaSeparated: String): String {
        val bytes = commaSeparated.split(",")
            .filter { it.isNotBlank() }
            .map {
                it.trim().toUByteOrNull()?.toByte()
                    ?: throw PoTokenException("Malformed token bytes")
            }
            .toByteArray()
        return java.util.Base64.getUrlEncoder().encodeToString(bytes)
    }

    private fun descramble(scrambled: String): String =
        base64ToBytes(scrambled).map { (it + 97).toByte() }.toByteArray().decodeToString()

    private fun base64ToU8(base64: String): String = newUint8Array(base64ToBytes(base64))

    private fun newUint8Array(contents: ByteArray): String =
        "new Uint8Array([" + contents.joinToString(",") { it.toUByte().toString() } + "])"

    private fun base64ToBytes(base64: String): ByteArray {
        val std = base64.replace('-', '+').replace('_', '/').replace('.', '=')
        return try {
            java.util.Base64.getDecoder().decode(std)
        } catch (_: IllegalArgumentException) {
            throw PoTokenException("Cannot base64 decode")
        }
    }
}

class PoTokenException(message: String) : Exception(message)
