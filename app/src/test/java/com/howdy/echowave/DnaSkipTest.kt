package com.howdy.echowave.playback

import com.howdy.echowave.domain.dna.ListeningEventType
import org.junit.Assert.assertEquals
import org.junit.Test

class DnaSkipTest {
    @Test fun `quick abandon with known duration is early`() {
        assertEquals(
            ListeningEventType.SKIP_EARLY,
            classifySkip(8_000L, 240_000L),
        )
    }

    @Test fun `long listen then skip is late even under 30s ratio check`() {
        assertEquals(
            ListeningEventType.SKIP_LATE,
            classifySkip(120_000L, 240_000L),
        )
    }

    @Test fun `unknown duration falls back to 30s threshold`() {
        assertEquals(ListeningEventType.SKIP_EARLY, classifySkip(10_000L, null))
        assertEquals(ListeningEventType.SKIP_LATE, classifySkip(60_000L, null))
    }
}
