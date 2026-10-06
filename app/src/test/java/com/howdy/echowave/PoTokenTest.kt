package com.howdy.echowave.data.remote.potoken

import com.howdy.echowave.domain.source.PoToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PoTokenCacheTest {
    @Test fun `hit within expiry`() {
        var now = 1_000L
        val cache = PoTokenCache(clockMs = { now })
        cache.put("s", "POT", expiresInSec = 3600)
        now += 1000
        assertEquals("POT", cache.get("s"))
    }

    @Test fun `expiry is a miss`() {
        var now = 0L
        val cache = PoTokenCache(expiryMarginMs = 0, clockMs = { now })
        cache.put("s", "POT", expiresInSec = 10)
        now = 10_001L
        assertNull(cache.get("s"))
    }

    @Test fun `session change is a miss`() {
        val cache = PoTokenCache()
        cache.put("s1", "POT", expiresInSec = 3600)
        assertNull(cache.get("s2"))
    }

    @Test fun `clear empties`() {
        val cache = PoTokenCache()
        cache.put("s", "POT", expiresInSec = 3600)
        cache.clear()
        assertNull(cache.get("s"))
    }
}

private class FakeMinter(
    var streaming: Pair<String, Long> = "STREAM" to 3600L,
    var player: String = "PLAYER",
    var fail: Boolean = false,
    var streamingMints: Int = 0,
) : BotGuardMinter {
    override suspend fun mintStreamingPot(sessionId: String): Pair<String, Long> {
        if (fail) throw PoTokenException("mint boom")
        streamingMints++
        return streaming
    }

    override suspend fun mintPlayerPot(videoId: String): String {
        if (fail) throw PoTokenException("mint boom")
        return player
    }

    override fun close() {}
}

class WebViewPoTokenProviderTest {
    @Test fun `cache hit mints once`() = runBlocking {
        val fake = FakeMinter()
        val p = WebViewPoTokenProvider(null, openMinter = { fake })
        val first = p.getPoToken("v1")
        val second = p.getPoToken("v1")
        assertEquals(PoToken("PLAYER", "STREAM"), first)
        assertEquals(first, second)
        assertEquals(1, fake.streamingMints)
    }

    @Test fun `expiry refreshes`() = runBlocking {
        var now = 0L
        val cache = PoTokenCache(expiryMarginMs = 0, clockMs = { now })
        val fake = FakeMinter()
        val p = WebViewPoTokenProvider(null, cache = cache, openMinter = { fake })
        p.getPoToken("v1")
        now = 3_601_000L
        p.getPoToken("v1")
        assertEquals(2, fake.streamingMints)
    }

    @Test fun `minting failure resolves to null without throwing`() = runBlocking {
        val p = WebViewPoTokenProvider(null, openMinter = { FakeMinter(fail = true) })
        assertNull(p.getPoToken("v1"))
    }

    @Test fun `timeout resolves to null`() = runBlocking {
        val slow = object : BotGuardMinter {
            override suspend fun mintStreamingPot(sessionId: String): Pair<String, Long> {
                delay(5000)
                return "S" to 1L
            }

            override suspend fun mintPlayerPot(videoId: String): String = "P"
            override fun close() {}
        }
        val p = WebViewPoTokenProvider(null, timeoutMs = 100, openMinter = { slow })
        assertNull(p.getPoToken("v1"))
    }
}
