package com.howdy.echowave.data.remote.innertube

import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.source.StreamInfo
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
    val parts = text.trim().split(":").mapNotNull { it.toLongOrNull() }
    if (parts.isEmpty()) return null
    var ms = 0L
    for (p in parts) ms = ms * 60 + p
    return ms * 1000
}

/** visitorData echoed by every InnerTube response; fed back on every request. */
fun extractVisitorData(root: JsonObject): String? =
    root.obj("responseContext")?.str("visitorData")

/**
 * Parses a `search` response into tracks.
 * Tolerates layout drift: skips items missing a videoId instead of failing all.
 */
fun parseSearchResponse(root: JsonObject, limit: Int = 25): List<Track> {
    val out = mutableListOf<Track>()
    fun visit(el: JsonElement) {
        if (out.size >= limit) return
        when (el) {
            is JsonObject -> {
                el["musicResponsiveListItemRenderer"]?.let { item ->
                    parseListItem(item as JsonObject)?.let { out += it }
                    return
                }
                el.values.forEach { visit(it) }
            }
            is JsonArray -> el.forEach { visit(it) }
            else -> Unit
        }
    }
    visit(root)
    return out
}

private fun parseListItem(item: JsonObject): Track? {
    val flex = item.arr("flexColumns") ?: return null
    val title = flex.getOrNull(0)?.jsonObject
        ?.obj("musicResponsiveListItemFlexColumnRenderer")
        ?.obj("text")?.arr("runs")
        ?.getOrNull(0)?.jsonObject?.str("text") ?: return null

    var artist = "Unknown artist"
    var album: String? = null
    var isVideo = false
    var isEpisode = false
    val subRuns = flex.getOrNull(1)?.jsonObject
        ?.obj("musicResponsiveListItemFlexColumnRenderer")
        ?.obj("text")?.arr("runs")
    if (subRuns != null) {
        val texts = subRuns.mapNotNull { (it as? JsonObject)?.str("text") }
            .filter { it != " • " }
        val parts = texts.map(String::trim)
            .filterNot { it.isEmpty() || it in setOf("•", "â€¢", "·", "|") }
        val kind = parts.firstOrNull()?.lowercase()
        val hasItemKind = kind in setOf("song", "video", "album", "playlist", "artist")
        val rawSub = subRuns.mapNotNull { (it as? JsonObject)?.str("text") }.joinToString("")
        // Video results declare kind "Video" or show view counts ("1.2M views").
        // Episodes declare kind "Episode" or mention podcast/episode.
        isVideo = kind == "video" || rawSub.contains("view", ignoreCase = true)
        isEpisode = !isVideo && (
            kind == "episode" ||
                rawSub.contains("episode", ignoreCase = true) ||
                rawSub.contains("podcast", ignoreCase = true)
            )
        val artistIndex = if (hasItemKind) 1 else 0
        parts.getOrNull(artistIndex)?.let { artist = it }
        parts.getOrNull(artistIndex + 1)?.let { if (!isVideo && !isEpisode) album = it }
    }

    val videoId = item.obj("playlistItemData")?.str("videoId")
        ?: item.obj("overlay", "musicItemThumbnailOverlayRenderer", "content", "musicPlayButtonRenderer", "playNavigationEndpoint", "watchEndpoint")?.str("videoId")
        ?: item.obj("navigationEndpoint", "watchEndpoint")?.str("videoId")
        ?: return null

    val thumbs = item.obj("thumbnail", "musicThumbnailRenderer", "thumbnail")?.arr("thumbnails")
    val rawArt = (thumbs?.lastOrNull() as? JsonObject)?.str("url")
        ?: (thumbs?.firstOrNull() as? JsonObject)?.str("url")
    // Largest thumb, then upscale the size suffix (w60 -> w540) so big
    // artwork doesn't pixelate; unknown shapes pass through untouched.
    val artwork = upgradeArtwork(rawArt)

    val durationText = item.arr("fixedColumns")
        ?.firstOrNull()?.jsonObject
        ?.obj("musicResponsiveListItemFixedColumnRenderer")
        ?.obj("text")?.arr("runs")
        ?.firstOrNull()?.jsonObject?.str("text")
    val durationMs = durationText?.let(::parseDurationToMs)

    return Track(
        id = videoId,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artwork,
        durationMs = durationMs,
        source = "ytm",
        isVideo = isVideo,
        isEpisode = isEpisode,
    )
}

/** Display art at 540px when the URL carries a size suffix (=w60-h60). Pure + tested. */
fun upgradeArtwork(url: String?): String? {
    if (url == null) return null
    return if (ART_SIZE_RE.containsMatchIn(url)) {
        url.replace(ART_SIZE_RE, "=w540-h540")
    } else {
        url
    }
}

/**
 * New-release / chart albums: musicTwoRowItemRenderer cards.
 * browseId from navigationEndpoint, artist from subtitle runs. Pure + tested.
 */
fun parseTwoRowAlbums(root: JsonObject, limit: Int = 25): List<com.howdy.echowave.domain.model.Album> {
    val out = mutableListOf<com.howdy.echowave.domain.model.Album>()
    fun visit(el: JsonElement) {
        if (out.size >= limit) return
        when (el) {
            is JsonObject -> {
                el["musicTwoRowItemRenderer"]?.let { item ->
                    parseTwoRowAlbum(item as JsonObject)?.let { out += it }
                    return
                }
                el.values.forEach { visit(it) }
            }
            is JsonArray -> el.forEach { visit(it) }
            else -> Unit
        }
    }
    visit(root)
    return out
}

private fun parseTwoRowAlbum(item: JsonObject): com.howdy.echowave.domain.model.Album? {
    val title = item.obj("title")?.arr("runs")
        ?.firstOrNull()?.jsonObject?.str("text") ?: return null
    val subRuns = item.obj("subtitle")?.arr("runs") ?: return null
    val texts = subRuns.mapNotNull { (it as? JsonObject)?.str("text") }
        .filter { it != " • " }
    val artist = texts.getOrNull(1) ?: texts.firstOrNull() ?: return null
    val browseId = item.obj("navigationEndpoint")?.obj("browseEndpoint")?.str("browseId")
        ?: return null
    val thumbs = item.obj("thumbnailRenderer")
        ?.obj("musicThumbnailRenderer")
        ?.obj("thumbnail")?.arr("thumbnails")
        ?: item.obj("thumbnail")?.arr("thumbnails")
    val art = (thumbs?.lastOrNull() as? JsonObject)?.str("url")
    return com.howdy.echowave.domain.model.Album(
        id = browseId,
        title = title,
        artist = artist,
        artworkUrl = upgradeArtwork(art),
    )
}

private val ART_SIZE_RE = Regex("=w\\d+-h\\d+")

/**
 * Mood & genre buttons: gridRenderer.musicNavigationButtonRenderer cards.
 * browseId (+params) from clickCommand; stripe color when present.
 * Pure + tested.
 */
fun parseMoodGenres(root: JsonObject, limit: Int = 40): List<com.howdy.echowave.domain.model.Genre> {
    val out = mutableListOf<com.howdy.echowave.domain.model.Genre>()
    fun visit(el: JsonElement) {
        if (out.size >= limit) return
        when (el) {
            is JsonObject -> {
                el["musicNavigationButtonRenderer"]?.let { item ->
                    parseMoodGenre(item as JsonObject)?.let { out += it }
                    return
                }
                el.values.forEach { visit(it) }
            }
            is JsonArray -> el.forEach { visit(it) }
            else -> Unit
        }
    }
    visit(root)
    return out
}

private fun parseMoodGenre(item: JsonObject): com.howdy.echowave.domain.model.Genre? {
    val title = item.obj("buttonText")?.arr("runs")
        ?.firstOrNull()?.jsonObject?.str("text") ?: return null
    val endpoint = item.obj("clickCommand")?.obj("browseEndpoint") ?: return null
    val browseId = endpoint.str("browseId") ?: return null
    val params = endpoint.str("params")
    val color = item.obj("solid")?.let {
        it["leftStripeColor"]?.jsonPrimitive?.content?.toLongOrNull()
    }
    return com.howdy.echowave.domain.model.Genre(
        id = browseId,
        title = title,
        color = color,
        params = params,
    )
}

/** Result of parsing a `player` response. */
sealed interface PlayerParse {
    data class Playable(val info: StreamInfo) : PlayerParse
    data class Unplayable(val reason: String) : PlayerParse
}

/**
 * Picks the best audio-only adaptive format with a direct URL.
 * Signature-ciphered formats need the decipher port (donor: Echo-Music innertube)
 * and are reported as such instead of silently failing.
 */
fun parsePlayerResponse(videoId: String, root: JsonObject): PlayerParse {
    val status = root.obj("playabilityStatus")?.str("status") ?: "UNKNOWN"
    if (status != "OK") {
        val reason = root.obj("playabilityStatus")?.str("reason") ?: status
        return PlayerParse.Unplayable("playability=$reason")
    }
    val formats = root.obj("streamingData")?.arr("adaptiveFormats") ?: JsonArray(emptyList())
    var ciphered = 0
    var bestUrl: String? = null
    var bestMime: String? = null
    var bestRate = -1
    for (f in formats) {
        val o = f as? JsonObject ?: continue
        val mime = o.str("mimeType") ?: continue
        if (!mime.startsWith("audio/")) continue
        val url = o.str("url")
        if (url == null) {
            if (o["signatureCipher"] != null) ciphered++
            continue
        }
        val rate = o.jsonObject["bitrate"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        if (rate > bestRate) {
            bestRate = rate
            bestUrl = url
            bestMime = mime
        }
    }
    if (bestUrl != null) {
        // Proof-of-Origin token lives OUTSIDE streamingData, in
        // serviceIntegrityDimensions.poToken. A signed URL without its pot
        // is rejected with 403 even though sig/sparams are intact.
        val poToken = root.obj("serviceIntegrityDimensions")?.str("poToken")
        return PlayerParse.Playable(StreamInfo(videoId, bestUrl, bestMime, poToken = poToken))
    }
    return PlayerParse.Unplayable(
        if (ciphered > 0) "ciphered-only ($ciphered formats need decipher port)"
        else "no audio formats",
    )
}
