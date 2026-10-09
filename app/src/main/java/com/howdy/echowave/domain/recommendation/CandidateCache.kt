package com.howdy.echowave.domain.recommendation

import com.howdy.echowave.domain.model.Track
import java.util.LinkedHashMap

/**
 * Thread-safe bounded LRU memory cache for candidate graphs.
 * Minimizes network round-trips when songs or albums are revisited.
 */
class CandidateCache(
    private val maxEntries: Int = 30,
    private val clockMs: () -> Long = System::currentTimeMillis,
) {
    private val cache = object : LinkedHashMap<String, CacheEntry>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, CacheEntry>): Boolean {
            return size > maxEntries
        }
    }

    private data class CacheEntry(
        val tracks: List<Track>,
        val cachedAt: Long,
    )

    @Synchronized
    fun get(seedId: String, maxAgeMs: Long = DEFAULT_TTL_MS): List<Track>? {
        val entry = cache[seedId] ?: return null
        if (clockMs() - entry.cachedAt > maxAgeMs) {
            cache.remove(seedId)
            return null
        }
        return entry.tracks
    }

    @Synchronized
    fun put(seedId: String, tracks: List<Track>) {
        if (tracks.isNotEmpty()) {
            cache[seedId] = CacheEntry(tracks, clockMs())
        }
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }

    companion object {
        const val DEFAULT_TTL_MS = 2 * 60 * 60 * 1000L // 2 hours
    }
}
