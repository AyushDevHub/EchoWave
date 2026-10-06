package com.howdy.echowave.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.howdy.echowave.core.common.AppResult
import com.howdy.echowave.domain.model.PlaybackState
import com.howdy.echowave.domain.model.RepeatMode
import com.howdy.echowave.domain.model.Track
import com.howdy.echowave.domain.repository.LibraryRepository
import com.howdy.echowave.domain.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Media3-backed controller. Talks to [PlaybackService] via MediaController,
 * resolves streams via [MusicRepository], records history via [LibraryRepository].
 * Works state-only before session connect (pending play replays on attach).
 */
class Media3PlaybackController(
    private val music: MusicRepository,
    private val library: LibraryRepository,
) : PlaybackController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state

    private var controller: MediaController? = null
    private var pending: Pair<List<Track>, Int>? = null
    private var positionJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            android.util.Log.d("EchoWavePlay", "onIsPlayingChanged=$isPlaying")
            _state.value = _state.value.copy(isPlaying = isPlaying, isBuffering = false)
            if (isPlaying) startPositionPolling() else stopPositionPolling()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            android.util.Log.d(
                "EchoWavePlay",
                "playbackState=${when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> playbackState.toString()
                }}",
            )
            when (playbackState) {
                Player.STATE_BUFFERING -> _state.value = _state.value.copy(isBuffering = true)
                Player.STATE_READY -> _state.value = _state.value.copy(isBuffering = false)
                Player.STATE_ENDED -> onTrackEnded()
                else -> Unit
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val cause = generateSequence<Throwable>(error) { it.cause }.toList()
                .joinToString(" <- ") { "${it.javaClass.simpleName}: ${it.message}" }
            android.util.Log.e("EchoWavePlay", "onPlayerError code=${error.errorCode} name=${error.errorCodeName} msg=${error.message}")
            android.util.Log.e("EchoWavePlay", "causes: $cause")
            _state.value = _state.value.copy(
                isPlaying = false,
                isBuffering = false,
                error = "Player error (${error.errorCodeName}): ${error.message}",
            )
        }
    }

    fun attach(c: MediaController) {
        detach()
        android.util.Log.d("EchoWaveSession", "controller attached")
        controller = c.apply {
            addListener(listener)
            shuffleModeEnabled = _state.value.shuffleEnabled
            repeatMode = _state.value.repeatMode.toExoRepeat()
        }
        PlaybackRouter.install(
            next = ::next,
            previous = ::previous,
            hasNext = { val s = _state.value; s.queueIndex + 1 in s.queue.indices },
            hasPrevious = { val s = _state.value; s.queueIndex - 1 in s.queue.indices },
        )
        // Rebind sync: notification/lock-screen may have moved playback
        // while the UI was away (track, position, playing state).
        val sessionItem = c.currentMediaItem
        val restored = _state.value.currentTrack ?: trackFromSession(
            sessionItem?.mediaId,
            c.mediaMetadata.title?.toString(),
            c.mediaMetadata.artist?.toString(),
            c.mediaMetadata.albumTitle?.toString(),
        )
        if (restored != null && _state.value.currentTrack == null) {
            _state.value = _state.value.copy(
                currentTrack = restored,
                queue = listOf(restored),
                queueIndex = 0,
            )
        }
        _state.value = _state.value.copy(
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0),
            durationMs = c.duration.takeIf { it > 0 } ?: _state.value.durationMs,
        )
        pending?.let { (tracks, index) ->
            pending = null
            scope.launch { play(tracks, index) }
        }
        if (c.isPlaying) startPositionPolling()
    }

    fun detach() {
        android.util.Log.d("EchoWaveSession", "controller detached")
        // Do NOT clear PlaybackRouter here: detach fires on backgrounding
        // (onStop), exactly when notification/lock-screen need the routes.
        // Handlers stay valid while the process lives (app-scoped queue).
        controller?.removeListener(listener)
        controller = null
        stopPositionPolling()
    }

    override suspend fun play(tracks: List<Track>, index: Int) {
        val track = tracks.getOrNull(index) ?: return
        android.util.Log.d("EchoWavePlay", "play() trackId=${track.id} index=$index sessionBound=${controller != null}")
        val session = controller
        if (session == null) {
            // Session not bound yet — hold state, replay on attach.
            pending = tracks to index
            _state.value = _state.value.copy(
                currentTrack = track, queue = tracks, queueIndex = index,
                isBuffering = true, error = null,
            )
            return
        }
        _state.value = _state.value.copy(
            currentTrack = track, queue = tracks, queueIndex = index,
            isBuffering = true, error = null,
        )
        when (val r = music.resolveStream(track.id)) {
            is AppResult.Ok -> {
                android.util.Log.d("EchoWavePlay", "resolve OK trackId=${track.id} mime=${r.value.mimeType}")
                session.setMediaItem(track.toMediaItem(r.value.url))
                session.prepare()
                session.play()
                _state.value = _state.value.copy(currentTrack = track, isPlaying = true, isBuffering = false)
                scope.launch(Dispatchers.IO) { library.recordPlayed(track) }
            }
            is AppResult.Err -> {
                android.util.Log.e("EchoWavePlay", "resolve FAIL trackId=${track.id} reason=${r.message}")
                _state.value = _state.value.copy(isBuffering = false, error = r.message)
            }
        }
    }

    override fun toggle() {
        val c = controller ?: run {
            _state.value = _state.value.copy(isPlaying = !_state.value.isPlaying)
            return
        }
        if (c.isPlaying) c.pause() else c.play()
    }

    override fun next() = jump(1)
    override fun previous() = jump(-1)

    private fun jump(delta: Int) {
        val s = _state.value
        val next = s.queueIndex + delta
        if (next in s.queue.indices) scope.launch { play(s.queue, next) }
    }

    private fun onTrackEnded() {
        val s = _state.value
        when (s.repeatMode) {
            RepeatMode.ONE -> scope.launch { play(s.queue, s.queueIndex) }
            else -> {
                val next = s.queueIndex + 1
                if (next in s.queue.indices) scope.launch { play(s.queue, next) }
                else if (s.repeatMode == RepeatMode.ALL && s.queue.isNotEmpty()) {
                    scope.launch { play(s.queue, 0) }
                } else {
                    _state.value = s.copy(isPlaying = false)
                }
            }
        }
    }

    override fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        _state.value = _state.value.copy(positionMs = ms)
    }

    override fun setShuffle(enabled: Boolean) {
        _state.value = _state.value.copy(shuffleEnabled = enabled)
        controller?.shuffleModeEnabled = enabled
    }

    override fun setRepeat(mode: RepeatMode) {
        _state.value = _state.value.copy(repeatMode = mode)
        controller?.repeatMode = mode.toExoRepeat()
    }

    override fun removeAt(index: Int) {
        val s = _state.value
        if (index !in s.queue.indices) return
        val q = s.queue.toMutableList().also { it.removeAt(index) }
        _state.value = s.copy(queue = q, queueIndex = if (index < s.queueIndex) s.queueIndex - 1 else s.queueIndex)
    }

    private fun startPositionPolling() {
        if (positionJob != null) return
        positionJob = scope.launch {
            while (true) {
                val c = controller ?: break
                _state.value = _state.value.copy(
                    positionMs = c.currentPosition.coerceAtLeast(0),
                    durationMs = c.duration.takeIf { it > 0 } ?: _state.value.durationMs,
                )
                delay(500)
            }
        }
    }

    private fun stopPositionPolling() {
        positionJob?.cancel()
        positionJob = null
    }
}

private fun Track.toMediaItem(url: String): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(artworkUrl?.let { android.net.Uri.parse(it) })
        .build()
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(url)
        .setMediaMetadata(metadata)
        .build()
}

private fun RepeatMode.toExoRepeat(): Int = when (this) {
    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
}

/**
 * Rebuilds the current track from what the session player still holds
 * (set with mediaId + metadata in play()). Pure for unit tests.
 */
fun trackFromSession(mediaId: String?, title: String?, artist: String?, album: String?): Track? {
    if (mediaId.isNullOrEmpty()) return null
    return Track(
        id = mediaId,
        title = title?.takeIf { it.isNotBlank() } ?: "Unknown title",
        artist = artist?.takeIf { it.isNotBlank() } ?: "Unknown artist",
        album = album,
    )
}
