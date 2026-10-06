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
        this.expiresAtMs = clockMs() + expiresInSec * 1000L - expiryMarginMs
    }

    @Synchronized
    fun clear() {
        sessionId = null
        streamingPot = null
        expiresAtMs = 0L
    }
}
