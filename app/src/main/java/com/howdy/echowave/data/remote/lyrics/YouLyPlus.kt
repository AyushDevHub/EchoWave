package com.howdy.echowave.data.remote.lyrics

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.selects.select
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import android.text.Html

@Serializable
private data class YouLyPlusResponse(
    val type: String? = null,
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null,
    val lyrics: List<YouLyPlusItem>? = null,
)

@Serializable
private data class BinimumSearchResponse(val results: List<BinimumSearchResult> = emptyList())

@Serializable
private data class BinimumSearchResult(
    @SerialName("timing_type") val timingType: String? = null,
    val lyricsUrl: String? = null,
)

@Serializable
internal data class YouLyPlusItem(
    val text: String? = null,
    val time: Double? = null,
    val syllabus: List<YouLyPlusSyllable>? = null,
)

@Serializable
internal data class YouLyPlusSyllable(
    val text: String? = null,
    val time: Double? = null,
)

/**
 * Community LyricsPlus mirrors, raced in parallel, first usable result wins.
 * Port of Echo-Music's youlyplus module policy (GPL-3.0, see CREDITS.md):
 * same mirror set, sticky winner, KPoe word-sync converted to LRC.
 */
object YouLyPlus {
    private val SERVERS = listOf(
        "https://lyricsplus.prjktla.my.id",
        "https://lyricsplus.atomix.one",
        "https://lyricsplus.binimum.org",
        "https://lyricsplus.prjktla.workers.dev",
        "https://lyricsplus-seven.vercel.app",
        "https://lyrics-plus-backend.vercel.app",
    )

    private const val BINIMUM_SEARCH_URL = "https://lyrics-api.binimum.org/"
    private const val BINIMUM_STORAGE_HOST = "lyrics-storage.binimum.org"
    private const val LRC_RED_HOST = "lrc.red"

    private val lastWorking = AtomicReference<String?>(null)

    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** LRC text (synced preferred) or null. Never throws. */
    suspend fun fetch(
        title: String,
        artist: String?,
        durationSec: Double?,
        album: String? = null,
    ): String? = kotlinx.coroutines.coroutineScope {
        val ordered = lastWorking.get()?.let { w -> listOf(w) + SERVERS.filter { it != w } } ?: SERVERS
        val jobs = ordered.map { server -> server to async(Dispatchers.IO) { fetchOne(server, title, artist, durationSec, album) } }
        val binimumDeferred = async(Dispatchers.IO) { fetchBinimum(title, artist, durationSec, album) }
        try {
            // The old JSON mirrors can be rate limited or retired. Binimum's
            // catalog returns a signed TTML URL and remains a separate route.
            // Keep Binimum as a fallback while checking JSON mirrors: some
            // mirrors provide per-word timestamps even when Binimum only has
            // line timing for the same track.
            val remaining = jobs.toMutableList()
            var fallback: String? = null
            while (remaining.isNotEmpty()) {
                val (server, resp) = select {
                    remaining.forEach { (srv, deferred) -> deferred.onAwait { r -> srv to r } }
                }
                remaining.removeAll { it.first == server }
                val lrc = resp?.bestLrc()
                if (!lrc.isNullOrBlank()) {
                    if (resp.hasWordSync() || lrc.hasInlineWordSync()) {
                        lastWorking.set(server)
                        Log.d("EchoWaveLyrics", "LyricsPlus selected word-synced mirror=${server.substringAfter("//")}")
                        remaining.forEach { it.second.cancel() }
                        binimumDeferred.cancel()
                        return@coroutineScope lrc
                    }
                    if (fallback == null) {
                        fallback = lrc
                        lastWorking.set(server)
                    }
                }
            }
            val binimumLyrics = runCatching { binimumDeferred.await() }.getOrNull()
            if (binimumLyrics.hasInlineWordSync()) {
                Log.d("EchoWaveLyrics", "LyricsPlus selected word-synced Binimum fallback")
                return@coroutineScope binimumLyrics
            }
            return@coroutineScope fallback ?: binimumLyrics
        } finally {
            jobs.forEach { it.second.cancel() }
            binimumDeferred.cancel()
        }
    }

    private fun fetchOne(
        server: String,
        title: String,
        artist: String?,
        durationSec: Double?,
        album: String?,
    ): YouLyPlusResponse? {
        return try {
            val base = server.trimEnd('/') + "/v2/lyrics/get"
            val url = base.toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("title", title)
                ?.apply {
                    artist?.takeIf { it.isNotBlank() }?.let { addQueryParameter("artist", it) }
                    durationSec?.let { addQueryParameter("duration", it.toString()) }
                    album?.let { addQueryParameter("album", it) }
                }
                ?.build() ?: return null
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "EchoWave/1.0 (https://github.com/AyushDevHub/EchoWave)")
                .header("X-Client-Package", "EchoWave <https://github.com/AyushDevHub/EchoWave>")
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w("EchoWaveLyrics", "LyricsPlus mirror=${server.substringAfter("//")} status=${resp.code}")
                    return null
                }
                val body = resp.body?.string() ?: return null
                runCatching { json.decodeFromString(YouLyPlusResponse.serializer(), body) }
                    .onFailure { Log.w("EchoWaveLyrics", "LyricsPlus mirror=${server.substringAfter("//")} invalid response=${it.javaClass.simpleName}") }
                    .getOrNull()
                    ?.also { result ->
                        Log.d("EchoWaveLyrics", "LyricsPlus mirror=${server.substringAfter("//")} type=${result.type ?: "unknown"} lines=${result.lyrics?.size ?: 0} wordSync=${result.hasWordSync()} synced=${!result.syncedLyrics.isNullOrBlank()} plain=${!result.plainLyrics.isNullOrBlank()}")
                    }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w("EchoWaveLyrics", "LyricsPlus mirror=${server.substringAfter("//")} failed=${error.javaClass.simpleName}")
            null
        }
    }

    private fun YouLyPlusResponse.bestLrc(): String? =
        lyrics?.takeIf { items -> items.any { !it.syllabus.isNullOrEmpty() } }
            ?.convertToLrc()?.takeIf { it.isNotBlank() }
            ?: syncedLyrics?.takeIf { it.isNotBlank() }
            ?: lyrics?.convertToLrc()?.takeIf { it.isNotBlank() }
            ?: plainLyrics?.takeIf { it.isNotBlank() }

    private fun YouLyPlusResponse.hasWordSync(): Boolean =
        lyrics?.any { !it.syllabus.isNullOrEmpty() } == true ||
            syncedLyrics?.hasInlineWordSync() == true

    private fun String?.hasInlineWordSync(): Boolean =
        !this.isNullOrBlank() && Regex("""<\d{1,}:\d{2}(?:[.:]\d{1,3})?>""").containsMatchIn(this)

    private fun fetchBinimum(title: String, artist: String?, durationSec: Double?, album: String?): String? = try {
        val searchUrl = BINIMUM_SEARCH_URL.toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("track", title)
            ?.apply {
                artist?.takeIf { it.isNotBlank() }?.let { addQueryParameter("artist", it) }
                album?.takeIf { it.isNotBlank() }?.let { addQueryParameter("album", it) }
                durationSec?.let { addQueryParameter("duration", it.toInt().toString()) }
            }?.build() ?: return null
        val result = http.newCall(Request.Builder().url(searchUrl)
            .header("User-Agent", "EchoWave/1.0 (https://github.com/AyushDevHub/EchoWave)")
            .header("X-Client-Package", "EchoWave <https://github.com/AyushDevHub/EchoWave>")
            .build()).execute().use { response ->
            Log.d("EchoWaveLyrics", "LyricsPlus Binimum search status=${response.code}")
            if (!response.isSuccessful) {
                Log.w("EchoWaveLyrics", "LyricsPlus Binimum search status=${response.code}")
                return null
            }
            val body = response.body?.string() ?: return null
            val search = json.decodeFromString(BinimumSearchResponse.serializer(), body)
            Log.d("EchoWaveLyrics", "LyricsPlus Binimum candidates=${search.results.size}")
            search.results.firstOrNull()
        } ?: return null
        val lyricsUrl = result.lyricsUrl?.let { it.toHttpUrlOrNull() }
            ?.takeIf {
                it.isHttps && (it.host.equals(BINIMUM_STORAGE_HOST, ignoreCase = true) ||
                    it.host.equals(LRC_RED_HOST, ignoreCase = true))
            }
            ?: return null
        Log.d("EchoWaveLyrics", "LyricsPlus Binimum selected host=${lyricsUrl.host} timing=${result.timingType ?: "unknown"}")
        val ttml = http.newCall(Request.Builder().url(lyricsUrl).build()).execute().use { response ->
            Log.d("EchoWaveLyrics", "LyricsPlus TTML status=${response.code}")
            if (!response.isSuccessful) return null
            response.body?.string()?.takeIf { it.isNotBlank() }
        } ?: return null
        ttmlToLrc(ttml).also { lrc ->
            Log.d("EchoWaveLyrics", "LyricsPlus TTML parsed=${lrc?.lineSequence()?.count() ?: 0} lines")
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Log.w("EchoWaveLyrics", "LyricsPlus Binimum failed=${error.javaClass.simpleName}")
        null
    }

    private fun ttmlToLrc(ttml: String): String? = runCatching {
        require(!ttml.contains("<!DOCTYPE", ignoreCase = true)) { "DOCTYPE is not allowed" }
        val paragraphs = Regex("""<p\b([^>]*)>(.*?)</p>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val spans = Regex("""<span\b([^>]*)>(.*?)</span>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val attribute = Regex("""(?:^|\s)begin\s*=\s*[\"']([^\"']+)[\"']""", RegexOption.IGNORE_CASE)
        val output = buildString {
            paragraphs.findAll(ttml).forEach { paragraph ->
                val start = attribute.find(paragraph.groupValues[1])?.groupValues?.get(1)
                    ?.let(::parseTtmlTime) ?: return@forEach
                val content = paragraph.groupValues[2]
                val wordSpans = spans.findAll(content).toList()
                val decodedLine = decodeXmlText(content.replace(Regex("<[^>]+>"), "")).trim()
                if (decodedLine.isBlank()) return@forEach
                append(formatLrc(start))
                val timedWords = wordSpans.mapNotNull { span ->
                    val time = attribute.find(span.groupValues[1])?.groupValues?.get(1)
                        ?.let(::parseTtmlTime) ?: return@mapNotNull null
                    time to decodeXmlText(span.groupValues[2])
                }
                if (timedWords.isEmpty()) {
                    append(decodedLine)
                } else {
                    timedWords.forEachIndexed { index, (time, text) ->
                        val gap = wordSpans.getOrNull(index + 1)?.let { next ->
                            decodeXmlText(content.substring(wordSpans[index].range.last + 1, next.range.first))
                        } ?: ""
                        // Some TTML feeds place each word in an adjacent span with no
                        // whitespace text node. Keep the token boundaries in the LRC
                        // payload so the renderer does not join every word together.
                        val separator = if (index < timedWords.lastIndex && gap.isEmpty() &&
                            text.isNotEmpty() && !text.last().isWhitespace()) " " else gap
                        append(formatSyl(time)).append(text).append(separator)
                    }
                }
                append('\n')
            }
        }.trim().takeIf(String::isNotBlank)
        Log.d("EchoWaveLyrics", "LyricsPlus TTML parsed=${output?.lineSequence()?.count() ?: 0} lines")
        output
    }.onFailure { Log.w("EchoWaveLyrics", "LyricsPlus Binimum TTML parse failed=${it.javaClass.simpleName}") }
        .getOrNull()

    private fun decodeXmlText(text: String): String =
        Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString()

    private fun parseTtmlTime(value: String): Long? = runCatching {
        val normalized = value.trim().removeSuffix("s")
        val seconds = if (normalized.contains(':')) {
            val parts = normalized.split(':')
            when (parts.size) {
                2 -> parts[0].toDouble() * 60 + parts[1].toDouble()
                3 -> parts[0].toDouble() * 3600 + parts[1].toDouble() * 60 + parts[2].toDouble()
                else -> return null
            }
        } else normalized.toDouble()
        (seconds * 1000).toLong()
    }.getOrNull()

    /** KPoe items (optional word syllables) -> LRC. Pure + tested. Time is seconds; LRC needs ms. */
    internal fun List<YouLyPlusItem>.convertToLrc(): String? {
        if (isEmpty()) return null
        return joinToString("\n") { item ->
            val stamp = formatLrc(((item.time ?: 0.0) * 1000).toLong())
            val syls = item.syllabus
            if (syls.isNullOrEmpty()) {
                stamp + (item.text ?: "")
            } else {
                buildString {
                    append(stamp)
                    syls.forEach { s ->
                        append(formatSyl(((s.time ?: 0.0) * 1000).toLong()))
                        append(s.text ?: "")
                        if (s.text?.endsWith(" ") == false) append(" ")
                    }
                }.trim()
            }
        }
    }

    private fun formatLrc(ms: Long): String {
        val m = ms / 60_000
        val s = (ms / 1000) % 60
        val x = ms % 1000
        return "[%02d:%02d.%03d]".format(m, s, x)
    }

    private fun formatSyl(ms: Long): String {
        val m = ms / 60_000
        val s = (ms / 1000) % 60
        val x = ms % 1000
        return "<%02d:%02d.%03d>".format(m, s, x)
    }
}
