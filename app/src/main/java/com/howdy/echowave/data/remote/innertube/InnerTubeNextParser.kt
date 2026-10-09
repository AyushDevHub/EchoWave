package com.howdy.echowave.data.remote.innertube

import com.howdy.echowave.domain.model.Track
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses InnerTube `/next` watch-next / automated radio responses.
 * Extracts `playlistPanelVideoRenderer` items from the musicQueue.
 */
object InnerTubeNextParser {
    fun parse(root: JsonObject, limit: Int = 25): List<Track> {
        val tracks = mutableListOf<Track>()
        val seen = mutableSetOf<String>()

        fun visit(el: JsonElement) {
            if (tracks.size >= limit) return
            when (el) {
                is JsonObject -> {
                    el["playlistPanelVideoRenderer"]?.let { renderer ->
                        (renderer as? JsonObject)?.let { parsePlaylistPanelVideo(it) }?.let { track ->
                            if (seen.add(track.id)) tracks.add(track)
                        }
                        return
                    }
                    el["musicResponsiveListItemRenderer"]?.let { renderer ->
                        (renderer as? JsonObject)?.let { parseWatchNextItem(it) }?.let { track ->
                            if (seen.add(track.id)) tracks.add(track)
                        }
                        return
                    }
                    el.values.forEach { visit(it) }
                }
                is JsonArray -> el.forEach { visit(it) }
                else -> Unit
            }
        }

        visit(root)
        return tracks
    }

    private fun parsePlaylistPanelVideo(obj: JsonObject): Track? {
        val videoId = obj.str("videoId") ?: return null
        val title = obj.obj("title")?.arr("runs")?.firstOrNull()?.jsonObject?.str("text")
            ?: obj.obj("title")?.str("simpleText") ?: return null
        val bylineRuns = obj.obj("shortBylineText")?.arr("runs")
            ?: obj.obj("longBylineText")?.arr("runs")
        // Byline runs are [artist, " • ", album, ...] — pick text runs only,
        // not separator runs, so album is not lost when separators shift.
        val bylineTexts = bylineRuns?.mapNotNull { (it as? JsonObject)?.str("text") }
            ?.map(String::trim)
            ?.filter { it.isNotEmpty() && it != "•" && it != "·" && it != "|" } ?: emptyList()
        val artist = bylineTexts.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Unknown artist"
        val album = bylineTexts.getOrNull(1)?.takeIf { it.isNotBlank() }
        val lengthText = obj.obj("lengthText")?.arr("runs")?.firstOrNull()?.jsonObject?.str("text")
            ?: obj.obj("lengthText")?.str("simpleText")
        val durationMs = lengthText?.let { parseDurationToMs(it) }

        val thumbs = obj.obj("thumbnail")?.arr("thumbnails")
        val art = (thumbs?.lastOrNull() as? JsonObject)?.str("url")

        return Track(
            id = videoId,
            title = title,
            artist = artist,
            album = album,
            artworkUrl = upgradeArtwork(art),
            durationMs = durationMs,
            source = "ytm",
        )
    }

    private fun parseWatchNextItem(obj: JsonObject): Track? {
        val item = parseSearchItem(obj) ?: return null
        return when (item) {
            is com.howdy.echowave.domain.model.SearchItem.Song -> item.track
            is com.howdy.echowave.domain.model.SearchItem.Video -> item.track
            else -> null
        }
    }

    private fun JsonObject.obj(vararg path: String): JsonObject? {
        var cur: JsonElement = this
        for (key in path) {
            cur = (cur as? JsonObject)?.get(key) ?: return null
        }
        return cur as? JsonObject
    }

    private fun JsonObject.arr(vararg path: String): JsonArray? {
        var cur: JsonElement = this
        for (key in path) {
            cur = (cur as? JsonObject)?.get(key) ?: return null
        }
        return cur as? JsonArray
    }

    private fun JsonObject.str(key: String): String? =
        (get(key) as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it.isString }?.content

    private fun parseDurationToMs(text: String): Long? {
        val raw = text.trim()
        if (raw.isEmpty()) return null
        if (!Regex("""^\d{1,3}(:\d{1,2}){1,2}$""").matches(raw)) return null
        val parts = raw.split(":").map { it.toLongOrNull() ?: return null }
        for (i in 1 until parts.size) {
            if (parts[i] !in 0..59) return null
        }
        var ms = 0L
        for (p in parts) ms = ms * 60 + p
        return ms * 1000
    }

    private fun upgradeArtwork(url: String?): String? {
        if (url == null) return null
        return if (url.contains("=w") && url.contains("-h")) {
            url.replace(Regex("=w\\d+-h\\d+"), "=w540-h540")
        } else url
    }
}
