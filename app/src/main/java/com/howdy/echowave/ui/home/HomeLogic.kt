package com.howdy.echowave.ui.home

import java.time.LocalTime

/** 5-11 morning, 12-16 afternoon, 17-21 evening, else night. Pure + tested. */
fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

fun currentGreeting(): String {
    return try {
        greetingForHour(LocalTime.now().hour)
    } catch (_: Exception) {
        "Good evening"
    }
}

/** Distinct artists in order of appearance, with their track counts. */
fun topArtists(tracks: List<com.howdy.echowave.domain.model.Track>): List<Pair<String, Int>> {
    val counts = LinkedHashMap<String, Int>()
    for (t in tracks) counts[t.artist] = (counts[t.artist] ?: 0) + 1
    return counts.toList().sortedByDescending { it.second }
}

/**
 * Deterministic decorative waveform heights in (0.15, 1]. Same seed always
 * yields the same bars (static pattern; real audio analysis is post-MVP).
 */
fun waveformBars(seed: String, n: Int = 48): List<Float> {
    var h = seed.hashCode()
    if (h == 0) h = 1
    return List(n) {
        h = h * 1103515245 + 12345
        val v = ((h ushr 16) and 0x7FFF) / 32767f
        0.15f + 0.85f * v
    }
}
