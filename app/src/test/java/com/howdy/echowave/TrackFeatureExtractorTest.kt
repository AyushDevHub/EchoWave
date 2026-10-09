package com.howdy.echowave

import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.recommendation.TrackFeatureExtractor
import com.howdy.echowave.domain.recommendation.TrackStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackFeatureExtractorTest {
    @Test
    fun extractsDancePartyStyleAndLanguage() {
        val track = Track(
            id = "t1",
            title = "Beedi Jalaile (Club Dance Remix)",
            artist = "Sunidhi Chauhan, Sukhwinder Singh",
            album = "Omkara",
        )
        val features = TrackFeatureExtractor.extract(track)

        assertEquals("hindi", features.language)
        assertTrue(features.styles.contains(TrackStyle.DANCE_PARTY))
        assertTrue(features.styles.contains(TrackStyle.REMIX_CLUB))
        assertTrue(features.baseTitleTokens.contains("beedi"))
    }

    @Test
    fun extractsRomanticMelodicStyle() {
        val track = Track(
            id = "t2",
            title = "Tum Hi Ho (Love Theme)",
            artist = "Arijit Singh",
            album = "Aashiqui 2",
        )
        val features = TrackFeatureExtractor.extract(track)

        assertEquals("hindi", features.language)
        assertTrue(features.styles.contains(TrackStyle.ROMANTIC_MELODIC))
    }

    @Test
    fun extractsEraFromYearOrDecadeString() {
        val track90s = Track(
            id = "t3",
            title = "Chhaiya Chhaiya 90s Classic",
            artist = "Sukhwinder Singh",
        )
        val features90s = TrackFeatureExtractor.extract(track90s)
        assertEquals("1990s", features90s.era)

        val trackYear = Track(
            id = "t4",
            title = "Desi Girl",
            artist = "Shankar Mahadevan",
            album = "Dostana 2008",
        )
        val featuresYear = TrackFeatureExtractor.extract(trackYear)
        assertEquals("2000s", featuresYear.era)
    }
}
