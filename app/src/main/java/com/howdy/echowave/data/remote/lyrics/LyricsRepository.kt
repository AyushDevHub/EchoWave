package com.howdy.echowave.data.remote.lyrics

import com.howdy.echowave.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

/** A word/phrase inside a line with the provider's word-start timestamp. */
data class LrcWord(val ms: Long, val text: String)

/** One lyric line. [words] is populated for enhanced/word-synced sources. */
data class LrcLine(val ms: Long, val text: String, val words: List<LrcWord> = emptyList())

@Serializable
private data class LrclibResponse(
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
)

/**
 * LyricsPlus word-sync first, LRCLIB exact/search fallback. Provider calls
 * stay off the main thread; only successful results are cached so retries can
 * recover from temporary provider failures.
 */
class LyricsRepository(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val cache = ConcurrentHashMap<String, List<LrcLine>>()

    suspend fun lyrics(track: Track, forceRefresh: Boolean = false): List<LrcLine>? {
        if (!forceRefresh) cache[track.id]?.let { return it }
        val result = withTimeoutOrNull(30_000) { withContext(Dispatchers.IO) {
            // LyricsPlus can return enhanced word timestamps, line-synced LRC,
            // or plain text. Prefer word-sync whenever the payload has it.
            val plusPayload = YouLyPlus.fetch(
                title = track.title,
                artist = track.lyricsArtist().takeIf { it.isNotBlank() },
                durationSec = track.durationMs?.toDouble()?.div(1000.0),
                album = track.album,
            )
            val plusLines = plusPayload?.let { payload ->
                parseLrc(payload).takeIf { it.isNotEmpty() }
                    ?: payload.lineSequence().map(String::trim)
                        .filter { it.isNotEmpty() && !it.startsWith("[") }
                        .map { LrcLine(0, it) }.toList().takeIf { it.isNotEmpty() }
            }?.filterNot { line ->
                val text = line.text.lowercase()
                val title = track.title.trim().lowercase()
                val artist = track.artist.trim().lowercase()
                title.length > 2 && artist.length > 2 && text.contains(title) && text.contains(artist)
            }
            val normalizedPlus = plusLines?.let { normalizeLyricTiming(it, track.durationMs) }
            // A provider can return malformed word times (for example, seconds
            // scaled twice). Prefer another timed source over freezing every
            // line at zero or showing timestamps beyond the track duration.
            normalizedPlus?.takeIf { it.any { line -> line.ms > 0L } }
                ?: fetchLrclib(track)
                ?: plusLines
        } } ?: run {
            android.util.Log.w("EchoWaveLyrics", "Lyrics lookup timed out")
            null
        }
        if (result != null) cache[track.id] = result
        return result
    }

    private fun fetchLrclib(track: Track): List<LrcLine>? {
        // Exact lookup may miss when the provider reports a different album or
        // duration. Retry without those constraints, then use LRCLIB search.
        val exact = requestLrclib("get", track, includeAlbum = true, includeDuration = true)
            ?: requestLrclib("get", track, includeAlbum = false, includeDuration = false)
        if (exact != null) return exact

        val url = lrclibUrl("search", track, includeAlbum = false, includeDuration = false)
        return try {
            http.newCall(lrclibRequest(url)).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val results = json.decodeFromString(
                    kotlinx.serialization.builtins.ListSerializer(LrclibResponse.serializer()),
                    body,
                )
                results.asSequence()
                    .sortedBy { result ->
                        val duration = result.duration?.times(1000.0)?.toLong()
                        if (duration == null || track.durationMs == null) Long.MAX_VALUE
                        else kotlin.math.abs(duration - track.durationMs)
                    }
                    .mapNotNull(::parseLrclibResult)
                    .firstOrNull()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    private fun requestLrclib(
        endpoint: String,
        track: Track,
        includeAlbum: Boolean,
        includeDuration: Boolean,
    ): List<LrcLine>? = try {
        http.newCall(lrclibRequest(lrclibUrl(endpoint, track, includeAlbum, includeDuration)))
            .execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()?.let { body ->
                    runCatching { json.decodeFromString(LrclibResponse.serializer(), body) }
                        .getOrNull()?.let(::parseLrclibResult)
                }
            }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private fun lrclibUrl(
        endpoint: String,
        track: Track,
        includeAlbum: Boolean,
        includeDuration: Boolean,
    ) = "https://lrclib.net/api/$endpoint".toHttpUrl().newBuilder()
        .addQueryParameter("track_name", track.title)
        .apply {
            track.lyricsArtist().takeIf { it.isNotBlank() }?.let { addQueryParameter("artist_name", it) }
            if (includeAlbum) track.album?.takeIf { it.isNotBlank() }?.let { addQueryParameter("album_name", it) }
            if (includeDuration) track.durationMs?.let { addQueryParameter("duration", (it / 1000).toString()) }
        }
        .build()

    private fun Track.lyricsArtist(): String {
        val artist = artist.trim()
        if (artist.isNotBlank() && artist.lowercase() !in UNKNOWN_ARTISTS) return artist
        return album.orEmpty().trim().takeIf { it.lowercase() !in UNKNOWN_ARTISTS }.orEmpty()
    }

    private companion object {
        val UNKNOWN_ARTISTS = setOf("unknown artist", "unknown", "song", "video", "album", "playlist", "artist")
    }

    private fun lrclibRequest(url: okhttp3.HttpUrl) = Request.Builder()
        .url(url)
        .header("User-Agent", "EchoWave/1.0 (https://github.com/AyushDevHub/EchoWave)")
        .build()

    private fun parseLrclibResult(result: LrclibResponse): List<LrcLine>? {
        val synced = result.syncedLyrics?.let(::parseLrc)?.takeIf { it.isNotEmpty() }
        return synced ?: result.plainLyrics
            ?.lineSequence()?.map { it.trim() }?.filter { it.isNotEmpty() }?.map { LrcLine(0, it) }?.toList()
            ?.takeIf { it.isNotEmpty() }
    }

    internal fun parseLrclib(body: String): List<LrcLine>? {
        return try {
            val r = json.decodeFromString(LrclibResponse.serializer(), body)
            val synced = r.syncedLyrics?.let(::parseLrc)?.takeIf { it.isNotEmpty() }
            synced ?: r.plainLyrics
                ?.lineSequence()?.map { it.trim() }?.filter { it.isNotEmpty() }?.map { LrcLine(0, it) }?.toList()
                ?.takeIf { it.isNotEmpty() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }
}

/** Corrects a consistent seconds/milliseconds scale error when duration is known. */
internal fun normalizeLyricTiming(lines: List<LrcLine>, durationMs: Long?): List<LrcLine> {
    val maximum = lines.asSequence()
        .flatMap { sequenceOf(it.ms) + it.words.asSequence().map(LrcWord::ms) }
        .maxOrNull() ?: return lines
    val divideByThousand = if (durationMs != null && durationMs > 0L) {
        maximum > durationMs * 1.25 && maximum / 1000L <= durationMs * 1.1
    } else {
        // Song lyrics should not span an hour. This catches a seconds to
        // milliseconds conversion applied twice when track metadata lacks a
        // duration, while leaving ordinary LRC timestamps untouched.
        maximum > 3_600_000L && maximum / 1000L <= 3_600_000L
    }
    if (!divideByThousand) return lines
    return lines.map { line ->
        val words = line.words.map { word ->
            word.copy(ms = word.ms / 1000L)
        }
        val lineMs = line.ms / 1000L
        line.copy(
            ms = lineMs.takeIf { it > 0L } ?: words.firstOrNull { it.ms > 0L }?.ms ?: 0L,
            words = words,
        )
    }
}

/** Parses [mm:ss.xx] lines; tolerates metadata tags and multi-stamps. Pure + tested. */
fun parseLrc(text: String): List<LrcLine> {
    val stamp = Regex("""\[(\d+):(\d{2})(?:[.:](\d{1,3}))?\]""")
    val inlineStamp = Regex("""<(\d+):(\d{2})(?:[.:](\d{1,3}))?>""")
    val out = mutableListOf<LrcLine>()
    for (raw in text.lineSequence()) {
        val matches = stamp.findAll(raw).toList()
        if (matches.isEmpty()) continue
        val content = raw.substring(matches.last().range.last + 1)
        val wordStamps = inlineStamp.findAll(content).toList()
        val lyric = content.replace(inlineStamp, "").trim()
        if (lyric.isEmpty()) continue
        val words = wordStamps.mapIndexed { index, wordStamp ->
            val start = wordStamp.range.last + 1
            val end = wordStamps.getOrNull(index + 1)?.range?.first ?: content.length
            LrcWord(parseTimestamp(wordStamp.groupValues), content.substring(start, end))
        }
        for (m in matches) {
            out += LrcLine(parseTimestamp(m.groupValues), lyric, words)
        }
    }
    return out.sortedBy { it.ms }
}

private fun parseTimestamp(groups: List<String>): Long {
    val minutes = groups[1].toLong()
    val seconds = groups[2].toLong()
    val fraction = groups.getOrElse(3) { "" }
    val millis = when (fraction.length) {
        0 -> 0L
        1 -> fraction.toLong() * 100L
        2 -> fraction.toLong() * 10L
        else -> fraction.take(3).toLong()
    }
    return minutes * 60_000 + seconds * 1000 + millis
}

/** Index of the line active at [positionMs], or -1. Pure + tested. */
fun currentLrcIndex(lines: List<LrcLine>, positionMs: Long): Int {
    if (lines.none { it.ms > 0L }) return -1
    var idx = -1
    for (i in lines.indices) {
        if (lines[i].ms <= positionMs) idx = i else break
    }
    return idx
}

/** Index of the active word within a line, or -1 before its first word. */
fun currentLrcWordIndex(words: List<LrcWord>, positionMs: Long): Int {
    var idx = -1
    for (i in words.indices) {
        if (words[i].ms <= positionMs) idx = i else break
    }
    return idx
}
