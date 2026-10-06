package com.howdy.echowave.data.remote.innertube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerClientTest {
    @Test fun `selects client from c param`() {
        assertEquals(PlayerClient.ANDROID, PlayerClient.forStreamUrl("https://x.googlevideo.com/v?c=ANDROID&cver=1"))
        assertEquals(PlayerClient.ANDROID_MUSIC, PlayerClient.forStreamUrl("https://x.googlevideo.com/v?c=ANDROID_MUSIC"))
        assertEquals(PlayerClient.IOS, PlayerClient.forStreamUrl("https://x.googlevideo.com/v?c=IOS"))
        assertEquals(PlayerClient.WEB_REMIX, PlayerClient.forStreamUrl("https://x.googlevideo.com/v?c=WEB_REMIX"))
        assertEquals(PlayerClient.TVHTML5, PlayerClient.forStreamUrl("https://x.googlevideo.com/v?c=TVHTML5"))
        assertEquals(PlayerClient.VISIONOS, PlayerClient.forStreamUrl("https://x.googlevideo.com/v?c=VISIONOS"))
    }

    @Test fun `unknown or unparsable falls back to IOS`() {
        assertEquals(PlayerClient.IOS, PlayerClient.forStreamUrl("https://x.googlevideo.com/v?c=FUTURE"))
        assertEquals(PlayerClient.IOS, PlayerClient.forStreamUrl("https://x.googlevideo.com/v"))
        assertEquals(PlayerClient.IOS, PlayerClient.forStreamUrl("not a url"))
    }

    @Test fun `native clients send no origin, web clients do`() {
        val native = PlayerClient.ANDROID.mediaHeaders()
        assertFalse(native.containsKey("Origin"))
        assertFalse(native.containsKey("Referer"))
        val web = PlayerClient.WEB_REMIX.mediaHeaders()
        assertEquals("https://music.youtube.com", web["Origin"])
        assertEquals("https://music.youtube.com/", web["Referer"])
        assertTrue(web["User-Agent"]!!.isNotBlank())
    }

    @Test fun `range sizes per client`() {
        assertEquals(512L * 1024, PlayerClient.rangeBytesFor("https://x.googlevideo.com/v?c=ANDROID_VR"))
        assertEquals(1024L * 1024, PlayerClient.rangeBytesFor("https://x.googlevideo.com/v?c=ANDROID"))
        assertEquals(Long.MAX_VALUE, PlayerClient.rangeBytesFor("https://example.com/v?c=ANDROID"))
    }
}
