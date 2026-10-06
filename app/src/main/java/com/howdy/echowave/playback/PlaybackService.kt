package com.howdy.echowave.playback

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.howdy.echowave.EchoWaveApp
import com.howdy.echowave.data.playback.ChunkedDataSource
import com.howdy.echowave.data.playback.DressedDataSource

/**
 * Background playback + notification + lock-screen come from this service.
 * Media bytes go through [DressedDataSource]: per-URL client dressing
 * (donor PlayerClient rule) + safe fetch diagnostics.
 */
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
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(chunked))
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
