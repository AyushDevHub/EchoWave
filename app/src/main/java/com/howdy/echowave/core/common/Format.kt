package com.howdy.echowave.core.common

/** 200_000 -> "3:20", null/<=0 -> "–". Pure + unit-tested. */
fun formatDuration(durationMs: Long?): String {
    if (durationMs == null || durationMs <= 0) return "–"
    val totalSec = durationMs / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
