package com.howdy.echowave.domain.model

import com.howdy.echowave.core.common.TextMatch

/** Metadata identity for comparing catalog entries independently of provider IDs. */
data class TrackIdentity(
    val titleKey: String,
    val artistKey: String?,
    val versionKey: String,
    val albumFallbackKey: String? = null,
    val providerFallbackKey: String? = null,
) {
    companion object {
        fun from(track: Track): TrackIdentity {
            val titleTokens = TextMatch.normalize(TextMatch.baseTitle(track.title))
                .split(' ')
                .filter { it.isNotBlank() && it !in TextMatch.MATCH_NOISE_WORDS }
            val variants = TextMatch.tokens(track.title)
                .intersect(TextMatch.VARIANT_WORDS)
                .sorted()
            val artist = track.artist.takeUnless {
                it.isBlank() || it.equals("Unknown artist", ignoreCase = true)
            }?.let(TextMatch::normalize)?.takeIf(String::isNotBlank)
            val album = track.album?.takeIf(String::isNotBlank)?.let(TextMatch::normalize)
            return TrackIdentity(
                titleKey = titleTokens.joinToString(" "),
                artistKey = artist,
                versionKey = variants.joinToString(" "),
                albumFallbackKey = if (artist == null) album else null,
                providerFallbackKey = if (artist == null && album == null) track.id else null,
            )
        }
    }
}
