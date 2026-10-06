package com.howdy.echowave.domain.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val artworkUrl: String? = null,
    val durationMs: Long? = null,
    val source: String = "ytm",
    /** True when the subtitle carries view counts (video result, not a track). */
    val isVideo: Boolean = false,
)

enum class RepeatMode { OFF, ALL, ONE }

data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int = 0,
)
