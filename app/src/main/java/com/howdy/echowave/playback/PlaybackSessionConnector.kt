package com.howdy.echowave.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

/**
 * Binds UI process to [PlaybackService]. Owned by MainActivity,
 * attached to [Media3PlaybackController] on connect.
 */
class PlaybackSessionConnector(
    private val appContext: Context,
    private val controller: Media3PlaybackController,
) {
    private var future: ListenableFuture<MediaController>? = null

    fun connect() {
        if (future != null) return
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        future = MediaController.Builder(appContext, token).buildAsync().also { f ->
            f.addListener(
                {
                    try {
                        controller.attach(f.get())
                    } catch (_: Exception) {
                        // Service unavailable — controller stays in state-only mode.
                    }
                },
                MoreExecutors.directExecutor(),
            )
        }
    }

    fun release() {
        controller.detach()
        future?.let(MediaController::releaseFuture)
        future = null
    }
}
