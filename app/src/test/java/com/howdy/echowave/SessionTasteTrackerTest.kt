package com.howdy.echowave

import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.recommendation.SessionTasteTracker
import com.howdy.echowave.domain.recommendation.TrackStyle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTasteTrackerTest {
    @Test
    fun reinforcesCompletedLanguageAndStyles() {
        val tracker = SessionTasteTracker()
        val t1 = Track("t1", "Beedi Party Dance", "Sunidhi Chauhan")
        val t2 = Track("t2", "Fevicol Se Dance Beat", "Mamta Sharma")

        tracker.record(t1, completed = true, earlySkip = false)
        tracker.record(t2, completed = true, earlySkip = false)

        assertTrue(tracker.languageAffinity("hindi") > 0.5f)
        assertTrue(tracker.styleAffinity(TrackStyle.DANCE_PARTY) > 0.5f)
        assertFalse(tracker.isArtistFatigued("Sunidhi Chauhan"))
    }

    @Test
    fun detectsSkipFatigueOnRepeatedEarlySkips() {
        val tracker = SessionTasteTracker()
        val t1 = Track("t1", "Song 1", "Artist A")
        val t2 = Track("t2", "Song 2", "Artist A")

        tracker.record(t1, completed = false, earlySkip = true)
        tracker.record(t2, completed = false, earlySkip = true)

        assertTrue(tracker.isArtistFatigued("Artist A"))
        assertFalse(tracker.isArtistFatigued("Artist B"))
    }

    @Test
    fun pivotsStyleAffinityWhenSkippedEarly() {
        val tracker = SessionTasteTracker()
        val t1 = Track("t1", "Dance Party 1", "Artist A")
        val t2 = Track("t2", "Dance Party 2", "Artist B")

        tracker.record(t1, completed = false, earlySkip = true)
        tracker.record(t2, completed = false, earlySkip = true)

        assertTrue(tracker.styleAffinity(TrackStyle.DANCE_PARTY) < 0f)
    }
}
