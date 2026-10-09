package com.howdy.echowave.playback

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.common.util.UnstableApi
import com.howdy.echowave.EchoWaveApp
import com.howdy.echowave.data.playback.ChunkedDataSource
import com.howdy.echowave.data.playback.DressedDataSource
import com.howdy.echowave.data.playback.MediaCacheManager

/**
 * Background playback + notification + lock-screen come from this service.
 * Media bytes go through [DressedDataSource]: per-URL client dressing
 * (donor PlayerClient rule) + safe fetch diagnostics.
 */
@UnstableApi
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val container = (applicationContext as EchoWaveApp).container
        val dressed = DressedDataSource.Factory()
        val chunked = ChunkedDataSource.Factory(dressed, onRefused = { url ->
            container.streamRegistry.onRefused(url)
        })
        val audio = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val cached = MediaCacheManager.createCacheDataSourceFactory(this, chunked)
        // A seek invalidates the forward buffer. Keep the rebuffer threshold
        // short so a sparse range read can resume as soon as playable audio is
        // available instead of waiting for the much larger default threshold.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 60_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 1_500,
            )
            .build()
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cached))
            .setLoadControl(loadControl)
            .setAudioAttributes(audio, true)
            .build()
        val routed = QueueRoutingPlayer(player)
        session = MediaSession.Builder(this, routed).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onDestroy() {
        session?.run { player.release(); release() }
        session = null
        super.onDestroy()
    }
}
