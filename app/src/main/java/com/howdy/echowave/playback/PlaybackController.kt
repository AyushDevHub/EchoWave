package com.howdy.echowave.playback

import com.howdy.echowave.domain.model.PlaybackState
import com.howdy.echowave.domain.model.RepeatMode
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.recommendation.RecommendedTrack
import kotlinx.coroutines.flow.StateFlow

/** UI observes this. Screens never touch ExoPlayer. */
interface PlaybackController {
    val state: StateFlow<PlaybackState>
    suspend fun play(tracks: List<Track>, index: Int = 0)
    suspend fun playRadio(seed: Track)
    /** Rebuilds only the upcoming portion, keeping the current song in place. */
    suspend fun refreshQueue(): AppResult<Unit> = AppResult.Err("Queue refresh is unavailable")
    /** Returns personalized candidates without changing the active queue. */
    suspend fun recommendations(seed: Track, limit: Int = 20): AppResult<List<RecommendedTrack>> =
        AppResult.Err("Recommendations are unavailable")
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
