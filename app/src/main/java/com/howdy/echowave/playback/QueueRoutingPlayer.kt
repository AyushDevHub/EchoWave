package com.howdy.echowave.playback

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

/**
 * Routes session prev/next (notification, lock screen, headset) to the
 * queue owner instead of the single-item player timeline. Advertises the
 * commands exactly when the queue has somewhere to go, so notification
 * and lock screen show the right actions.
 */
@UnstableApi
class QueueRoutingPlayer(wrapped: Player) : ForwardingPlayer(wrapped) {
    override fun seekToNext() {
        val custom = PlaybackRouter.onNext
        android.util.Log.d(TAG, "seekToNext ownerPresent=${custom != null}")
        if (custom != null) custom() else super.seekToNext()
    }

    // MediaSession transport controls and system media notifications use the
    // media-item variants. The app's mini-player calls seekToNext(), so route
    // both APIs to the same queue owner.
    override fun seekToNextMediaItem() {
        val custom = PlaybackRouter.onNext
        android.util.Log.d(TAG, "seekToNextMediaItem ownerPresent=${custom != null}")
        if (custom != null) custom() else super.seekToNextMediaItem()
    }

    override fun seekToPrevious() {
        val custom = PlaybackRouter.onPrevious
        android.util.Log.d(TAG, "seekToPrevious ownerPresent=${custom != null}")
        if (custom != null) custom() else super.seekToPrevious()
    }

    override fun seekToPreviousMediaItem() {
        val custom = PlaybackRouter.onPrevious
        android.util.Log.d(TAG, "seekToPreviousMediaItem ownerPresent=${custom != null}")
        if (custom != null) custom() else super.seekToPreviousMediaItem()
    }

    override fun hasNext(): Boolean =
        if (PlaybackRouter.onNext != null) PlaybackRouter.canNext() else super.hasNext()

    override fun hasNextMediaItem(): Boolean =
        if (PlaybackRouter.onNext != null) PlaybackRouter.canNext() else super.hasNextMediaItem()

    override fun hasPreviousMediaItem(): Boolean =
        if (PlaybackRouter.onPrevious != null) PlaybackRouter.canPrevious() else super.hasPreviousMediaItem()

    override fun getAvailableCommands(): Player.Commands {
        val base = super.getAvailableCommands()
        if (PlaybackRouter.onNext == null && PlaybackRouter.onPrevious == null) return base
        // Per-bounds advertising so notification/lock-screen only show
        // actions that will do something.
        val builder = base.buildUpon()
        if (PlaybackRouter.onNext != null && PlaybackRouter.canNext()) {
            builder.add(Player.COMMAND_SEEK_TO_NEXT)
            builder.add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        }
        if (PlaybackRouter.onPrevious != null && PlaybackRouter.canPrevious()) {
            builder.add(Player.COMMAND_SEEK_TO_PREVIOUS)
            builder.add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        }
        return builder.build()
    }

    companion object {
        private const val TAG = "EchoWaveRoute"
    }
}
