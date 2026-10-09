package com.howdy.echowave.playback

import com.howdy.echowave.domain.model.PlaybackState
import com.howdy.echowave.domain.model.RepeatMode
import com.howdy.echowave.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

/** UI observes this. Screens never touch ExoPlayer. */
interface PlaybackController {
    val state: StateFlow<PlaybackState>
    suspend fun play(tracks: List<Track>, index: Int = 0)
    suspend fun playRadio(seed: Track)
    fun appendToQueue(tracks: List<Track>)
    fun toggle()
    fun next()
    fun previous()
    fun seekTo(ms: Long)
    fun setShuffle(enabled: Boolean)
    fun setRepeat(mode: RepeatMode)
    fun setVolume(volume: Float)
    fun removeAt(index: Int)
}
