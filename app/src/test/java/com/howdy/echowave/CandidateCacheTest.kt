package com.howdy.echowave

import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.recommendation.CandidateCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CandidateCacheTest {
    @Test
    fun cachesAndRetrievesCandidates() {
        val cache = CandidateCache(maxEntries = 5)
        val seedId = "seed123"
        val candidates = listOf(
            Track("c1", "Candidate 1", "Artist 1"),
            Track("c2", "Candidate 2", "Artist 2"),
        )

        assertNull(cache.get(seedId))
        cache.put(seedId, candidates)

        val retrieved = cache.get(seedId)
        assertNotNull(retrieved)
        assertEquals(2, retrieved?.size)
        assertEquals("c1", retrieved?.first()?.id)
    }

    @Test
    fun evictsOldEntriesOnTtlExpiry() {
        val cache = CandidateCache(maxEntries = 5)
        val seedId = "seed123"
        cache.put(seedId, listOf(Track("c1", "Title", "Artist")))

        // Query with maxAgeMs = -1 (already expired)
        assertNull(cache.get(seedId, maxAgeMs = -1))
    }
}
