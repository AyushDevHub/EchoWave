package com.howdy.echowave.data.remote.innertube

/**
 * Per-track client exclusion from fetch refusals (donor: InnerTubeXResolver
 * excluded map). A googlevideo 403 on fetch excludes that minting client for
 * the track for 10 minutes so resolve picks another client instead of
 * retrying the dead one every tap. Clock injectable for tests.
 */
class StreamRegistry(
    private val clockMs: () -> Long = System::currentTimeMillis,
) {
    private val minted = HashMap<String, Minted>()
    private val excluded = HashMap<String, MutableMap<String, Long>>()

    private data class Minted(val videoId: String, val client: String)

    @Synchronized
    fun record(url: String, videoId: String, client: String) {
        if (minted.size >= MAX_REMEMBERED) {
            // Evict oldest single entry (insertion order), not the whole map.
            val eldest = minted.keys.firstOrNull()
            if (eldest != null) minted.remove(eldest)
        }
        minted[url] = Minted(videoId, client)
    }

    /** Returns the videoId when [url] was minted by resolve, else null. */
    @Synchronized
    fun onRefused(url: String): String? {
        val exact = minted.remove(url)
        val m = exact ?: run {
            val norm = normalizeUrl(url)
            val hitKey = minted.keys.firstOrNull { normalizeUrl(it) == norm }
            if (hitKey != null) minted.remove(hitKey) else null
        } ?: return null
        excluded.getOrPut(m.videoId) { HashMap() }[m.client] =
            clockMs() + EXCLUDE_MS
        return m.videoId
    }

    /** Clients currently refused for [videoId] (expired entries pruned). */
    @Synchronized
    fun excludedFor(videoId: String): Set<String> {
        val entries = excluded[videoId] ?: return emptySet()
        val now = clockMs()
        entries.entries.removeAll { it.value <= now }
        return entries.keys.toSet()
    }

    companion object {
        private const val EXCLUDE_MS = 10 * 60 * 1000L
        private const val MAX_REMEMBERED = 64

        /** Sort query params so reordered/encoded URLs still match minted entries. */
        internal fun normalizeUrl(url: String): String {
            return try {
                val q = url.indexOf('?')
                if (q < 0) return url
                val base = url.substring(0, q)
                val query = url.substring(q + 1).split('&').filter { it.isNotEmpty() }.sorted().joinToString("&")
                "$base?$query"
            } catch (_: Exception) {
                url
            }
        }
    }
}
