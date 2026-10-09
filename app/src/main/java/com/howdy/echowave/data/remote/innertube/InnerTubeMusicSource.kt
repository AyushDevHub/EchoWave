package com.howdy.echowave.data.remote.innertube

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.core.network.EchoWaveError
import com.howdy.echowave.core.network.userMessage
import com.howdy.echowave.domain.model.SearchFilter
import com.howdy.echowave.domain.model.SearchItem
import com.howdy.echowave.domain.model.SearchResults
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.model.TrackIdentity
import com.howdy.echowave.domain.model.stableId
import com.howdy.echowave.domain.source.MusicSource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Production InnerTube source.
 * Port target for donor decipher logic (Echo-Music `innertube/`, GPL-3.0):
 * ciphered-only player responses are surfaced, not guessed.
 */
class InnerTubeMusicSource(
    private val api: InnerTubeApi,
    private val visitors: VisitorStore = VisitorStore.inMemory(),
) : MusicSource {
    override val name = "innertube-primary"

    /**
     * Fan-out search (LastWave pattern): one unfiltered query plus
     * songs-only and albums-only filtered queries, run concurrently and
     * merged. Soundtrack songs buried in lower shelves of the mixed
     * response surface via the songs-filtered query. Each leg is
     * fail-soft — a dead leg never fails the whole search.
     */
    override suspend fun search(query: String, limit: Int): AppResult<SearchResults> {
        searchLog("search start")
        return try {
            val merged = coroutineScope {
                val base = async { searchOnce(query, null, limit) }
                val songs = async {
                    searchOnce(query, SearchFilter.SONGS, SONGS_FANOUT_LIMIT)
                }
                val albums = async {
                    searchOnce(query, SearchFilter.ALBUMS, ALBUMS_FANOUT_LIMIT)
                }
                val baseResults = base.await()
                if (baseResults == null) {
                    return@coroutineScope null
                }
                val songResults = songs.await()
                val albumResults = albums.await()
                // Older film catalogs are often indexed as soundtrack albums
                // or as songs under an expanded query. Fetch both surfaces;
                // either leg can fail without taking down the primary search.
                val soundtrackQueries = if (query.trim().length >= 5) {
                    coroutineScope {
                        val album = async {
                            searchOnce("${query.trim()} soundtrack", SearchFilter.ALBUMS, ALBUMS_FANOUT_LIMIT)
                        }
                        val hindiMovieSongs = async {
                            searchOnce("${query.trim()} Hindi movie songs", SearchFilter.SONGS, SONGS_FANOUT_LIMIT)
                        }
                        val songs = async {
                            searchOnce("${query.trim()} songs", SearchFilter.SONGS, SONGS_FANOUT_LIMIT)
                        }
                        val originalSoundtrack = async {
                            searchOnce(
                                "${query.trim()} original motion picture soundtrack",
                                SearchFilter.SONGS,
                                SONGS_FANOUT_LIMIT,
                            )
                        }
                        listOfNotNull(
                            album.await(), hindiMovieSongs.await(), songs.await(), originalSoundtrack.await(),
                        )
                    }
                } else emptyList()
                val seen = LinkedHashMap<String, SearchItem>()
                // Keep secondary top-result cards too: filtered album searches
                // frequently return the matching soundtrack only as a card,
                // with no regular shelf rows to merge.
                // Put songs returned by soundtrack-specific searches first. A
                // bare movie-title search often ranks same-named music videos
                // or unrelated regional songs above the film's actual OST.
                val legs = soundtrackQueries.drop(1) + soundtrackQueries.take(1) +
                    listOfNotNull(songResults, albumResults, baseResults)
                // The card is often a same-title video/song unrelated to the
                // soundtrack. Keep cards for the dedicated top slot below;
                // merge actual shelf rows in focused-query order.
                legs.flatMap { it.items }
                    .forEach { seen.putIfAbsent(it.searchMergeKey(), it) }
                // Prefer a soundtrack album card over an unrelated artist/song
                // card from the bare movie-title query.
                val top = (soundtrackQueries.firstNotNullOfOrNull { it.topResult as? SearchItem.Album })
                    ?: (albumResults?.topResult as? SearchItem.Album)
                    ?: baseResults.topResult
                    ?: seen.values.firstOrNull { it is SearchItem.Song }
                    ?: seen.values.firstOrNull()
                SearchResults(topResult = top, items = seen.values.toList())
            }
            if (merged == null) {
                return AppResult.Err("Search unavailable. Check your connection and retry.")
            }
            AppResult.Ok(merged)
        } catch (e: Exception) {
            AppResult.Err(
                EchoWaveError.Network(e.message ?: "search failed").userMessage(),
                e,
            )
        }
    }

    private suspend fun searchOnce(
        query: String,
        filter: SearchFilter?,
        limit: Int,
    ): SearchResults? {
        return try {
            val root = api.search(
                clientId = INNERTUBE_CLIENT_ID,
                clientVersion = INNERTUBE_CLIENT_VERSION,
                body = searchBody(query, visitors.current(), filter?.params),
            )
            visitors.offer(extractVisitorData(root))
            parseSearchResponse(root, limit, SEARCH_CAPS)
        } catch (e: Exception) {
            searchLog("fan-out leg failed filter=$filter: ${e.message}")
            null
        }
    }

    override suspend fun searchFiltered(
        query: String,
        filter: SearchFilter,
        limit: Int,
    ): AppResult<SearchResults> {
        return try {
            val root = api.search(
                clientId = INNERTUBE_CLIENT_ID,
                clientVersion = INNERTUBE_CLIENT_VERSION,
                body = searchBody(query, visitors.current(), filter.params),
            )
            visitors.offer(extractVisitorData(root))
            AppResult.Ok(parseSearchResponse(root, limit, SEARCH_CAPS))
        } catch (e: Exception) {
            AppResult.Err(
                EchoWaveError.Network(e.message ?: "search failed").userMessage(),
                e,
            )
        }
    }

    override suspend fun getTrack(id: String): AppResult<Track> {
        // Direct lookup by ID: search treats the ID as a query which rarely
        // matches. Try exact search first, then fall back to not-found.
        // Full player/browse lookup is a future enhancement.
        if (id.isBlank()) return AppResult.Err("track not found")
        return when (val r = search(id, 10)) {
            is AppResult.Ok -> r.value.tracks.firstOrNull { it.id == id }?.let { AppResult.Ok(it) }
                ?: AppResult.Err("track not found")
            is AppResult.Err -> r
        }
    }

    companion object {
        private const val SONGS_FANOUT_LIMIT = 15
        private const val ALBUMS_FANOUT_LIMIT = 10
    }
}

private fun SearchItem.searchMergeKey(): String = when (this) {
    is SearchItem.Song -> "song-identity:${TrackIdentity.from(track)}"
    else -> stableId()
}

/** Swappable in unit tests (android.util.Log is unavailable there). */
internal var searchLog: (String) -> Unit = { android.util.Log.d("EchoWaveSearch", it) }
