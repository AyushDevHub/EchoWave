package com.howdy.echowave

import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.recommendation.CandidateContext
import com.howdy.echowave.domain.recommendation.CandidatePipeline
import com.howdy.echowave.domain.recommendation.CandidateSource
import com.howdy.echowave.domain.recommendation.TrackCandidate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CandidatePipelineTest {
    private fun track(id: String, title: String = id, artist: String = "Artist") = Track(id, title, artist)
    private fun source(vararg tracks: Track) = object : CandidateSource {
        override val id = "test"
        override suspend fun fetch(context: CandidateContext, limit: Int) =
            AppResult.Ok(tracks.map { TrackCandidate(it, sourceId = id) })
    }

    @Test fun `filters seed junk videos and artist overload then dedupes identities`() = runBlocking {
        val pipeline = CandidatePipeline(listOf(source(
            track("seed", "Seed", "Singer"),
            track("junk", "Interview about music"),
            track("v", "A video", "Channel").copy(isVideo = true),
            track("a1", "A", "Same Artist"),
            track("a2", "B", "Same Artist"),
            track("a3", "C", "Same Artist"),
            track("a4", "D", "Same Artist"),
            track("dup", "A", "Same Artist"),
            track("other", "E", "Other Artist"),
        )))
        val result = pipeline.recommend(CandidateContext(seed = track("seed", "Seed", "Singer")))
        assertTrue(result is AppResult.Ok)
        assertEquals(listOf("a1", "a2", "a3", "other"),
            (result as AppResult.Ok).value.map { it.track.id })
    }

    @Test fun `sort priority is relevance then taste then freshness and stable`() = runBlocking {
        val candidates = listOf(
            TrackCandidate(track("first"), sourceId = "test", relevanceScore = 10, tasteScore = 0, publishedAtEpochMs = 1),
            TrackCandidate(track("fresh"), sourceId = "test", relevanceScore = 10, tasteScore = 0, publishedAtEpochMs = 3),
            TrackCandidate(track("taste"), sourceId = "test", relevanceScore = 10, tasteScore = 1, publishedAtEpochMs = 0),
            TrackCandidate(track("relevance"), sourceId = "test", relevanceScore = 11, tasteScore = 0, publishedAtEpochMs = 0),
        )
        val candidateSource = object : CandidateSource {
            override val id = "scores"
            override suspend fun fetch(context: CandidateContext, limit: Int) = AppResult.Ok(candidates)
        }
        val result = CandidatePipeline(listOf(candidateSource), filters = emptyList())
            .recommend(CandidateContext(seed = track("seed"))) as AppResult.Ok
        assertEquals(listOf("relevance", "taste", "fresh", "first"), result.value.map { it.track.id })
    }

    @Test fun `source errors are fail soft but all failed returns an error`() = runBlocking {
        val broken = object : CandidateSource {
            override val id = "broken"
            override suspend fun fetch(context: CandidateContext, limit: Int): AppResult<List<TrackCandidate>> =
                AppResult.Err("offline")
        }
        val healthy = source(track("good"))
        val ok = CandidatePipeline(listOf(broken, healthy)).recommend(CandidateContext(track("seed")))
        assertTrue(ok is AppResult.Ok)
        val fail = CandidatePipeline(listOf(broken)).recommend(CandidateContext(track("seed")))
        assertTrue(fail is AppResult.Err)
    }

    @Test fun `no sources is an empty successful result`() = runBlocking {
        val result = CandidatePipeline(emptyList()).recommend(CandidateContext(track("seed")))
        assertEquals(emptyList<TrackCandidate>(), (result as AppResult.Ok).value)
    }
}
