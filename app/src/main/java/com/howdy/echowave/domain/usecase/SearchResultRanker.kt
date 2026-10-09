package com.howdy.echowave.domain.usecase

import com.howdy.echowave.core.common.TextMatch
import com.howdy.echowave.domain.model.SearchItem
import com.howdy.echowave.domain.model.SearchResults
import com.howdy.echowave.domain.model.stableId

/** Pure ranking policy. Provider parsing and stream resolution remain outside this class. */
object SearchResultRanker {
    fun rank(results: SearchResults, query: String): SearchResults {
        if (query.isBlank()) return results
        val sourceItems = results.items.toMutableList().apply {
            val top = results.topResult
            if (top != null && none { it.stableId() == top.stableId() }) add(top)
        }
        if (sourceItems.isEmpty()) return results.copy(items = sourceItems)
        val intent = SearchQuery.parse(query)
        val titleQuery = intent.title

        val matchingMovieAlbums = sourceItems.filterIsInstance<SearchItem.Album>()
            .filter { queryMatchesTitle(it.title, titleQuery) }
        val matchingMovieAlbum = matchingMovieAlbums.isNotEmpty()
        val songs = sourceItems.filterIsInstance<SearchItem.Song>().map { it.track }
        val movieAlbumArtwork = matchingMovieAlbums.mapNotNull { artworkKey(it.artworkUrl) }.toSet()
        fun isMovieTrack(track: com.howdy.echowave.domain.model.Track): Boolean =
            track.album?.let { queryMatchesTitle(it, titleQuery) } == true ||
                artworkKey(track.artworkUrl)?.let { it in movieAlbumArtwork } == true
        val rankedSongs = when {
            songs.isEmpty() -> emptyList()
            matchingMovieAlbum -> {
                val related = songs.filter(::isMovieTrack)
                related + songs.filterNot(::isMovieTrack)
            }
            else -> {
                val confidentTitleHit = songs.any { TextMatch.similarity(it.title, titleQuery) >= 85 }
                val hasAlbumHit = songs.any { it.album?.let { album -> queryMatchesTitle(album, titleQuery) } == true }
                if (!confidentTitleHit && !hasAlbumHit) songs
                else songs.withIndex().sortedWith(
                    compareByDescending<IndexedValue<com.howdy.echowave.domain.model.Track>> {
                        val track = it.value
                        var score = TextMatch.matchScore(track.title, track.artist, titleQuery, intent.artist.orEmpty())
                        if (track.album?.let { album -> queryMatchesTitle(album, titleQuery) } == true) {
                            score += 3_000
                        }
                        score
                    }.thenBy { it.index },
                ).map { it.value }
            }
        }

        val songIterator = rankedSongs.iterator()
        val items = sourceItems.map { item ->
            if (item is SearchItem.Song) SearchItem.Song(songIterator.next()) else item
        }
        val top = if (matchingMovieAlbum) {
            val related = rankedSongs.firstOrNull(::isMovieTrack)
            // A generic result whose title is only the movie name is often a video or
            // a weak query echo. Preserve the focused shelf order until a soundtrack
            // relationship (album/artwork) identifies a more reliable song.
            val titleMatch = rankedSongs.firstOrNull {
                TextMatch.similarity(it.title, titleQuery) >= 50 &&
                    TextMatch.similarity(it.title, titleQuery) < 85
            }
            val matchedAlbum = matchingMovieAlbums.firstOrNull()
            (related ?: titleMatch ?: rankedSongs.firstOrNull())?.let(SearchItem::Song)
                ?: matchedAlbum ?: results.topResult
        } else {
            val playable = items.mapNotNull { item ->
                when (item) {
                    is SearchItem.Song -> item
                    is SearchItem.Video -> item
                    is SearchItem.Episode -> item
                    else -> null
                }
            }
            val best = playable.maxWithOrNull(
                compareBy<SearchItem> { item ->
                    val track = when (item) {
                        is SearchItem.Song -> item.track
                        is SearchItem.Video -> item.track
                        is SearchItem.Episode -> item.track
                        else -> error("unreachable")
                    }
                    TextMatch.matchScore(track.title, track.artist, titleQuery, intent.artist.orEmpty()) +
                        if (track.album?.let { queryMatchesTitle(it, titleQuery) } == true) 3_000 else 0
                }.thenBy { if (it is SearchItem.Song) 1 else 0 },
            )
            val bestTrack = best?.let { item ->
                when (item) {
                    is SearchItem.Song -> item.track
                    is SearchItem.Video -> item.track
                    is SearchItem.Episode -> item.track
                    else -> error("unreachable")
                }
            }
            val hasAlbumEvidence = bestTrack?.album?.let { queryMatchesTitle(it, titleQuery) } == true
            if (bestTrack != null && (TextMatch.similarity(bestTrack.title, titleQuery) >= 50 || hasAlbumEvidence)) {
                best
            } else {
                when (results.topResult) {
                    is SearchItem.Album, is SearchItem.Artist, is SearchItem.Playlist -> results.topResult
                    else -> null
                }
            }
        }
        return results.copy(topResult = top, items = items)
    }

    private fun artworkKey(url: String?): String? {
        if (url == null) return null
        // Strip only known thumbnail size suffixes (=w60-h60, =s100, =w540),
        // not arbitrary "=x123" tails that may be part of a signed URL.
        val noParams = url.substringBefore('?')
        val stripped = noParams
            .replace(Regex("=w\\d+(-h\\d+)?$"), "")
            .replace(Regex("=s\\d+$"), "")
        return stripped.trimEnd('=').takeIf(String::isNotBlank)
    }
}
