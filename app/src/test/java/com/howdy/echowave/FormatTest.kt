package com.howdy.echowave.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun `formats minutes and seconds`() {
        assertEquals("3:20", formatDuration(200_000))
        assertEquals("0:05", formatDuration(5_000))
        assertEquals("10:00", formatDuration(600_000))
    }

    @Test fun `unknown shows dash`() {
        assertEquals("–", formatDuration(null))
        assertEquals("–", formatDuration(0))
        assertEquals("–", formatDuration(-10))
    }
}
