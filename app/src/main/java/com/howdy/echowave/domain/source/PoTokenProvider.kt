package com.howdy.echowave.domain.source

/** Proof-of-Origin tokens for InnerTube playback. Null-safe by contract: never throws. */
data class PoToken(
    /** Bound to the session (visitor/dataSyncId); goes in player body serviceIntegrityDimensions. */
    val playerRequestPoToken: String,
    /** Bound to the session for WEB_REMIX; appended to stream URLs as &pot=. */
    val streamingDataPoToken: String,
)

interface PoTokenProvider {
    /** Returns tokens for [videoId], or null when minting is unavailable/failed. */
    suspend fun getPoToken(videoId: String): PoToken?
}

/** Default: no minting. Resolver behaves exactly as before. */
class NoOpPoTokenProvider : PoTokenProvider {
    override suspend fun getPoToken(videoId: String): PoToken? = null
}
