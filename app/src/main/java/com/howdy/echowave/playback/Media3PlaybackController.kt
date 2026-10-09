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
import com.howdy.echowave.domain.model.TrackIdentity
import com.howdy.echowave.domain.recommendation.CandidateContext
import com.howdy.echowave.domain.recommendation.CandidatePipeline
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
    private val dna: com.howdy.echowave.domain.repository.ListeningEventRecorder? = null,
    private val dnaClock: () -> Long = System::currentTimeMillis,
    private val candidatePipeline: CandidatePipeline? = null,
    val sessionTaste: com.howdy.echowave.domain.recommendation.SessionTasteTracker = com.howdy.echowave.domain.recommendation.SessionTasteTracker(),
    private val dnaProfileProvider: suspend () -> com.howdy.echowave.domain.dna.MusicDnaProfile? = { null },
    private val favoriteTrackIdsProvider: suspend () -> Set<String> = { emptySet() },
    private val recentPlayedProvider: suspend () -> List<Track> = { emptyList() },
) : PlaybackController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state

    private var controller: MediaController? = null
    private var pending: Pair<List<Track>, Int>? = null
    private var positionJob: Job? = null
    private var loadingNextTracks = false
    private var skipDebounceJob: Job? = null
    private var lastPrefetchedSeedId: String? = null
    private var navigatingPrevious = false
    private var advanceWhenRecommendationsReady = false
    private var activePrefetchSeedId: String? = null
    private var rerunPrefetchForNewSeed = false

    /** Active DNA listen session: track + active listening time tracking. */
    private var dnaTrack: Track? = null
    private var dnaAccumulatedListenMs: Long = 0L
    private var dnaLastPlayingStartedAt: Long? = null

    private fun dnaCurrentListenMs(): Long {
        val active = dnaLastPlayingStartedAt?.let { (dnaClock() - it).coerceAtLeast(0L) } ?: 0L
        return dnaAccumulatedListenMs + active
    }

    private fun resetDnaListenClock(nowPlaying: Boolean) {
        dnaAccumulatedListenMs = 0L
        dnaLastPlayingStartedAt = if (nowPlaying) dnaClock() else null
    }

    companion object {
        private const val RESTART_THRESHOLD_MS = 3000L
        internal const val SKIP_EARLY_THRESHOLD_MS = 30_000L
        internal const val REPEAT_THRESHOLD_MS = 60_000L
    }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            logd("onIsPlayingChanged=$isPlaying")
            _state.value = _state.value.copy(isPlaying = isPlaying, isBuffering = false)
            if (isPlaying) {
                if (dnaLastPlayingStartedAt == null && dnaTrack != null) {
                    dnaLastPlayingStartedAt = dnaClock()
                }
                startPositionPolling()
            } else {
                dnaLastPlayingStartedAt?.let {
                    dnaAccumulatedListenMs += (dnaClock() - it).coerceAtLeast(0L)
                }
                dnaLastPlayingStartedAt = null
                stopPositionPolling()
            }
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
            loge("onPlayerError code=${error.errorCode} name=${error.errorCodeName} msg=${error.message}")
            loge("causes: $cause")
            _state.value = _state.value.copy(
                isPlaying = false,
                isBuffering = false,
                error = "Player error (${error.errorCodeName}): ${error.message}",
            )
        }
    }

    fun attach(c: MediaController) {
        detach()
        logsess("controller attached")
        controller = c.apply {
            addListener(listener)
            shuffleModeEnabled = _state.value.shuffleEnabled
            repeatMode = _state.value.repeatMode.toExoRepeat()
            runCatching { volume = _state.value.volume }
        }
        PlaybackRouter.install(
            next = ::next,
            previous = ::previous,
            hasNext = {
                val s = _state.value
                s.queueIndex + 1 in s.queue.indices ||
                    (candidatePipeline != null && s.repeatMode == RepeatMode.OFF)
            },
            hasPrevious = { val s = _state.value; s.queueIndex - 1 in s.queue.indices },
        )
        // Rebind sync: notification/lock-screen may have moved playback
        // while the UI was away (track, position, playing state).
        // Preserve the real queue: only fabricate when we have nothing.
        val sessionItem = c.currentMediaItem
        val restored = _state.value.currentTrack ?: trackFromSession(
            sessionItem?.mediaId,
            c.mediaMetadata.title?.toString(),
            c.mediaMetadata.artist?.toString(),
            c.mediaMetadata.albumTitle?.toString(),
        )
        if (restored != null && _state.value.currentTrack == null) {
            val existingQueue = _state.value.queue
            if (existingQueue.isEmpty()) {
                _state.value = _state.value.copy(
                    currentTrack = restored,
                    queue = listOf(restored),
                    queueIndex = 0,
                )
            } else {
                _state.value = _state.value.copy(currentTrack = restored)
            }
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
        logsess("controller detached")
        // Detach from session updates but keep PlaybackRouter routes:
        // detach fires on backgrounding (onStop), exactly when
        // notification/lock-screen need the routes. Routes are cleared
        // explicitly via releaseRoutes() on process teardown.
        controller?.removeListener(listener)
        controller = null
        stopPositionPolling()
    }

    /** Clear process-wide routes. Call only on app teardown, not backgrounding. */
    fun releaseRoutes() {
        PlaybackRouter.clear()
    }

    override suspend fun play(tracks: List<Track>, index: Int) {
        // Direct tap: surface the error, do not skip.
        playAt(tracks, index)
    }

    override suspend fun playRadio(seed: Track) {
        if (playAt(listOf(seed), 0)) {
            lastPrefetchedSeedId = seed.id
            if (candidatePipeline != null) scope.launch { prefetchNextCandidates() }
        }
    }

    override fun appendToQueue(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val state = _state.value
        val seen = state.queue.mapTo(mutableSetOf()) { TrackIdentity.from(it) }
        val unique = tracks.filter { seen.add(TrackIdentity.from(it)) }
        if (unique.isNotEmpty()) {
            _state.value = state.copy(queue = state.queue + unique, nextTracksMessage = null)
        }
    }

    /**
     * Resolve + load one queue slot. Returns true when the track is
     * playable (or will be on attach); false leaves the reason in state.
     */
    private suspend fun playAt(tracks: List<Track>, index: Int): Boolean {
        val track = tracks.getOrNull(index) ?: return false
        logd("play() trackId=${track.id} index=$index sessionBound=${controller != null}")
        _state.value = _state.value.copy(
            currentTrack = track, queue = tracks, queueIndex = index,
            positionMs = 0L,
            durationMs = track.durationMs ?: 0L,
            isBuffering = true, error = null, isLoadingNext = false, nextTracksMessage = null,
        )
        when (val r = music.resolveStream(track.id)) {
            is AppResult.Ok -> {
                logd("resolve OK trackId=${track.id} mime=${r.value.mimeType}")
                val session = controller
                if (session == null) {
                    // Session not bound yet — hold, replay on attach.
                    pending = tracks to index
                    return true
                }
                session.setMediaItem(track.toMediaItem(r.value.url))
                session.prepare()
                session.play()
                _state.value = _state.value.copy(currentTrack = track, isPlaying = true, isBuffering = false)
                scope.launch(Dispatchers.IO) { library.recordPlayed(track) }
                beginDnaSession(track)
                return true
            }
            is AppResult.Err -> {
                loge("resolve FAIL trackId=${track.id} reason=${r.message}")
                _state.value = _state.value.copy(isBuffering = false, error = r.message)
                return false
            }
        }
    }

    /**
     * Queue advance with bounded skip: unplayable tracks are stepped over
     * (at most a full queue) instead of stranding on an invisible error.
     */
    internal suspend fun advancePlay(delta: Int) {
        val tracks = _state.value.queue
        var i = _state.value.queueIndex + delta
        var attempts = 0
        while (i in tracks.indices && attempts < tracks.size) {
            logd("advance try index=$i")
            if (playAt(tracks, i)) return
            i += delta
            attempts++
        }
    }

    override fun toggle() {
        val c = controller
        if (c == null) {
            // No session yet: do not fake playing state; surface pending only.
            return
        }
        if (c.isPlaying) c.pause() else c.play()
    }

    override fun next() {
        logd("next() queueIndex=${_state.value.queueIndex} size=${_state.value.queue.size}")
        scope.launch {
            val state = _state.value
            if (state.queueIndex + 1 in state.queue.indices) {
                advancePlay(1)
                val remaining = state.queue.size - (state.queueIndex + 1)
                if (remaining < 2 && state.repeatMode == RepeatMode.OFF) {
                    scheduleDebouncedPrefetch()
                }
            } else if (state.repeatMode == RepeatMode.OFF) {
                requestNextAtQueueEnd()
            }
        }
    }

    override fun previous() {
        val s = _state.value
        val pos = controller?.currentPosition ?: s.positionMs
        logd("previous() queueIndex=${s.queueIndex} size=${s.queue.size} pos=$pos")
        // Standard player semantics: fresh/rewind when early in (or at the
        // start of) the queue head, step back only when further in.
        if (pos > RESTART_THRESHOLD_MS || s.queueIndex == 0) {
            seekTo(0)
            return
        }
        navigatingPrevious = true
        scope.launch {
            try {
                advancePlay(-1)
            } finally {
                navigatingPrevious = false
            }
        }
    }

    private fun onTrackEnded() {
        val s = _state.value
        endDnaSession(completed = true)
        when (s.repeatMode) {
            RepeatMode.ONE -> scope.launch { playAt(s.queue, s.queueIndex) }
            else -> {
                val next = s.queueIndex + 1
                if (next in s.queue.indices) scope.launch { advancePlay(1) }
                else if (s.repeatMode == RepeatMode.ALL && s.queue.isNotEmpty()) {
                    scope.launch { advancePlayFromStart() }
                } else {
                    _state.value = s.copy(isPlaying = false)
                    if (s.repeatMode == RepeatMode.OFF) scope.launch { requestNextAtQueueEnd() }
                }
            }
        }
    }

    internal suspend fun requestNextAtQueueEnd() {
        val pipeline = candidatePipeline ?: return
        if (loadingNextTracks) {
            advanceWhenRecommendationsReady = true
            if (activePrefetchSeedId != _state.value.currentTrack?.id) rerunPrefetchForNewSeed = true
            return
        }
        val initial = _state.value
        val seed = initial.currentTrack ?: return
        if (initial.repeatMode != RepeatMode.OFF) return
        loadingNextTracks = true
        // Keep playing state; only show loading indicator for next songs.
        _state.value = initial.copy(isLoadingNext = true, nextTracksMessage = null)
        try {
            val context = buildCandidateContext(seed, initial.queue)
            when (val result = pipeline.recommend(context)) {
                is AppResult.Err -> if (_state.value.currentTrack?.id == seed.id) {
                    _state.value = _state.value.copy(
                        isLoadingNext = false,
                        nextTracksMessage = "Couldn't find next songs: ${result.message}",
                    )
                }
                is AppResult.Ok -> {
                    if (_state.value.currentTrack?.id != seed.id) return
                    val additions = result.value.map { it.track }
                    if (additions.isEmpty()) {
                        _state.value = _state.value.copy(
                            isLoadingNext = false,
                            nextTracksMessage = "No more songs found.",
                        )
                    } else {
                        appendToQueue(additions)
                        _state.value = _state.value.copy(isLoadingNext = false)
                        advancePlay(1)
                    }
                }
            }
        } finally {
            loadingNextTracks = false
            // Ensure flag cleared exactly once (early-returns above already set it).
            if (_state.value.isLoadingNext) {
                _state.value = _state.value.copy(isLoadingNext = false)
            }
        }
    }

    private suspend fun advancePlayFromStart() {
        val tracks = _state.value.queue
        for (i in tracks.indices) {
            if (playAt(tracks, i)) return
        }
    }

    override fun seekTo(ms: Long) {
        val clamped = ms.coerceAtLeast(0L)
        val dur = _state.value.durationMs
        val finalMs = if (dur > 0) clamped.coerceAtMost(dur) else clamped
        controller?.seekTo(finalMs)
        _state.value = _state.value.copy(positionMs = finalMs)
    }

    override fun setShuffle(enabled: Boolean) {
        _state.value = _state.value.copy(shuffleEnabled = enabled)
        controller?.shuffleModeEnabled = enabled
    }

    override fun setRepeat(mode: RepeatMode) {
        _state.value = _state.value.copy(repeatMode = mode)
        controller?.repeatMode = mode.toExoRepeat()
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = clamped)
        runCatching { controller?.volume = clamped }
    }

    override fun removeAt(index: Int) {
        val s = _state.value
        if (index !in s.queue.indices) return
        val q = s.queue.toMutableList().also { it.removeAt(index) }
        val newIndex = when {
            q.isEmpty() -> 0
            index < s.queueIndex -> (s.queueIndex - 1).coerceIn(0, q.size - 1)
            index == s.queueIndex -> s.queueIndex.coerceIn(0, q.size - 1)
            else -> s.queueIndex.coerceIn(0, q.size - 1)
        }
        val newCurrent = q.getOrNull(newIndex)
        _state.value = s.copy(
            queue = q,
            queueIndex = newIndex,
            currentTrack = newCurrent ?: s.currentTrack.takeIf { q.isNotEmpty() },
        )
        if (q.isEmpty()) {
            _state.value = _state.value.copy(currentTrack = null, queueIndex = 0, isPlaying = false, positionMs = 0L)
        }
    }

    private fun startPositionPolling() {
        if (positionJob != null) return
        positionJob = scope.launch {
            while (true) {
                val c = controller ?: break
                val pos = c.currentPosition.coerceAtLeast(0)
                val dur = c.duration.takeIf { it > 0 } ?: _state.value.durationMs
                _state.value = _state.value.copy(
                    positionMs = pos,
                    durationMs = dur,
                )
                if (dur > 0 && pos.toFloat() / dur.toFloat() >= 0.75f) {
                    checkAndPrefetchNextTracks()
                }
                // 120ms keeps word-synced lyrics tight to the voice without
                // changing playback itself; UI just observes more often.
                delay(120)
            }
        }
    }

    private fun checkAndPrefetchNextTracks() {
        val s = _state.value
        val current = s.currentTrack ?: return
        if (candidatePipeline == null || s.repeatMode != RepeatMode.OFF) return
        if (lastPrefetchedSeedId == current.id) return
        val remaining = s.queue.size - (s.queueIndex + 1)
        if (remaining <= 1) {
            lastPrefetchedSeedId = current.id
            scope.launch { prefetchNextCandidates() }
        }
    }

    private fun scheduleDebouncedPrefetch() {
        skipDebounceJob?.cancel()
        skipDebounceJob = scope.launch {
            delay(400)
            prefetchNextCandidates()
        }
    }

    private suspend fun prefetchNextCandidates() {
        val pipeline = candidatePipeline ?: return
        val current = _state.value.currentTrack ?: return
        if (loadingNextTracks) {
            if (activePrefetchSeedId != current.id) rerunPrefetchForNewSeed = true
            return
        }
        loadingNextTracks = true
        activePrefetchSeedId = current.id
        _state.value = _state.value.copy(isLoadingNext = true, nextTracksMessage = null)
        try {
            val queueAtRequest = _state.value.queue
            val context = buildCandidateContext(current, queueAtRequest)
            when (val result = pipeline.recommend(context)) {
                is AppResult.Ok -> {
                    if (_state.value.currentTrack?.id != current.id) return
                    val additions = result.value.map { it.track }
                    if (additions.isNotEmpty()) {
                        appendToQueue(additions)
                        // Pre-resolve stream for the immediate next song in background so transition is 0ms
                        val nextTrack = additions.firstOrNull()
                        if (nextTrack != null) {
                            runCatching { music.resolveStream(nextTrack.id) }
                        }
                    } else if (advanceWhenRecommendationsReady) {
                        _state.value = _state.value.copy(nextTracksMessage = "No more songs found.")
                    }
                }
                is AppResult.Err -> if (_state.value.currentTrack?.id == current.id && advanceWhenRecommendationsReady) {
                    _state.value = _state.value.copy(nextTracksMessage = "Couldn't find next songs: ${result.message}")
                }
            }
        } finally {
            loadingNextTracks = false
            _state.value = _state.value.copy(isLoadingNext = false)
            if (advanceWhenRecommendationsReady) {
                val s = _state.value
                if (s.queueIndex + 1 in s.queue.indices) {
                    advanceWhenRecommendationsReady = false
                    advancePlay(1)
                } else if (s.currentTrack?.id == activePrefetchSeedId && !rerunPrefetchForNewSeed) {
                    advanceWhenRecommendationsReady = false
                }
            }
            if (rerunPrefetchForNewSeed) {
                rerunPrefetchForNewSeed = false
                scope.launch { prefetchNextCandidates() }
            }
        }
    }

    private suspend fun buildCandidateContext(seed: Track, queue: List<Track>): CandidateContext {
        val recentHistory = recentPlayedProvider()
        val played = buildSet {
            queue.forEach { add(TrackIdentity.from(it)) }
            recentHistory.forEach { add(TrackIdentity.from(it)) }
        }
        val artists = queue.groupingBy { it.artist.trim().lowercase() }.eachCount()
        return CandidateContext(
            seed = seed,
            played = played,
            sessionTaste = sessionTaste,
            dnaProfile = dnaProfileProvider(),
            favoriteIds = favoriteTrackIdsProvider(),
            queuedArtistsCount = artists,
        )
    }

    private fun stopPositionPolling() {
        positionJob?.cancel()
        positionJob = null
    }

    /**
     * Starts a DNA listen session. The previous session (if any) is closed
     * as a skip — callers that know the track completed must call
     * [endDnaSession] with completed=true first (see [onTrackEnded]).
     * Fire-and-forget on IO; never blocks playback or calls AI.
     */
    private fun beginDnaSession(track: Track) {
        val isCurrentlyPlaying = controller?.isPlaying == true || _state.value.isPlaying
        val recorder = dna ?: run {
            dnaTrack = track
            resetDnaListenClock(isCurrentlyPlaying)
            return
        }
        val now = dnaClock()
        val listenMs = dnaCurrentListenMs()
        dnaTrack?.let { prev ->
            if (prev.id != track.id) {
                recordDnaSkip(recorder, prev, listenMs)
            } else {
                // Same track replayed (repeat-one or re-tap): count a repeat
                // when the previous stint was substantial.
                if (listenMs >= REPEAT_THRESHOLD_MS) {
                    val at = now
                    scope.launch(Dispatchers.IO) {
                        recorder.record(
                            com.howdy.echowave.domain.dna.ListeningEvent(
                                trackId = prev.id,
                                title = prev.title,
                                artist = prev.artist,
                                type = com.howdy.echowave.domain.dna.ListeningEventType.REPEAT,
                                playedAt = at,
                                listenMs = listenMs,
                                completionRatio = ratioOf(listenMs, prev.durationMs),
                                hourOfDay = hourOf(at),
                                durationMs = prev.durationMs,
                            ),
                        )
                    }
                }
            }
        }
        dnaTrack = track
        resetDnaListenClock(isCurrentlyPlaying)
        scope.launch(Dispatchers.IO) {
            recorder.record(
                com.howdy.echowave.domain.dna.ListeningEvent(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    type = com.howdy.echowave.domain.dna.ListeningEventType.START,
                    playedAt = now,
                    hourOfDay = hourOf(now),
                    durationMs = track.durationMs,
                ),
            )
        }
    }

    private fun endDnaSession(completed: Boolean) {
        val recorder = dna ?: run {
            dnaTrack = null
            resetDnaListenClock(false)
            return
        }
        val track = dnaTrack ?: return
        val now = dnaClock()
        val listenMs = dnaCurrentListenMs()
        dnaTrack = null
        resetDnaListenClock(false)
        if (completed) {
            sessionTaste.record(track, completed = true, earlySkip = false)
        }
        scope.launch(Dispatchers.IO) {
            if (completed) {
                recorder.record(
                    com.howdy.echowave.domain.dna.ListeningEvent(
                        trackId = track.id,
                        title = track.title,
                        artist = track.artist,
                        type = com.howdy.echowave.domain.dna.ListeningEventType.COMPLETE,
                        playedAt = now,
                        listenMs = listenMs,
                        completionRatio = 1f,
                        hourOfDay = hourOf(now),
                        durationMs = track.durationMs,
                    ),
                )
            } else {
                recordDnaSkip(recorder, track, listenMs)
            }
        }
    }

    private fun recordDnaSkip(
        recorder: com.howdy.echowave.domain.repository.ListeningEventRecorder,
        track: Track,
        listenMs: Long,
    ) {
        val now = dnaClock()
        val type = classifySkip(listenMs, track.durationMs)
        val isEarly = type == com.howdy.echowave.domain.dna.ListeningEventType.SKIP_EARLY
        if (!navigatingPrevious) {
            sessionTaste.record(track, completed = false, earlySkip = isEarly)
        }
        scope.launch(Dispatchers.IO) {
            recorder.record(
                com.howdy.echowave.domain.dna.ListeningEvent(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    type = type,
                    playedAt = now,
                    listenMs = listenMs,
                    completionRatio = ratioOf(listenMs, track.durationMs),
                    hourOfDay = hourOf(now),
                    durationMs = track.durationMs,
                ),
            )
        }
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

internal var ctlLog: (String, String, Throwable?) -> Unit = { tag, msg, err ->
    if (err == null) android.util.Log.d(tag, msg) else android.util.Log.e(tag, msg, err)
}

private fun logd(msg: String) = ctlLog("EchoWavePlay", msg, null)
private fun loge(msg: String, e: Throwable? = null) = ctlLog("EchoWavePlay", msg, e)
private fun logsess(msg: String) = ctlLog("EchoWaveSession", msg, null)

/**
 * Rebuilds the current track from what the session player still holds
 * (set with mediaId + metadata in play()). Pure for unit tests.
 */fun trackFromSession(mediaId: String?, title: String?, artist: String?, album: String?): Track? {
    if (mediaId.isNullOrEmpty()) return null
    return Track(
        id = mediaId,
        title = title?.takeIf { it.isNotBlank() } ?: "Unknown title",
        artist = artist?.takeIf { it.isNotBlank() } ?: "Unknown artist",
        album = album,
    )
}

/**
 * Skip bucket: early when abandoned quickly (<30s and <25% when duration
 * is known), late otherwise. Pure for unit tests.
 */
internal fun classifySkip(
    listenMs: Long,
    durationMs: Long?,
): com.howdy.echowave.domain.dna.ListeningEventType {
    if (durationMs != null && durationMs > 0) {
        val ratio = listenMs.toDouble() / durationMs.toDouble()
        if (listenMs < Media3PlaybackController.SKIP_EARLY_THRESHOLD_MS && ratio < 0.25) {
            return com.howdy.echowave.domain.dna.ListeningEventType.SKIP_EARLY
        }
        return com.howdy.echowave.domain.dna.ListeningEventType.SKIP_LATE
    }
    return if (listenMs < Media3PlaybackController.SKIP_EARLY_THRESHOLD_MS) {
        com.howdy.echowave.domain.dna.ListeningEventType.SKIP_EARLY
    } else {
        com.howdy.echowave.domain.dna.ListeningEventType.SKIP_LATE
    }
}

private fun ratioOf(listenMs: Long, durationMs: Long?): Float? {
    if (durationMs == null || durationMs <= 0) return null
    return (listenMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

private fun hourOf(epochMs: Long, zone: java.util.TimeZone = java.util.TimeZone.getDefault()): Int {
    val cal = java.util.Calendar.getInstance(zone)
    cal.timeInMillis = epochMs
    return cal.get(java.util.Calendar.HOUR_OF_DAY)
}
