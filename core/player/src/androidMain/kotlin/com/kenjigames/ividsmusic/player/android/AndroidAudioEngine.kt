package com.kenjigames.ividsmusic.player.android

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.AudioEngineCommand
import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.EngineSnapshot
import com.kenjigames.ividsmusic.domain.model.PlaybackStatus
import com.kenjigames.ividsmusic.domain.model.StreamSource
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.AudioEngine
import com.kenjigames.ividsmusic.player.EngineStateMachine
import com.kenjigames.ividsmusic.player.StreamSourceValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android [AudioEngine] bridging domain commands to [AndroidPlaybackService] via Media3 [MediaController].
 * Emits [snapshot] updates with 250ms position polling and exposes lockscreen/notification [externalCommands].
 */
@OptIn(UnstableApi::class)
class AndroidAudioEngine(
    private val context: Context,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : AudioEngine {

    private val appContext: Context = context.applicationContext
    private val stateMachine = EngineStateMachine()
    private val engineScope = CoroutineScope(SupervisorJob() + mainDispatcher)
    private val connectionMutex = Mutex()
    private val _externalCommands = MutableSharedFlow<AudioEngineCommand>(extraBufferCapacity = 8)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pollingJob: Job? = null
    private var activeTrackId: String = ""
    private var currentVolume: Float = 1.0f

    override val snapshot: StateFlow<EngineSnapshot>
        get() = stateMachine.snapshot

    override val externalCommands: Flow<AudioEngineCommand> = _externalCommands.asSharedFlow()

    private val playerListener = object : Player.Listener {

        /**
         * Maps ExoPlayer [playbackState] changes to [EngineStateMachine] buffering, ready, and ended transitions.
         * Starts or stops the 250ms position polling loop without masking prior [PlaybackStatus.ERROR] states.
         */
        override fun onPlaybackStateChanged(playbackState: Int) {
            val activeController = controller ?: return
            val generation = stateMachine.currentGeneration()
            val durationMs = readControllerDurationMs(activeController)
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    stopPolling()
                    stateMachine.onBuffering(generation, durationMs)
                }
                Player.STATE_READY -> {
                    stateMachine.onReady(generation, durationMs)
                    if (activeController.isPlaying) {
                        stateMachine.onPlaying(generation, durationMs)
                        startPolling(generation)
                    } else if (!activeController.playWhenReady) {
                        stopPolling()
                        stateMachine.onPaused(generation)
                    }
                }
                Player.STATE_ENDED -> {
                    stopPolling()
                    stateMachine.onCompleted(generation)
                }
                Player.STATE_IDLE -> stopPolling()
            }
        }

        /**
         * Updates playing/paused status when ExoPlayer audio output or audio focus state changes.
         * Drives the 250ms progress polling coroutine based on active playback state.
         */
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val activeController = controller ?: return
            val generation = stateMachine.currentGeneration()
            val durationMs = readControllerDurationMs(activeController)
            if (isPlaying) {
                stateMachine.onPlaying(generation, durationMs)
                startPolling(generation)
                return
            }
            stopPolling()
            if (activeController.playbackState == Player.STATE_READY && !activeController.playWhenReady) {
                stateMachine.onPaused(generation)
            }
        }

        /**
         * Translates ExoPlayer [PlaybackException] events into [DomainError] and halts position polling.
         * Updates [snapshot] with [PlaybackStatus.ERROR] while preserving existing volume settings.
         */
        override fun onPlayerError(error: PlaybackException) {
            stopPolling()
            val mapped = PlaybackErrorMapper.map(error, activeTrackId)
            stateMachine.onError(stateMachine.currentGeneration(), mapped)
        }
    }

    private val controllerListener = object : MediaController.Listener {

        /**
         * Intercepts custom next/previous commands broadcast from [AndroidPlaybackService] notifications.
         * Emits corresponding [AudioEngineCommand] values into [externalCommands].
         */
        override fun onCustomCommand(
            controller: MediaController,
            command: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (command.customAction) {
                AndroidPlaybackService.ACTION_COMMAND_NEXT ->
                    _externalCommands.tryEmit(AudioEngineCommand.NEXT)
                AndroidPlaybackService.ACTION_COMMAND_PREVIOUS ->
                    _externalCommands.tryEmit(AudioEngineCommand.PREVIOUS)
            }
            return SimpleImmediateResult(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        /**
         * Clears stale [MediaController] references when the remote service disconnects unexpectedly.
         * Allows subsequent [play] calls to reconnect cleanly to [AndroidPlaybackService].
         */
        override fun onDisconnected(controller: MediaController) {
            stopPolling()
            if (this@AndroidAudioEngine.controller === controller) {
                this@AndroidAudioEngine.controller = null
                this@AndroidAudioEngine.controllerFuture = null
            }
        }
    }

    /**
     * Validates [source], connects to [AndroidPlaybackService], and starts playback of [track].
     * Returns [AppResult.Success] once handed off to Media3 or [AppResult.Failure] with mapped [DomainError].
     */
    override suspend fun play(source: StreamSource, track: Track): AppResult<Unit> {
        if (stateMachine.isReleased()) {
            return AppResult.Failure(DomainError.Cancelled("AudioEngine has been released"))
        }
        val validationError = StreamSourceValidator.validate(source)
        if (validationError != null) {
            stateMachine.onError(null, validationError)
            return AppResult.Failure(validationError)
        }

        val resolvedUri = resolvePlayableUri(source, track.id)
        if (resolvedUri is AppResult.Failure) {
            stateMachine.onError(null, resolvedUri.error)
            return resolvedUri
        }
        val playableUri = (resolvedUri as AppResult.Success).data
        val generation = stateMachine.beginTrack(track)
            ?: return AppResult.Failure(DomainError.Cancelled("AudioEngine has been released"))

        return try {
            withContext(mainDispatcher) {
                stopPolling()
                activeTrackId = track.id
                val mediaController = getOrConnectController()
                if (stateMachine.isReleased() || stateMachine.currentGeneration() != generation) {
                    return@withContext AppResult.Failure(DomainError.Cancelled("Playback superseded"))
                }
                val item = buildMediaItem(track = track, source = source, resolvedUri = playableUri)
                mediaController.volume = currentVolume
                mediaController.setMediaItem(item)
                mediaController.prepare()
                mediaController.play()
                AppResult.Success(Unit)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (throwable: Throwable) {
            val mapped = DomainError.Unknown(
                message = throwable.message ?: "Failed to start Android playback",
                causeName = throwable.javaClass.simpleName,
            )
            stateMachine.onError(generation, mapped)
            AppResult.Failure(mapped)
        }
    }

    /**
     * Pauses active playback on the Media3 main thread and halts 250ms position polling.
     * Safe no-op when disconnected or already released.
     */
    override suspend fun pause() {
        if (stateMachine.isReleased()) return
        withContext(mainDispatcher) {
            stopPolling()
            controller?.pause()
            if (stateMachine.snapshot.value.status == PlaybackStatus.PLAYING) {
                stateMachine.onPaused(stateMachine.currentGeneration())
            }
        }
    }

    /**
     * Resumes paused playback on the Media3 main thread and restarts position polling when playing.
     * Safe no-op when disconnected or already released.
     */
    override suspend fun resume() {
        if (stateMachine.isReleased()) return
        withContext(mainDispatcher) {
            val activeController = controller ?: return@withContext
            activeController.play()
            if (activeController.isPlaying) {
                val gen = stateMachine.currentGeneration()
                stateMachine.onPlaying(gen, readControllerDurationMs(activeController))
                startPolling(gen)
            }
        }
    }

    /**
     * Seeks active playback to clamped [positionMs] on the Media3 main thread.
     * Updates [snapshot] immediately so UI scrubbers stay synchronized between 250ms polls.
     */
    override suspend fun seekTo(positionMs: Long) {
        if (stateMachine.isReleased()) return
        withContext(mainDispatcher) {
            val activeController = controller ?: return@withContext
            val durationMs = readControllerDurationMs(activeController)
                .takeIf { it > 0L } ?: stateMachine.snapshot.value.durationMs
            val clamped = StreamSourceValidator.clampSeekPosition(positionMs, durationMs)
            activeController.seekTo(clamped)
            stateMachine.onProgress(stateMachine.currentGeneration(), clamped, durationMs)
        }
    }

    /**
     * Clamps [volume] into `0.0f..1.0f`, persists it for future tracks, and applies it to ExoPlayer.
     * Updates [EngineSnapshot.volume] via [EngineStateMachine].
     */
    override suspend fun setVolume(volume: Float) {
        val clamped = StreamSourceValidator.clampVolume(volume)
        currentVolume = clamped
        stateMachine.onVolumeChanged(clamped)
        if (stateMachine.isReleased()) return
        withContext(mainDispatcher) {
            controller?.volume = clamped
        }
    }

    /**
     * Halts current playback, clears queued items, and resets [snapshot] to [PlaybackStatus.IDLE].
     * Preserves the active [MediaController] session connection and configured volume.
     */
    override suspend fun stop() {
        if (stateMachine.isReleased()) return
        withContext(mainDispatcher) {
            stopPolling()
            stateMachine.onStopped()
            controller?.apply {
                stop()
                clearMediaItems()
            }
        }
    }

    /**
     * Permanently terminates the engine, detaches listeners, cancels polling, and releases [MediaController].
     * Dispatches controller cleanup on the main looper without relying on the cancelled [engineScope].
     */
    override fun release() {
        stateMachine.onReleased()
        stopPolling()
        engineScope.cancel()
        val futureToRelease = controllerFuture
        val controllerToRelease = controller
        controllerFuture = null
        controller = null

        val releaseAction = Runnable {
            controllerToRelease?.removeListener(playerListener)
            if (futureToRelease != null) {
                MediaController.releaseFuture(futureToRelease)
            } else {
                controllerToRelease?.release()
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            releaseAction.run()
        } else {
            Handler(Looper.getMainLooper()).post(releaseAction)
        }
    }

    /**
     * Resolves and verifies readability of [source], ensuring missing local files fail fast before Media3 IPC.
     * Accepts Android `content://` URIs directly and normalizes filesystem paths or `file://` URIs.
     */
    private fun resolvePlayableUri(source: StreamSource, trackId: String): AppResult<Uri> {
        return when (source) {
            is StreamSource.RemoteUrl -> AppResult.Success(Uri.parse(source.url))
            is StreamSource.LocalFile -> {
                val raw = source.filePath
                if (raw.startsWith("content://", ignoreCase = true)) {
                    return AppResult.Success(Uri.parse(raw))
                }
                val cleanPath = if (raw.startsWith("file://", ignoreCase = true)) {
                    Uri.parse(raw).path ?: raw.removePrefix("file://")
                } else {
                    raw
                }
                val file = File(cleanPath)
                if (!file.exists() || !file.isFile || !file.canRead()) {
                    return AppResult.Failure(
                        DomainError.StreamNotFound("Local audio file not found for track '$trackId': $raw"),
                    )
                }
                AppResult.Success(Uri.fromFile(file))
            }
        }
    }

    /**
     * Lazily connects or returns the active [MediaController] bound to [AndroidPlaybackService] under [connectionMutex].
     * Registers [playerListener] once per controller instance on the main thread.
     */
    private suspend fun getOrConnectController(): MediaController {
        return connectionMutex.withLock {
            controller?.takeIf { it.isConnected }?.let { return@withLock it }
            val sessionToken = SessionToken(
                appContext,
                ComponentName(appContext, AndroidPlaybackService::class.java),
            )
            val future = MediaController.Builder(appContext, sessionToken)
                .setApplicationLooper(Looper.getMainLooper())
                .setListener(controllerListener)
                .buildAsync()
            controllerFuture = future
            val connected = future.awaitOnMain()
            if (stateMachine.isReleased()) {
                MediaController.releaseFuture(future)
                throw CancellationException("AudioEngine released during MediaController connect")
            }
            connected.removeListener(playerListener)
            connected.addListener(playerListener)
            controller = connected
            connected
        }
    }

    /**
     * Builds a Media3 [MediaItem] embedding lockscreen metadata, artwork URI, MIME type, and custom headers.
     * Populates both `requestMetadata.mediaUri` and `mediaMetadata.extras` for safe Media3 session IPC transfer.
     */
    private fun buildMediaItem(
        track: Track,
        source: StreamSource,
        resolvedUri: Uri,
    ): MediaItem {
        val headers = (source as? StreamSource.RemoteUrl)?.headers.orEmpty()
        val mimeType = (source as? StreamSource.RemoteUrl)?.mimeType
        val extras = StreamHeaderRegistry.encodeExtras(headers = headers, mimeType = mimeType)

        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(track.coverUrl.takeIf { it.isNotBlank() }?.let(Uri::parse))
            .setExtras(extras)
            .build()

        val requestMetadata = MediaItem.RequestMetadata.Builder()
            .setMediaUri(resolvedUri)
            .setExtras(extras)
            .build()

        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(resolvedUri)
            .apply {
                if (!mimeType.isNullOrBlank()) {
                    setMimeType(mimeType)
                }
            }
            .setRequestMetadata(requestMetadata)
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * Launches a 250ms main-thread polling loop that pushes ExoPlayer position and duration into [stateMachine].
     * Automatically terminates when playback pauses, ends, errors, or [generation] changes.
     */
    private fun startPolling(generation: Long) {
        pollingJob?.cancel()
        pollingJob = engineScope.launch {
            while (isActive && !stateMachine.isReleased() && stateMachine.currentGeneration() == generation) {
                val activeController = controller ?: break
                if (!activeController.isPlaying) break
                val posMs = activeController.currentPosition.coerceAtLeast(0L)
                val durMs = readControllerDurationMs(activeController)
                stateMachine.onProgress(generation, posMs, durMs)
                delay(POSITION_POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * Cancels the active 250ms position polling coroutine cleanly.
     * Prevents stale progress updates during pause, stop, seek, or release transitions.
     */
    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * Reads [MediaController.getDuration] safely, converting [C.TIME_UNSET] or negative values to `0L`.
     * Prevents invalid Media3 sentinel values from corrupting [EngineSnapshot.durationMs].
     */
    private fun readControllerDurationMs(activeController: MediaController): Long {
        val duration = activeController.duration
        return if (duration == C.TIME_UNSET || duration < 0L) 0L else duration
    }

    /**
     * Bridges a Guava [ListenableFuture] to Kotlin coroutines using Android's main executor.
     * Cancels the underlying future automatically if the awaiting coroutine is cancelled.
     */
    private suspend fun <T> ListenableFuture<T>.awaitOnMain(): T {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { cancel(false) }
            addListener(
                {
                    if (!continuation.isActive) return@addListener
                    try {
                        continuation.resume(get())
                    } catch (e: ExecutionException) {
                        continuation.resumeWithException(e.cause ?: e)
                    } catch (t: Throwable) {
                        continuation.resumeWithException(t)
                    }
                },
                ContextCompat.getMainExecutor(appContext),
            )
        }
    }

    /**
     * Minimal completed [ListenableFuture] for synchronous [MediaController.Listener.onCustomCommand] acknowledgments.
     * Avoids exposing transitive Guava `Futures` helpers directly in module dependencies.
     */
    private class SimpleImmediateResult<T>(
        private val result: T,
    ) : ListenableFuture<T> {
        /** Reports cancellation as unsupported because [result] is already resolved. */
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false

        /** Always returns false because immediate results cannot be cancelled. */
        override fun isCancelled(): Boolean = false

        /** Always returns true because [result] is immediately available. */
        override fun isDone(): Boolean = true

        /** Returns the resolved [result] immediately without blocking. */
        override fun get(): T = result

        /** Returns the resolved [result] immediately regardless of timeout arguments. */
        override fun get(timeout: Long, unit: java.util.concurrent.TimeUnit): T = result

        /** Dispatches [listener] immediately on [executor] for Media3 callback completion. */
        override fun addListener(listener: Runnable, executor: java.util.concurrent.Executor) {
            executor.execute(listener)
        }
    }

    private companion object {
        const val POSITION_POLL_INTERVAL_MS: Long = 250L
    }
}
