package com.kenjigames.ividsmusic.ui.player

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.AudioEngineCommand
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.PlaybackStatus
import com.kenjigames.ividsmusic.domain.model.PlaybackSessionSnapshot
import com.kenjigames.ividsmusic.domain.model.RepeatMode
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.AudioEngine
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.domain.port.OfflineDownloadManager
import com.kenjigames.ividsmusic.domain.port.PlaybackSessionRepository
import com.kenjigames.ividsmusic.domain.port.PreferencesRepository
import com.kenjigames.ividsmusic.domain.port.StreamPrefetcher
import com.kenjigames.ividsmusic.domain.usecase.PlaybackQueueController
import com.kenjigames.ividsmusic.domain.usecase.RecommendationEngine
import com.kenjigames.ividsmusic.domain.usecase.ResolveTrackStreamUseCase
import com.kenjigames.ividsmusic.ui.components.ToastController
import com.kenjigames.ividsmusic.ui.theme.ColorExtractor
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable presentation projection combining active queue metadata, AudioEngine telemetry,
 * liked/offline flags, and the album-derived dynamic accent color.
 */
@Immutable
data class PlayerUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isResolving: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isCurrentLiked: Boolean = false,
    val isCurrentDownloaded: Boolean = false,
    val activeQuality: AudioQuality = AudioQuality.Q196,
    val dynamicAccent: Color = IvidsColorTokens.ChalkInk,
    val errorMessage: String? = null
) {
    /** Returns true when swiping/pressing previous can jump back or restart the active track. */
    val canSkipPrevious: Boolean
        get() = currentTrack != null && (currentIndex > 0 || repeatMode == RepeatMode.ALL || positionMs > 3_000L)

    /** Returns true when skipping forward to a queued track is currently available. */
    val canSkipNext: Boolean
        get() = queue.isNotEmpty() && (currentIndex < queue.lastIndex || repeatMode == RepeatMode.ALL || isShuffleEnabled)
}

/**
 * Application-wide playback coordinator connecting Compose UI controls to [AudioEngine],
 * [PlaybackQueueController], [ResolveTrackStreamUseCase], [RecommendationEngine], and [ColorExtractor].
 */
@Stable
class PlayerViewModel(
    private val audioEngine: AudioEngine,
    private val queueController: PlaybackQueueController,
    private val resolveStream: ResolveTrackStreamUseCase,
    private val libraryRepository: LibraryRepository,
    private val recommendationEngine: RecommendationEngine,
    private val preferencesRepository: PreferencesRepository,
    private val offlineDownloadManager: OfflineDownloadManager,
    private val toastController: ToastController,
    private val sessionRepository: PlaybackSessionRepository? = null,
    private val streamPrefetcher: StreamPrefetcher? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _uiState = MutableStateFlow(PlayerUiState())

    /** Reactive playback UI state consumed by MiniPlayerBar, DesktopPlayerDeck, and NowPlayingSheet. */
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var activePlaybackJob: Job? = null
    private var accentJob: Job? = null
    private var prefetchJob: Job? = null
    private var volumePersistJob: Job? = null
    private var playbackSessionSeq: Long = 0L
    private var handledEndedSessionSeq: Long = -1L
    private var preMuteVolume: Float = 1.0f
    private var initialVolumeRestored: Boolean = false
    private var pendingRestorePositionMs: Long = 0L
    private var pendingSeekOnReadyMs: Long = 0L
    private var prefetchedForTrackId: String? = null
    private var lastSessionSaveMs: Long = 0L

    init {
        observeEngineAndQueue()
        observeExternalCommands()
        observeLibraryAndPreferences()
        restoreLastSession()
    }

    /**
     * Starts playing [track] within [contextQueue], recording any early skip on the outgoing track
     * and extracting dynamic accent colors from the new track's cover artwork.
     */
    fun playTrack(track: Track, contextQueue: List<Track> = listOf(track)) {
        val safeQueue = if (contextQueue.isEmpty()) listOf(track) else contextQueue
        val startIndex = safeQueue.indexOfFirst { it.id == track.id }.let { if (it >= 0) it else 0 }
        recordOutgoingEarlySkip()
        val chosen = queueController.setQueue(safeQueue, startIndex) ?: track
        launchTrackPlayback(chosen)
    }

    /**
     * Replaces the active queue with [tracks] starting at [startIndex], optionally enabling shuffle.
     */
    fun playQueue(tracks: List<Track>, startIndex: Int = 0, shuffled: Boolean = false) {
        if (tracks.isEmpty()) return
        recordOutgoingEarlySkip()
        val queueState = queueController.state.value
        if (shuffled != queueState.isShuffleEnabled) {
            queueController.toggleShuffle()
        }
        val chosen = queueController.setQueue(tracks, startIndex) ?: return
        launchTrackPlayback(chosen)
    }

    /**
     * Inserts [track] immediately after the currently playing item, or begins playback if idle.
     */
    fun enqueueNext(track: Track) {
        val wasEmpty = queueController.state.value.currentTrack == null
        queueController.playNext(track)
        if (wasEmpty) {
            launchTrackPlayback(track)
        } else {
            toastController.show("Playing next: ${track.title}")
        }
    }

    /**
     * Appends [track] to the tail of the active queue, or starts playback if the queue is empty.
     */
    fun enqueueLast(track: Track) {
        val wasEmpty = queueController.state.value.currentTrack == null
        queueController.addToQueue(track)
        if (wasEmpty) {
            launchTrackPlayback(track)
        } else {
            toastController.show("Added to queue: ${track.title}")
        }
    }

    /**
     * Toggles between playing and paused states, or starts/resumes the restored track if idle or ended.
     */
    fun togglePlayPause() {
        val state = _uiState.value
        val track = state.currentTrack ?: return
        scope.launch {
            val status = audioEngine.snapshot.value.status
            when {
                state.isPlaying -> {
                    audioEngine.pause()
                    saveSessionNow()
                }
                status == PlaybackStatus.ENDED || status == PlaybackStatus.IDLE || status == PlaybackStatus.ERROR -> {
                    val seekTarget = if (status == PlaybackStatus.IDLE) pendingRestorePositionMs else 0L
                    pendingRestorePositionMs = 0L
                    launchTrackPlayback(track, initialPositionMs = seekTarget)
                }
                else -> audioEngine.resume()
            }
        }
    }

    /**
     * Clears stream cache for the active track and retries playback from the last visible position.
     */
    fun retryCurrent() {
        val track = _uiState.value.currentTrack ?: return
        val resumePos = _uiState.value.positionMs.coerceAtLeast(pendingRestorePositionMs)
        pendingRestorePositionMs = 0L
        resolveStream.invalidate(track.id)
        launchTrackPlayback(track, initialPositionMs = resumePos)
    }

    /**
     * Skips to the next track in [PlaybackQueueController], recording early skip penalties when `< 20s`.
     */
    fun next() {
        recordOutgoingEarlySkip()
        val nextTrack = queueController.next(respectRepeatOne = false)
        if (nextTrack != null) {
            launchTrackPlayback(nextTrack)
        } else {
            scope.launch { audioEngine.stop() }
        }
    }

    /**
     * Rewinds to the start of the active track if past 3.5 seconds, otherwise moves to the previous item.
     */
    fun previous() {
        if (_uiState.value.positionMs > 3_500L) {
            seekTo(0L)
            return
        }
        recordOutgoingEarlySkip()
        val prevTrack = queueController.previous() ?: return
        launchTrackPlayback(prevTrack)
    }

    /**
     * Seeks playback to [positionMs] inside [AudioEngine] safely clamped within track duration.
     */
    fun seekTo(positionMs: Long) {
        scope.launch {
            audioEngine.seekTo(positionMs.coerceAtLeast(0L))
        }
    }

    /**
     * Shifts current playback position by [deltaMs] milliseconds for keyboard shortcut seeking.
     */
    fun seekRelative(deltaMs: Long) {
        val state = _uiState.value
        val maxDur = state.durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
        val target = (state.positionMs + deltaMs).coerceIn(0L, maxDur)
        seekTo(target)
    }

    /**
     * Updates normalized audio output volume (`0.0f..1.0f`), clears mute when positive, and debounces DataStore persistence.
     */
    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        if (clamped > 0f) preMuteVolume = clamped
        val muted = clamped <= 0.001f
        _uiState.update { it.copy(volume = clamped, isMuted = muted) }
        scope.launch {
            audioEngine.setVolume(clamped)
        }
        persistVolumeDebounced(volume = if (muted) preMuteVolume else clamped, isMuted = muted)
    }

    /**
     * Mutes or restores audio output volume cleanly while preserving pre-mute gain across launches.
     */
    fun toggleMute() {
        val currentlyMuted = _uiState.value.isMuted || _uiState.value.volume <= 0.001f
        if (currentlyMuted) {
            val restored = preMuteVolume.coerceAtLeast(0.5f)
            _uiState.update { it.copy(volume = restored, isMuted = false) }
            scope.launch { audioEngine.setVolume(restored) }
            persistVolumeDebounced(volume = restored, isMuted = false)
        } else {
            preMuteVolume = _uiState.value.volume.coerceAtLeast(0.5f)
            _uiState.update { it.copy(volume = 0f, isMuted = true) }
            scope.launch { audioEngine.setVolume(0f) }
            persistVolumeDebounced(volume = preMuteVolume, isMuted = true)
        }
    }

    /**
     * Toggles queue shuffle while keeping the currently playing track pinned at the head.
     */
    fun toggleShuffle() {
        queueController.toggleShuffle()
        scope.launch { saveSessionNow() }
    }

    /**
     * Cycles repeat progression through `OFF -> ALL -> ONE -> OFF`.
     */
    fun cycleRepeatMode() {
        queueController.cycleRepeatMode()
        scope.launch { saveSessionNow() }
    }

    /**
     * Jumps directly to queue entry [index] and begins playback.
     */
    fun jumpToQueueIndex(index: Int) {
        recordOutgoingEarlySkip()
        val target = queueController.jumpTo(index) ?: return
        launchTrackPlayback(target)
    }

    /**
     * Removes queue item at [index]; stops playback if the queue becomes empty.
     */
    fun removeFromQueue(index: Int) {
        val wasCurrent = index == queueController.state.value.currentIndex
        queueController.removeAt(index)
        val nextCurrent = queueController.state.value.currentTrack
        if (nextCurrent == null) {
            scope.launch {
                audioEngine.stop()
                sessionRepository?.clear()
            }
        } else if (wasCurrent) {
            launchTrackPlayback(nextCurrent)
        } else {
            scope.launch { saveSessionNow() }
        }
    }

    /**
     * Clears the playback queue, removes persisted session state, and stops active audio playback cleanly.
     */
    fun clearQueue() {
        queueController.clear()
        prefetchJob?.cancel()
        scope.launch {
            audioEngine.stop()
            sessionRepository?.clear()
        }
    }

    /**
     * Toggles the like state of the active track in [LibraryRepository] and [RecommendationEngine].
     */
    fun toggleLikeCurrent() {
        val track = _uiState.value.currentTrack ?: return
        toggleTrackLike(track)
    }

    /**
     * Toggles the persisted like flag for any [track] and records affinity inside [RecommendationEngine].
     */
    fun toggleTrackLike(track: Track) {
        scope.launch {
            val persisted = libraryRepository.getTrack(track.id)
            val currentlyLiked = persisted?.isLiked ?: track.isLiked
            val newLiked = !currentlyLiked
            libraryRepository.setTrackLiked(track, newLiked)
            if (newLiked) {
                recommendationEngine.recordLikeTrack(track)
            }
        }
    }

    /**
     * Records a dislike penalty for the active track via [RecommendationEngine] and skips forward.
     */
    fun dislikeCurrent() {
        val track = _uiState.value.currentTrack ?: return
        scope.launch {
            libraryRepository.setTrackLiked(track, false)
            recommendationEngine.recordDislikeTrack(track)
            toastController.show("Reduced recommendations like ${track.title}")
        }
        next()
    }

    /**
     * Enqueues the active track for offline download using [qualityOverride] or saved default quality.
     */
    fun downloadCurrent(qualityOverride: AudioQuality? = null) {
        val track = _uiState.value.currentTrack ?: return
        downloadTrack(track, qualityOverride)
    }

    /**
     * Enqueues any [track] for offline download using [qualityOverride] or persisted preferred bitrate.
     */
    fun downloadTrack(track: Track, qualityOverride: AudioQuality? = null) {
        val quality = qualityOverride ?: _uiState.value.activeQuality
        scope.launch {
            when (val res = offlineDownloadManager.enqueue(track, quality)) {
                is AppResult.Success -> toastController.show("Downloading (${quality.label}): ${track.title}")
                is AppResult.Failure -> toastController.show(res.error.message, isError = true)
            }
        }
    }

    /**
     * Adds the active track into custom playlist [playlistId] via [LibraryRepository].
     */
    fun addCurrentToPlaylist(playlistId: Long) {
        val track = _uiState.value.currentTrack ?: return
        scope.launch {
            when (val res = libraryRepository.addTrackToPlaylist(playlistId, track)) {
                is AppResult.Success -> toastController.show("Added to playlist")
                is AppResult.Failure -> toastController.show(res.error.message, isError = true)
            }
        }
    }

    /**
     * Clears any transient playback error message displayed in the player bar.
     */
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Persists the active queue and playback cursor immediately into [PlaybackSessionRepository].
     * Called on pause, track switches, and window shutdown.
     */
    suspend fun saveSessionNow() {
        val repo = sessionRepository ?: return
        val q = queueController.state.value
        val state = _uiState.value
        if (q.queue.isEmpty()) {
            repo.clear()
            return
        }
        repo.save(
            PlaybackSessionSnapshot(
                queue = q.queue,
                currentIndex = q.currentIndex,
                positionMs = state.positionMs.coerceAtLeast(0L),
                isShuffleEnabled = q.isShuffleEnabled,
                repeatMode = q.repeatMode,
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }

    /**
     * Cancels active playback, prefetch, and persistence coroutines when the app terminates.
     */
    fun dispose() {
        activePlaybackJob?.cancel()
        accentJob?.cancel()
        prefetchJob?.cancel()
        volumePersistJob?.cancel()
        scope.cancel()
    }

    /**
     * Resolves [track]'s audio stream, invokes [AudioEngine.play] (with one cache-invalidated retry on failure),
     * seeks to [initialPositionMs] once playing, records history, and launches dynamic album-art color extraction.
     */
    private fun launchTrackPlayback(track: Track, initialPositionMs: Long = 0L) {
        activePlaybackJob?.cancel()
        prefetchJob?.cancel()
        pendingRestorePositionMs = 0L
        pendingSeekOnReadyMs = initialPositionMs.coerceAtLeast(0L)
        prefetchedForTrackId = null
        val sessionId = ++playbackSessionSeq
        _uiState.update {
            it.copy(
                currentTrack = track,
                isPlaying = false,
                isResolving = true,
                isBuffering = true,
                positionMs = initialPositionMs.coerceAtLeast(0L),
                durationMs = track.durationSeconds * 1000L,
                errorMessage = null
            )
        }
        extractAccentForTrack(track)

        activePlaybackJob = scope.launch {
            saveSessionNow()
            try {
                val quality = _uiState.value.activeQuality
                var resolved = resolveStream(track, quality)
                if (sessionId != playbackSessionSeq) return@launch

                if (resolved is AppResult.Success) {
                    var playResult = audioEngine.play(resolved.data, track)
                    if (sessionId != playbackSessionSeq) return@launch
                    if (playResult is AppResult.Failure) {
                        resolveStream.invalidate(track.id)
                        resolved = resolveStream(track, quality)
                        if (sessionId != playbackSessionSeq) return@launch
                        if (resolved is AppResult.Success) {
                            playResult = audioEngine.play(resolved.data, track)
                            if (sessionId != playbackSessionSeq) return@launch
                        }
                    }
                    when (playResult) {
                        is AppResult.Failure -> {
                            _uiState.update {
                                it.copy(
                                    isResolving = false,
                                    isBuffering = false,
                                    errorMessage = playResult.error.message
                                )
                            }
                            toastController.show(playResult.error.message, isError = true)
                        }
                        is AppResult.Success -> {
                            _uiState.update { it.copy(isResolving = false, errorMessage = null) }
                            libraryRepository.recordHistory(track, System.currentTimeMillis())
                            recommendationEngine.recordTrackClick(track)
                        }
                    }
                } else if (resolved is AppResult.Failure) {
                    _uiState.update {
                        it.copy(
                            isResolving = false,
                            isBuffering = false,
                            isPlaying = false,
                            errorMessage = resolved.error.message
                        )
                    }
                    toastController.show(resolved.error.message, isError = true)
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                if (sessionId == playbackSessionSeq) {
                    _uiState.update {
                        it.copy(
                            isResolving = false,
                            isBuffering = false,
                            errorMessage = error.message ?: "Playback error"
                        )
                    }
                }
            }
        }
    }

    /**
     * Subscribes to [AudioEngine.snapshot] and [PlaybackQueueController.state] to synchronize UI
     * telemetry and handle track-completion auto-advance once per playback session.
     */
    private fun observeEngineAndQueue() {
        scope.launch {
            combine(audioEngine.snapshot, queueController.state) { snap, queueState ->
                snap to queueState
            }.collectLatest { (snap, queueState) ->
                val currentTrack = queueState.currentTrack
                val restoredPos = pendingRestorePositionMs
                val displayPositionMs = if (snap.status == PlaybackStatus.IDLE && restoredPos > 0L) {
                    restoredPos
                } else {
                    snap.positionMs
                }
                _uiState.update { prev ->
                    prev.copy(
                        currentTrack = currentTrack ?: prev.currentTrack,
                        queue = queueState.queue,
                        currentIndex = queueState.currentIndex,
                        isShuffleEnabled = queueState.isShuffleEnabled,
                        repeatMode = queueState.repeatMode,
                        isPlaying = snap.isPlaying,
                        isBuffering = snap.isBuffering || prev.isResolving,
                        positionMs = displayPositionMs,
                        durationMs = snap.durationMs.takeIf { it > 0L }
                            ?: ((currentTrack?.durationSeconds ?: 0) * 1000L),
                        volume = snap.volume,
                        isMuted = snap.volume <= 0.001f,
                        errorMessage = snap.errorMessage ?: prev.errorMessage
                    )
                }

                if (snap.isPlaying && pendingSeekOnReadyMs > 1_000L) {
                    val seekTarget = pendingSeekOnReadyMs
                    pendingSeekOnReadyMs = 0L
                    scope.launch { audioEngine.seekTo(seekTarget) }
                }

                if (snap.isPlaying && currentTrack != null && prefetchedForTrackId != currentTrack.id) {
                    scheduleNextTrackPrefetch(currentTrack.id)
                }

                if (snap.isPlaying && currentTrack != null) {
                    val now = System.currentTimeMillis()
                    if (now - lastSessionSaveMs >= SESSION_AUTOSAVE_INTERVAL_MS) {
                        lastSessionSaveMs = now
                        scope.launch { saveSessionNow() }
                    }
                }

                if (snap.status == PlaybackStatus.ENDED &&
                    currentTrack != null &&
                    playbackSessionSeq > 0L &&
                    handledEndedSessionSeq != playbackSessionSeq
                ) {
                    handledEndedSessionSeq = playbackSessionSeq
                    handleTrackCompleted(currentTrack)
                }
            }
        }
    }

    /**
     * Awards completion score, increments listening seconds, and advances to the next queued track.
     */
    private suspend fun handleTrackCompleted(completedTrack: Track) {
        recommendationEngine.recordTrackCompletion(completedTrack.id)
        if (completedTrack.durationSeconds > 0) {
            preferencesRepository.addListeningSeconds(completedTrack.durationSeconds.toLong())
        }
        val nextTrack = queueController.next(respectRepeatOne = true)
        if (nextTrack != null) {
            launchTrackPlayback(nextTrack)
        }
    }

    /**
     * Collects lockscreen/notification media key events emitted by [AudioEngine.externalCommands].
     */
    private fun observeExternalCommands() {
        scope.launch {
            audioEngine.externalCommands.collect { command ->
                when (command) {
                    AudioEngineCommand.NEXT -> next()
                    AudioEngineCommand.PREVIOUS -> previous()
                }
            }
        }
    }

    /**
     * Keeps liked, downloaded, volume, and preferred quality flags synchronized with active track state.
     */
    private fun observeLibraryAndPreferences() {
        scope.launch {
            combine(
                preferencesRepository.preferences,
                libraryRepository.observeLikedTracks(),
                libraryRepository.observeDownloadedTracks()
            ) { prefs, liked, downloaded ->
                Triple(prefs, liked.map { it.id }.toSet(), downloaded.map { it.id }.toSet())
            }.collectLatest { (prefs, likedIds, downloadedIds) ->
                if (!initialVolumeRestored) {
                    initialVolumeRestored = true
                    preMuteVolume = prefs.volume.coerceIn(0f, 1f).coerceAtLeast(0.5f)
                    val targetGain = if (prefs.isMuted) 0f else prefs.volume.coerceIn(0f, 1f)
                    _uiState.update { it.copy(volume = targetGain, isMuted = prefs.isMuted || targetGain <= 0.001f) }
                    audioEngine.setVolume(targetGain)
                }
                _uiState.update { state ->
                    val id = state.currentTrack?.id
                    state.copy(
                        activeQuality = prefs.downloadQuality,
                        isCurrentLiked = id != null && id in likedIds,
                        isCurrentDownloaded = id != null && id in downloadedIds
                    )
                }
            }
        }
    }

    /**
     * Restores the last persisted queue and track cursor on startup in a paused state without auto-playing.
     */
    private fun restoreLastSession() {
        val repo = sessionRepository ?: return
        scope.launch {
            val snapshot = repo.load() ?: return@launch
            if (snapshot.queue.isEmpty() || _uiState.value.currentTrack != null) return@launch
            queueController.restoreState(snapshot)
            val restoredTrack = queueController.state.value.currentTrack ?: return@launch
            val safePos = snapshot.positionMs.coerceAtLeast(0L)
            pendingRestorePositionMs = safePos
            _uiState.update {
                it.copy(
                    currentTrack = restoredTrack,
                    queue = queueController.state.value.queue,
                    currentIndex = queueController.state.value.currentIndex,
                    isShuffleEnabled = queueController.state.value.isShuffleEnabled,
                    repeatMode = queueController.state.value.repeatMode,
                    positionMs = safePos,
                    durationMs = restoredTrack.durationSeconds * 1000L
                )
            }
            extractAccentForTrack(restoredTrack)
        }
    }

    /**
     * Schedules background prefetching of the next queued track after 2.5 seconds of stable playback.
     */
    private fun scheduleNextTrackPrefetch(activeTrackId: String) {
        val prefetcher = streamPrefetcher ?: return
        prefetchedForTrackId = activeTrackId
        prefetchJob?.cancel()
        prefetchJob = scope.launch {
            delay(PREFETCH_STABILITY_DELAY_MS)
            val upcoming = queueController.peekNext() ?: return@launch
            if (upcoming.id == activeTrackId) return@launch
            runCatching { prefetcher.prefetch(upcoming, _uiState.value.activeQuality) }
        }
    }

    /**
     * Debounces volume/mute writes to [PreferencesRepository] so slider drags avoid excessive disk I/O.
     */
    private fun persistVolumeDebounced(volume: Float, isMuted: Boolean) {
        volumePersistJob?.cancel()
        volumePersistJob = scope.launch {
            delay(VOLUME_PERSIST_DEBOUNCE_MS)
            preferencesRepository.setVolumeState(volume.coerceIn(0f, 1f), isMuted)
        }
    }

    private companion object {
        const val PREFETCH_STABILITY_DELAY_MS = 2_500L
        const val VOLUME_PERSIST_DEBOUNCE_MS = 250L
        const val SESSION_AUTOSAVE_INTERVAL_MS = 15_000L
    }

    /**
     * Records an early-skip recommendation penalty when leaving a track within its first 20 seconds.
     */
    private fun recordOutgoingEarlySkip() {
        val current = _uiState.value.currentTrack ?: return
        val pos = _uiState.value.positionMs
        if (pos in 1L until 20_000L) {
            scope.launch {
                recommendationEngine.recordEarlySkip(current.id, pos)
            }
        }
    }

    /**
     * Launches asynchronous album-art color sampling via [ColorExtractor] for smooth 450ms tinting.
     */
    private fun extractAccentForTrack(track: Track) {
        accentJob?.cancel()
        accentJob = scope.launch {
            val extracted = ColorExtractor.extractDominantColor(
                imageUrl = track.coverUrl,
                fallback = IvidsColorTokens.ChalkInk
            )
            _uiState.update { it.copy(dynamicAccent = extracted) }
        }
    }
}
