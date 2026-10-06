package com.howdy.echowave.data.remote.innertube

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

const val INNERTUBE_BASE_URL = "https://music.youtube.com/youtubei/v1/"
const val INNERTUBE_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
const val INNERTUBE_CLIENT_NAME = "WEB_REMIX"
const val INNERTUBE_CLIENT_VERSION = "1.20241201.00.00"
const val INNERTUBE_CLIENT_ID = "67"

const val ANDROID_CLIENT_VERSION = "21.26.364"
const val ANDROID_CLIENT_ID = "3"
const val VISIONOS_CLIENT_VERSION = "1.02"
const val VISIONOS_CLIENT_ID = "101"
const val ANDROID_VR_CLIENT_VERSION = "1.65.10"
const val ANDROID_VR_CLIENT_ID = "28"
const val TV_CLIENT_VERSION = "7.20260213.00.00"
const val TV_CLIENT_ID = "7"

/** Anonymous browse bodies (charts, new releases). Donor browseIds, WEB_REMIX. */
fun browseBody(browseId: String, params: String? = null, visitorData: String? = null): JsonObject = buildJsonObject {
    put("context", fullContext(clientObj(INNERTUBE_CLIENT_NAME, INNERTUBE_CLIENT_VERSION, visitorData)))
    put("browseId", browseId)
    if (params != null) put("params", params)
}

val innerTubeJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    isLenient = true
}

/** Raw InnerTube transport. Parsing lives in InnerTubeMappers. */
interface InnerTubeApi {
    @POST("search")
    suspend fun search(
        @Query("key") key: String? = INNERTUBE_API_KEY,
        @Query("prettyPrint") prettyPrint: Boolean = false,
        @Header("X-YouTube-Client-Name") clientId: String? = null,
        @Header("X-YouTube-Client-Version") clientVersion: String? = null,
        @Body body: JsonObject,
    ): JsonObject

    @POST("player")
    suspend fun player(
        @Query("key") key: String? = INNERTUBE_API_KEY,
        @Query("prettyPrint") prettyPrint: Boolean = false,
        @Header("X-YouTube-Client-Name") clientId: String? = null,
        @Header("X-YouTube-Client-Version") clientVersion: String? = null,
        @Body body: JsonObject,
    ): JsonObject

    @POST("browse")
    suspend fun browse(
        @Query("key") key: String? = INNERTUBE_API_KEY,
        @Query("prettyPrint") prettyPrint: Boolean = false,
        @Header("X-YouTube-Client-Name") clientId: String? = null,
        @Header("X-YouTube-Client-Version") clientVersion: String? = null,
        @Body body: JsonObject,
    ): JsonObject
}

fun buildInnerTubeApi(debug: Boolean = false, visitor: () -> String? = { null }): InnerTubeApi {
    val logging = HttpLoggingInterceptor().apply {
        level = if (debug) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }
    val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) EchoWave/1.0")
                    .header("Accept", "application/json")
                    .header("X-Goog-Api-Format-Version", "1")
                    .header("X-Origin", "https://music.youtube.com")
                    .header("Origin", "https://music.youtube.com")
                    .header("Referer", "https://music.youtube.com/")
                    .apply { visitor()?.let { header("X-Goog-Visitor-Id", it) } }
                    .build(),
            )
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    return Retrofit.Builder()
        .baseUrl(INNERTUBE_BASE_URL)
        .client(client)
        .addConverterFactory(innerTubeJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(InnerTubeApi::class.java)
}

/** Donor-exact context shell: strict clients 400 without request/user blocks. */
private fun fullContext(client: JsonObject): JsonObject = buildJsonObject {
    put("client", client)
    put("request", buildJsonObject {
        put("useSsl", true)
    })
    put("user", buildJsonObject {
        put("lockedSafetyMode", false)
    })
}

private fun clientObj(
    name: String,
    version: String,
    visitorData: String?,
    extra: JsonObjectBuilder.() -> Unit = {},
): JsonObject = buildJsonObject {
    put("clientName", name)
    put("clientVersion", version)
    extra()
    put("hl", "en")
    put("gl", "US")
    if (visitorData != null) put("visitorData", visitorData)
}

fun searchBody(query: String, visitorData: String? = null): JsonObject = buildJsonObject {    put("context", fullContext(clientObj(INNERTUBE_CLIENT_NAME, INNERTUBE_CLIENT_VERSION, visitorData)))
    put("query", query)
}

fun playerBody(videoId: String, poToken: String? = null, visitorData: String? = null): JsonObject = buildJsonObject {
    put("context", fullContext(clientObj(INNERTUBE_CLIENT_NAME, INNERTUBE_CLIENT_VERSION, visitorData)))
    put("videoId", videoId)
    if (poToken != null) {
        put("serviceIntegrityDimensions", buildJsonObject { put("poToken", poToken) })
    }
    put("racyCheckOk", true)
    put("contentCheckOk", true)
}

/**
 * Donor-measured whole-file clients (Echo-Music STREAM_FALLBACK_CLIENTS order).
 * Same streamingData shape — the existing parser works unchanged.
 * These carry no web pot (donor: no useWebPoTokens on them).
 */
fun playerBodyVisionOs(videoId: String, visitorData: String? = null): JsonObject = buildJsonObject {
    put("context", fullContext(clientObj("VISIONOS", VISIONOS_CLIENT_VERSION, visitorData) {
        put("osName", "visionOS")
        put("osVersion", "1.3.21O771")
        put("deviceMake", "Apple")
        put("deviceModel", "RealityDevice14,1")
    }))
    put("videoId", videoId)
    put("racyCheckOk", true)
    put("contentCheckOk", true)
}

fun playerBodyAndroidVr(videoId: String, visitorData: String? = null): JsonObject = buildJsonObject {
    put("context", fullContext(clientObj("ANDROID_VR", ANDROID_VR_CLIENT_VERSION, visitorData) {
        put("osName", "Android")
        put("osVersion", "12L")
        put("deviceMake", "Oculus")
        put("deviceModel", "Quest 3")
        put("androidSdkVersion", 32)
    }))
    put("videoId", videoId)
    put("racyCheckOk", true)
    put("contentCheckOk", true)
}

fun playerBodyTv(videoId: String, visitorData: String? = null): JsonObject = buildJsonObject {
    put("context", fullContext(clientObj("TVHTML5", TV_CLIENT_VERSION, visitorData)))
    put("videoId", videoId)
    put("racyCheckOk", true)
    put("contentCheckOk", true)
}
fun playerBodyAndroid(videoId: String, poToken: String? = null, visitorData: String? = null): JsonObject = buildJsonObject {
    put("context", fullContext(clientObj("ANDROID", ANDROID_CLIENT_VERSION, visitorData) {
        put("androidSdkVersion", 34)
    }))
    put("videoId", videoId)
    if (poToken != null) {
        put("serviceIntegrityDimensions", buildJsonObject { put("poToken", poToken) })
    }
    put("playbackContext", buildJsonObject {
        put("contentPlaybackContext", buildJsonObject {
            put("html5Preference", "HTML5_PREF_WANTS")
        })
    })
    put("racyCheckOk", true)
    put("contentCheckOk", true)
}
