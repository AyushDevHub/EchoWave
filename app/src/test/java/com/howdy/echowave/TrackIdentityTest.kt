package com.howdy.echowave

import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.model.TrackIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TrackIdentityTest {
    @Test fun `promotional labels do not split identity`() {
        assertEquals(
            TrackIdentity.from(Track("one", "Haule Haule", "Sukhwinder Singh")),
            TrackIdentity.from(Track("two", "Haule Haule (Official Video)", "Sukhwinder Singh")),
        )
    }

    @Test fun `meaningful versions remain separate`() {
        val original = TrackIdentity.from(Track("one", "Haule Haule", "Sukhwinder Singh"))
        val live = TrackIdentity.from(Track("two", "Haule Haule (Live)", "Sukhwinder Singh"))
        assertNotEquals(original, live)
    }

    @Test fun `unknown artist falls back to album then provider id`() {
        val sameAlbumA = TrackIdentity.from(Track("one", "Song", "Unknown artist", "Album"))
        val sameAlbumB = TrackIdentity.from(Track("two", "Song", "Unknown artist", "Album"))
        val noAlbumA = TrackIdentity.from(Track("one", "Song", "Unknown artist"))
        val noAlbumB = TrackIdentity.from(Track("two", "Song", "Unknown artist"))
        assertEquals(sameAlbumA, sameAlbumB)
        assertNotEquals(noAlbumA, noAlbumB)
    }
}
