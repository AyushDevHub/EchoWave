package com.howdy.echowave.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionRestoreTest {
    @Test fun `restores track from session metadata`() {
        val t = trackFromSession("vid1", "Song", "Artist", "Album")!!
        assertEquals("vid1", t.id)
        assertEquals("Song", t.title)
        assertEquals("Artist", t.artist)
        assertEquals("Album", t.album)
    }

    @Test fun `null mediaId restores nothing`() {
        assertNull(trackFromSession(null, "Song", "Artist", null))
        assertNull(trackFromSession("", "Song", "Artist", null))
    }

    @Test fun `blank metadata falls back`() {
        val t = trackFromSession("vid1", "", null, null)!!
        assertEquals("Unknown title", t.title)
        assertEquals("Unknown artist", t.artist)
    }

    @Test fun `router is null-safe with no owner`() {
        PlaybackRouter.clear()
        assertEquals(null, PlaybackRouter.onNext)
        assertEquals(false, PlaybackRouter.canNext())
        PlaybackRouter.install(next = {}, previous = {}, hasNext = { true }, hasPrevious = { false })
        assertEquals(true, PlaybackRouter.canNext())
        assertEquals(false, PlaybackRouter.canPrevious())
        PlaybackRouter.clear()
    }
}
