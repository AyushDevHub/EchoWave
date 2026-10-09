package com.howdy.echowave.features.dna

/** Local hour (0-23) -> daypart bucket for time-of-day personalization. */
object DayPart {
    const val MORNING = "MORNING"
    const val AFTERNOON = "AFTERNOON"
    const val EVENING = "EVENING"
    const val LATE_NIGHT = "LATE_NIGHT"

    fun of(hour: Int): String = when ((hour % 24 + 24) % 24) {
        in 5..11 -> MORNING
        in 12..16 -> AFTERNOON
        in 17..21 -> EVENING
        else -> LATE_NIGHT
    }
}
