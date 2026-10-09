package com.howdy.echowave.data.remote.innertube

import com.howdy.echowave.domain.model.SearchItem
import com.howdy.echowave.domain.model.SearchResults
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.model.rank
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
    val raw = text.trim()
    if (raw.isEmpty()) return null
    // Strict m:ss / h:mm:ss: every part must be numeric, seconds/minutes < 60.
    if (!Regex("""^\d{1,3}(:\d{1,2}){1,2}$""").matches(raw)) return null
    val parts = raw.split(":").map { it.toLongOrNull() ?: return null }
    for (i in 1 until parts.size) {
        if (parts[i] !in 0..59) return null
    }
    var ms = 0L
    for (p in parts) ms = ms * 60 + p
    return ms * 1000
}

/** visitorData echoed by every InnerTube response; fed back on every request. */
fun extractVisitorData(root: JsonObject): String? =
    root.obj("responseContext")?.str("visitorData")

/**
 * Per-kind caps for fanned-out search: every shelf contributes, so videos
 * from the top shelf can no longer crowd out soundtrack songs below.
 * Null caps = legacy first-N-in-document-order (charts/album/mood paths).
 */
data class SearchCaps(
    val songs: Int = 20,
    val albums: Int = 6,
    val artists: Int = 6,
    val playlists: Int = 6,
    val videos: Int = 8,
    val episodes: Int = 6,
)

/** Per-kind caps for fan-out search; videos never crowd out songs. */
val SEARCH_CAPS = SearchCaps()

/** Hard bound on walked renderers; shelves beyond this are ignored. */
private const val MAX_WALK_ITEMS = 200

/**
 * Parses a `search` response into kinded items (songs, albums, artists,
 * playlists, videos, episodes) in server order.
 * Tolerates layout drift: skips items with neither a videoId nor a
 * browseId instead of failing all.
 * Top result prefers the card shelf (YouTube's own top hit), then the
 * first song, then the first item.
 */
fun parseSearchResponse(root: JsonObject, limit: Int = 25, caps: SearchCaps? = null): SearchResults {
    val cards = mutableListOf<SearchItem>()
    val out = mutableListOf<SearchItem>()
    val walkCap = if (caps == null) limit else MAX_WALK_ITEMS
    fun visit(el: JsonElement) {
        if (out.size + cards.size >= walkCap) return
        when (el) {
            is JsonObject -> {
                el["musicCardShelfRenderer"]?.let { card ->
                    // The card itself is the top hit; its content rows are
                    // visited below as regular section items (same as Echo).
                    parseCardShelf(card as JsonObject)?.let { if (cards.isEmpty()) cards += it }
                    el.values.forEach { visit(it) }
                    return
                }
                el["musicResponsiveListItemRenderer"]?.let { item ->
                    parseSearchItem(item as JsonObject)?.let { out += it }
                    return
                }
                el.values.forEach { visit(it) }
            }
            is JsonArray -> el.forEach { visit(it) }
            else -> Unit
        }
    }
    visit(root)
    val items = if (caps == null) out.take(limit) else applyCaps(out, caps)
    val top = cards.firstOrNull()
        ?: items.firstOrNull { it is SearchItem.Song }
        ?: items.firstOrNull()
    return SearchResults(topResult = top, items = items)
}

private fun applyCaps(items: List<SearchItem>, caps: SearchCaps): List<SearchItem> {
    val budget = intArrayOf(caps.songs, caps.albums, caps.artists, caps.playlists, caps.videos, caps.episodes)
    return items.filter { budget[it.rank()]-- > 0 }
}

/**
 * Top-result card shelf. Shapes mirror Echo-Music `SearchSummaryPage`
 * (`title`/`subtitle` runs, `thumbnail`, `onTap` endpoint); anything
 * unrecognized returns null and callers fall back to the first song.
 */
private fun parseCardShelf(shelf: JsonObject): SearchItem? {
    val title = shelf.obj("title")?.arr("runs")
        ?.firstOrNull()?.jsonObject?.str("text")?.takeIf { it.isNotBlank() } ?: return null
    val subRuns = shelf.obj("subtitle")?.arr("runs")
    val parts = (subRuns?.mapNotNull { (it as? JsonObject)?.str("text") } ?: emptyList())
        .map(String::trim)
        .filterNot { it.isEmpty() || it == "•" || it == "·" || it == "|" || it == "â€¢" }
    // " • " separators survive trimming as "•"; drop them by re-splitting.
    val flat = parts.flatMap { it.split("•", "·").map(String::trim) }.filter { it.isNotEmpty() }
    val kind = flat.firstOrNull()?.lowercase()
    val rawSub = flat.joinToString(" ")
    val thumbs = shelf.obj("thumbnail")?.obj("musicThumbnailRenderer")
        ?.obj("thumbnail")?.arr("thumbnails")
    val artwork = upgradeArtwork(
        (thumbs?.lastOrNull() as? JsonObject)?.str("url")
            ?: (thumbs?.firstOrNull() as? JsonObject)?.str("url"),
    )
    val tap = shelf.obj("navigationEndpoint") ?: shelf.obj("onTap") ?: return null
    tap.obj("watchEndpoint")?.str("videoId")?.let { videoId ->
        val isVideo = kind == "video" || VIEW_COUNT_RE.containsMatchIn(rawSub)
        val isEpisode = !isVideo && (
            kind == "episode" ||
                rawSub.contains("episode", ignoreCase = true) ||
                rawSub.contains("podcast", ignoreCase = true)
            )
        val artist = flat.getOrNull(1) ?: "Unknown artist"
        val album = flat.getOrNull(2)?.takeIf { !isVideo && !isEpisode }
        val durationMs = flat.lastOrNull()
            ?.takeIf { it.contains(":") }
            ?.let(::parseDurationToMs)
        val track = Track(
            id = videoId, title = title, artist = artist, album = album,
            artworkUrl = artwork, durationMs = durationMs, source = "ytm",
            isVideo = isVideo, isEpisode = isEpisode,
        )
        return when {
            isEpisode -> SearchItem.Episode(track)
            isVideo -> SearchItem.Video(track)
            else -> SearchItem.Song(track)
        }
    }
    val browseEndpoint = tap.obj("browseEndpoint") ?: return null
    val browseId = browseEndpoint.str("browseId") ?: return null
    val browseParams = browseEndpoint.str("params")
    val artist = flat.getOrNull(1) ?: flat.firstOrNull() ?: "Unknown artist"
    return when {
        kind == "album" || browseId.startsWith("MPRE") ->
            SearchItem.Album(browseId, title, artist, artwork, browseParams)
        kind == "playlist" || browseId.startsWith("VL") ->
            SearchItem.Playlist(browseId.removePrefix("VL"), title, artist, artwork)
        else -> SearchItem.Artist(browseId, title, artwork)
    }
}

/** Kinded equivalent of the old track-only parse; same subtitle signals. */
internal fun parseSearchItem(item: JsonObject): SearchItem? {
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
    val parts: List<String>
    if (subRuns != null) {
        val texts = subRuns.mapNotNull { (it as? JsonObject)?.str("text") }
            .filter { it != " • " }
        parts = texts.map(String::trim)
            .filterNot { it.isEmpty() || it in setOf("•", "â€¢", "·", "|") }
        val kind = parts.firstOrNull()?.lowercase()
        val hasItemKind = kind in setOf("song", "video", "album", "playlist", "artist", "episode")
        val rawSub = subRuns.mapNotNull { (it as? JsonObject)?.str("text") }.joinToString("")
        // Video results declare kind "Video" or show view counts ("1.2M views").
        // Episodes declare kind "Episode" or mention podcast/episode.
        // Word-boundary match avoids "review"/"preview" false positives.
        isVideo = kind == "video" || VIEW_COUNT_RE.containsMatchIn(rawSub)
        isEpisode = !isVideo && (
            kind == "episode" ||
                rawSub.contains("episode", ignoreCase = true) ||
                rawSub.contains("podcast", ignoreCase = true)
            )
        val artistIndex = if (hasItemKind) 1 else 0
        parts.getOrNull(artistIndex)?.let { artist = it }
        parts.getOrNull(artistIndex + 1)?.let { if (!isVideo && !isEpisode) album = it }
        // Browse kinds (album / playlist / artist rows) resolve via browseId.
        if (!isVideo && !isEpisode && kind in setOf("album", "playlist", "artist")) {
            val browseEndpoint = item.obj("navigationEndpoint")?.obj("browseEndpoint")
                ?: return null
            val browseId = browseEndpoint.str("browseId") ?: return null
            val thumbs = item.obj("thumbnail", "musicThumbnailRenderer", "thumbnail")?.arr("thumbnails")
            val artwork = upgradeArtwork(
                (thumbs?.lastOrNull() as? JsonObject)?.str("url")
                    ?: (thumbs?.firstOrNull() as? JsonObject)?.str("url"),
            )
            return when (kind) {
                "album" -> SearchItem.Album(
                    browseId, title, artist, artwork, browseEndpoint.str("params"),
                )
                "playlist" -> SearchItem.Playlist(
                    browseId.removePrefix("VL"), title, artist, artwork,
                )
                else -> SearchItem.Artist(browseId, title, artwork)
            }
        }
    } else {
        parts = emptyList()
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

    val track = Track(
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
    return when {
        isEpisode -> SearchItem.Episode(track)
        isVideo -> SearchItem.Video(track)
        else -> SearchItem.Song(track)
    }
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
    val browseParams = item.obj("navigationEndpoint")?.obj("browseEndpoint")?.str("params")
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
        browseParams = browseParams,
    )
}

private val ART_SIZE_RE = Regex("=w\\d+-h\\d+")

private val VIEW_COUNT_RE = Regex("""\b\d[\d.,KMB]*\s+views?\b""", RegexOption.IGNORE_CASE)

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
        val rate = o.jsonObject["bitrate"]?.jsonPrimitive?.content?.toLongOrNull()?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt() ?: 0
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
