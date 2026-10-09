package com.howdy.echowave.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.ListenableFuture

/**
 * Binds UI process to [PlaybackService]. Owned by MainActivity,
 * attached to [Media3PlaybackController] on connect.
 */
@UnstableApi
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
                        // Service unavailable — allow retry and stay state-only.
                        future?.let { runCatching { MediaController.releaseFuture(it) } }
                        future = null
                    }
                },
                { runnable -> android.os.Handler(android.os.Looper.getMainLooper()).post(runnable) },
            )
        }
    }

    fun release() {
        controller.detach()
        future?.let(MediaController::releaseFuture)
        future = null
    }
}
