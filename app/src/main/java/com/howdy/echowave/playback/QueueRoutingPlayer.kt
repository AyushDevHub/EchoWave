package com.howdy.echowave.playback

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player

/**
 * Routes session prev/next (notification, lock screen, headset) to the
 * queue owner instead of the single-item player timeline. Advertises the
 * commands exactly when the queue has somewhere to go, so notification
 * and lock screen show the right actions.
 */
class QueueRoutingPlayer(wrapped: Player) : ForwardingPlayer(wrapped) {
    override fun seekToNext() {
        val custom = PlaybackRouter.onNext
        if (custom != null) custom() else super.seekToNext()
    }

    override fun seekToPrevious() {
        val custom = PlaybackRouter.onPrevious
        if (custom != null) custom() else super.seekToPrevious()
    }

    override fun hasNext(): Boolean =
        if (PlaybackRouter.onNext != null) PlaybackRouter.canNext() else super.hasNext()

    override fun getAvailableCommands(): Player.Commands {
        val base = super.getAvailableCommands()
        // Advertise whenever a queue owner is installed, not per bounds:
        // the answer must be stable at notification-build time, because no
        // event fires when the queue merely fills. Bounds are guarded in
        // the controller (next/prev at the ends are no-ops).
        if (PlaybackRouter.onNext == null && PlaybackRouter.onPrevious == null) return base
        return base.buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .build()
    }
}
