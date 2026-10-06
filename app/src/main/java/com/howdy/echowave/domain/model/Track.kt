package com.howdy.echowave.domain.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val artworkUrl: String? = null,
    val durationMs: Long? = null,
    val source: String = "ytm",
)

enum class RepeatMode { OFF, ALL, ONE }
