package com.howdy.echowave.domain.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val artworkUrl: String? = null,
    val durationMs: Long? = null,
    val source: String = "ytm",
    /** True when the subtitle carries view counts (video result, not a track). */
    val isVideo: Boolean = false,
    /** True when the subtitle marks a podcast episode. */
    val isEpisode: Boolean = false,
)

enum class RepeatMode { OFF, ALL, ONE }

data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int = 0,
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val browseParams: String? = null,
)

data class Genre(
    val id: String,
    val title: String,
    /** ARGB stripe or null. */
    val color: Long?,
    val params: String?,
)

/**
 * One search hit. Echo-Music `YTItem` equivalent: songs/videos resolve to
 * playable tracks, albums/artists/playlists resolve via browse endpoints.
 */
sealed interface SearchItem {
    data class Song(val track: Track) : SearchItem
    data class Album(
        val id: String,
        val title: String,
        val artist: String,
        val artworkUrl: String?,
        /** Browse endpoint params disambiguate some provider album pages. */
        val browseParams: String? = null,
    ) : SearchItem
    data class Artist(val id: String, val name: String, val artworkUrl: String?) : SearchItem
    data class Playlist(val id: String, val title: String, val author: String, val artworkUrl: String?) : SearchItem
    data class Video(val track: Track) : SearchItem
    data class Episode(val track: Track) : SearchItem
}

/** Fixed display rank: songs → albums → artists → playlists → videos → episodes. */
fun SearchItem.rank(): Int = when (this) {
    is SearchItem.Song -> 0
    is SearchItem.Album -> 1
    is SearchItem.Artist -> 2
    is SearchItem.Playlist -> 3
    is SearchItem.Video -> 4
    is SearchItem.Episode -> 5
}

/** Stable reorder into display rank; server order preserved within a kind. */
fun orderSearchItems(items: List<SearchItem>): List<SearchItem> =
    items.sortedWith(compareBy { it.rank() })

/** Stable merge key: same track/album across fan-out queries dedupes. */
fun SearchItem.stableId(): String = when (this) {
    is SearchItem.Song -> "song:${track.id}"
    is SearchItem.Video -> "video:${track.id}"
    is SearchItem.Episode -> "episode:${track.id}"
    is SearchItem.Album -> "album:$id"
    is SearchItem.Artist -> "artist:$id"
    is SearchItem.Playlist -> "playlist:$id"
}

data class SearchResults(
    val topResult: SearchItem? = null,
    val items: List<SearchItem> = emptyList(),
) {
    val songs: List<Track> get() = items.filterIsInstance<SearchItem.Song>().map { it.track }
    val videos: List<Track> get() = items.filterIsInstance<SearchItem.Video>().map { it.track }
    val episodes: List<Track> get() = items.filterIsInstance<SearchItem.Episode>().map { it.track }
    /** Playable tracks in parse order (songs + videos + episodes). */
    val tracks: List<Track> get() = songs + videos + episodes
    /** Display order for sectioned UI. */
    fun ordered(): List<SearchItem> = orderSearchItems(items)

    companion object {
        fun empty() = SearchResults()
    }
}
