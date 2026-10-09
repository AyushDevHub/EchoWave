package com.howdy.echowave.data.remote.potoken

/**
 * Pure token-store policy: one streaming (session-bound) pot per visitor session,
 * expiry with safety margin, explicit invalidation. Clock injectable for tests.
 * Port of the policy in Echo-Music's PoTokenGenerator/PoTokenWebView.
 */
class PoTokenCache(
    private val expiryMarginMs: Long = 10 * 60 * 1000L,
    private val clockMs: () -> Long = System::currentTimeMillis,
) {
    private var sessionId: String? = null
    private var streamingPot: String? = null
    private var expiresAtMs: Long = 0L

    @Synchronized
    fun get(sessionId: String): String? {
        if (this.sessionId != sessionId) return null
        if (clockMs() >= expiresAtMs) return null
        return streamingPot
    }

    @Synchronized
    fun put(sessionId: String, streamingPot: String, expiresInSec: Long) {
        this.sessionId = sessionId
        this.streamingPot = streamingPot
        val now = clockMs()
        val computed = now + expiresInSec * 1000L - expiryMarginMs
        // If server expiry minus margin is already past (tiny expiry with
        // large margin), floor to a short fresh window to avoid mint loops.
        // Honest small expiries with zero margin still expire on time.
        this.expiresAtMs = if (computed <= now) now + MIN_FRESH_MS else computed
    }

    @Synchronized
    fun clear() {
        sessionId = null
        streamingPot = null
        expiresAtMs = 0L
    }

    companion object {
        /** Floor when computed expiry is already past: avoids mint loops. */
        const val MIN_FRESH_MS = 60_000L
    }
}
