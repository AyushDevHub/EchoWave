package com.howdy.echowave.playback

/**
 * Process-wide route from MediaSession player commands to the queue owner.
 * The session player holds one MediaItem at a time, so its native
 * seekToNext/Previous would just restart or no-op. The service's
 * ForwardingPlayer diverts them here; the UI controller (queue owner)
 * installs handlers on attach and clears them on detach. Null-safe:
 * with no owner installed, the service falls back to player default.
 */
object PlaybackRouter {
    @Volatile var onNext: (() -> Unit)? = null
    @Volatile var onPrevious: (() -> Unit)? = null
    @Volatile var canNext: () -> Boolean = { false }
    @Volatile var canPrevious: () -> Boolean = { false }

    @Synchronized
    fun install(
        next: () -> Unit,
        previous: () -> Unit,
        hasNext: () -> Boolean,
        hasPrevious: () -> Boolean,
    ) {
        onNext = next
        onPrevious = previous
        canNext = hasNext
        canPrevious = hasPrevious
    }

    @Synchronized
    fun clear() {
        onNext = null
        onPrevious = null
        canNext = { false }
        canPrevious = { false }
    }
}
