package com.howdy.echowave

import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.model.TrackIdentity
import com.howdy.echowave.domain.recommendation.RecommendationScorer
import com.howdy.echowave.domain.recommendation.SessionTasteTracker
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationScorerTest {
    private val scorer = RecommendationScorer()

    @Test
    fun scoresSimilarSongHigherThanUnrelatedSong() {
        val seed = Track("s1", "Beedi Jalaile Dance", "Sunidhi Chauhan, Sukhwinder Singh", "Omkara")
        val relatedCandidate = Track("c1", "Fevicol Se Party Dance", "Mamta Sharma", "Dabangg 2")
        val unrelatedCandidate = Track("c2", "Silent Night Lofi Chill", "Unknown Ambient", "Chill")

        val (relatedScore, relExpl) = scorer.score(seed = seed, candidate = relatedCandidate)
        val (unrelatedScore, unrelExpl) = scorer.score(seed = seed, candidate = unrelatedCandidate)

        assertTrue(
            "Expected related score ($relatedScore) > unrelated score ($unrelatedScore)",
            relatedScore > unrelatedScore,
        )
        assertTrue(relExpl.similarityScore > unrelExpl.similarityScore)
    }

    @Test
    fun `bollywood dance seed ranks dance radio above devotional mismatch`() {
        val seed = Track("seed", "Beedi Jalaile", "Sunidhi Chauhan", "Omkara")
        val sheila = Track("sheila", "Sheila Ki Jawani", "Sunidhi Chauhan", "Tees Maar Khan")
        val disco = Track("disco", "Dard E Disco", "Sukhwinder Singh", "Om Shanti Om")
        val devotional = Track("shiv", "Jay Shiv Omakara", "Traditional", "Bhajan")

        val sheilaScore = scorer.score(seed, sheila).first
        val discoScore = scorer.score(seed, disco).first
        val devotionalScore = scorer.score(seed, devotional).first

        assertTrue("Dance track should rank above an unrelated devotional track", sheilaScore > devotionalScore)
        assertTrue("Dance track should rank above an unrelated devotional track", discoScore > devotionalScore)
    }

    @Test
    fun appliesHeavyPenaltyToRecentlyPlayedTracks() {
        val seed = Track("s1", "Beedi Jalaile", "Sunidhi Chauhan")
        val candidate = Track("c1", "Fevicol Se", "Mamta Sharma")

        val (scoreBefore, _) = scorer.score(seed, candidate)
        val (scoreAfter, explAfter) = scorer.score(
            seed = seed,
            candidate = candidate,
            recentlyPlayedIdentities = setOf(TrackIdentity.from(candidate)),
        )

        assertTrue("Expected score penalty for recently played", scoreAfter < scoreBefore)
        assertTrue(explAfter.penalties >= 100f)
    }

    @Test
    fun penalizesArtistWithFatigueFromSessionSkips() {
        val seed = Track("s1", "Song 1", "Artist A")
        val candidate = Track("c1", "Song 2", "Artist A")

        val session = SessionTasteTracker()
        session.record(Track("x1", "Skip 1", "Artist A"), completed = false, earlySkip = true)
        session.record(Track("x2", "Skip 2", "Artist A"), completed = false, earlySkip = true)

        val (score, expl) = scorer.score(seed = seed, candidate = candidate, session = session)
        assertTrue(expl.penalties >= 60f)
    }
}
