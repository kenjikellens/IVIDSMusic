package com.kenjigames.ividsmusic.player.desktop

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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.net.URI

/**
 * Windows Desktop [AudioEngine] powered by [DesktopMediaBackend] (JavaFX Media 17.0.12 in production).
 * Guarantees deterministic `stop(); dispose()` cleanup on every track switch, stop, and release.
 */
class DesktopAudioEngine internal constructor(
    private val backend: DesktopMediaBackend = JavaFxMediaBackend(),
    pollingDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : AudioEngine {

    /**
     * Public zero-argument constructor used by Koin DI and standalone desktop callers.
     * Uses [JavaFxMediaBackend] and [Dispatchers.Default] for 250ms position polling.
     */
    constructor() : this(
        backend = JavaFxMediaBackend(),
        pollingDispatcher = Dispatchers.Default,
    )

    private val stateMachine = EngineStateMachine()
    private val engineScope = CoroutineScope(SupervisorJob() + pollingDispatcher)
    private val controlMutex = Mutex()
    private val handleLock = Any()
    private val pollingLock = Any()
    private val _externalCommands = MutableSharedFlow<AudioEngineCommand>(extraBufferCapacity = 8)

    private var activeHandle: DesktopMediaHandle? = null
    private var pollingJob: Job? = null
    private var currentVolume: Float = 1.0f

    override val snapshot: StateFlow<EngineSnapshot>
        get() = stateMachine.snapshot

    override val externalCommands: Flow<AudioEngineCommand> = _externalCommands.asSharedFlow()

    /**
     * Emits a desktop media-key or external transport [command] to [externalCommands] observers.
     * Exposed internally for desktop keybinding integration and unit tests.
     */
    internal fun emitExternalCommand(command: AudioEngineCommand): Boolean {
        return _externalCommands.tryEmit(command)
    }

    /**
     * Validates [source], deterministically tears down any previous player handle, and starts [track] playback.
     * Maps invalid inputs, missing local files, and headless/JavaFX failures cleanly into [AppResult.Failure].
     */
    override suspend fun play(source: StreamSource, track: Track): AppResult<Unit> {
        if (stateMachine.isReleased()) {
            return AppResult.Failure(DomainError.Cancelled("AudioEngine has been released"))
        }
        val validationError = StreamSourceValidator.validate(source)
        if (validationError != null) {
            stopPolling()
            swapAndDisposeActiveHandle()
            stateMachine.onError(null, validationError)
            return AppResult.Failure(validationError)
        }

        return controlMutex.withLock {
            if (stateMachine.isReleased()) {
                return@withLock AppResult.Failure(DomainError.Cancelled("AudioEngine has been released"))
            }
            stopPolling()
            swapAndDisposeActiveHandle()

            val resolvedSource = resolveMediaSource(source, track.id)
            if (resolvedSource is AppResult.Failure) {
                stateMachine.onError(null, resolvedSource.error)
                return@withLock resolvedSource
            }
            val (mediaUri, headers, isLocalFile) = (resolvedSource as AppResult.Success).data

            val generation = stateMachine.beginTrack(track)
                ?: return@withLock AppResult.Failure(DomainError.Cancelled("AudioEngine has been released"))

            val callbacks = buildHandleCallbacks(
                generation = generation,
                trackId = track.id,
                isLocalFile = isLocalFile,
            )

            val createResult = try {
                backend.createAndStart(
                    mediaUri = mediaUri,
                    headers = headers,
                    initialVolume = currentVolume,
                    callbacks = callbacks,
                    cacheKeyHint = track.videoId?.takeIf { it.isNotBlank() } ?: track.id,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                Result.failure(throwable)
            }

            val createdHandle = createResult.getOrElse { throwable ->
                val mapped = DesktopErrorMapper.map(throwable, track.id, isLocalFile)
                stateMachine.onError(generation, mapped)
                return@withLock AppResult.Failure(mapped)
            }

            val installed = installHandleIfActive(generation, createdHandle)
            if (!installed) {
                disposeHandleSafely(createdHandle)
                return@withLock AppResult.Failure(DomainError.Cancelled("Playback superseded or engine released"))
            }
            AppResult.Success(Unit)
        }
    }

    /**
     * Pauses the active desktop player handle and stops 250ms position polling.
     * Safe no-op when stopped or already released.
     */
    override suspend fun pause() {
        if (stateMachine.isReleased()) return
        controlMutex.withLock {
            if (stateMachine.isReleased()) return@withLock
            stopPolling()
            activeHandleSnapshot()?.pause()
            stateMachine.onPaused(stateMachine.currentGeneration())
        }
    }

    /**
     * Resumes playback on the active desktop player handle and restarts 250ms polling.
     * Safe no-op when no active handle exists or after [release].
     */
    override suspend fun resume() {
        if (stateMachine.isReleased()) return
        controlMutex.withLock {
            if (stateMachine.isReleased()) return@withLock
            val handle = activeHandleSnapshot() ?: return@withLock
            handle.play()
            val generation = stateMachine.currentGeneration()
            stateMachine.onPlaying(generation, handle.durationMs())
            startPolling(generation)
        }
    }

    /**
     * Clamps [positionMs] to known track bounds, seeks the active handle, and updates [snapshot] immediately.
     * Prevents UI scrubber bounce between 250ms polling ticks.
     */
    override suspend fun seekTo(positionMs: Long) {
        if (stateMachine.isReleased()) return
        controlMutex.withLock {
            if (stateMachine.isReleased()) return@withLock
            val handle = activeHandleSnapshot() ?: return@withLock
            val durationMs = handle.durationMs().takeIf { it > 0L } ?: stateMachine.snapshot.value.durationMs
            val clamped = StreamSourceValidator.clampSeekPosition(positionMs, durationMs)
            handle.seekTo(clamped)
            stateMachine.onProgress(stateMachine.currentGeneration(), clamped, durationMs)
        }
    }

    /**
     * Clamps [volume] into `0.0f..1.0f`, saves it for future tracks, and updates any active handle.
     * Updates [EngineSnapshot.volume] synchronously through [EngineStateMachine].
     */
    override suspend fun setVolume(volume: Float) {
        val clamped = StreamSourceValidator.clampVolume(volume)
        controlMutex.withLock {
            currentVolume = clamped
            stateMachine.onVolumeChanged(clamped)
            if (!stateMachine.isReleased()) {
                activeHandleSnapshot()?.setVolume(clamped)
            }
        }
    }

    /**
     * Stops playback, resets [snapshot] to [PlaybackStatus.IDLE], and invokes `stop(); dispose()` on the handle.
     * Preserves engine readiness and configured volume for future [play] requests.
     */
    override suspend fun stop() {
        if (stateMachine.isReleased()) return
        controlMutex.withLock {
            if (stateMachine.isReleased()) return@withLock
            stopPolling()
            stateMachine.onStopped()
            swapAndDisposeActiveHandle()
        }
    }

    /**
     * Permanently terminates the engine, stops polling, cancels coroutines, and runs `stop(); dispose()` immediately.
     * Uses [handleLock] directly so cleanup never deadlocks behind [controlMutex].
     */
    override fun release() {
        stateMachine.onReleased()
        stopPolling()
        swapAndDisposeActiveHandle()
        engineScope.cancel()
    }

    /**
     * Converts [source] into a playable URI string, verifying local file existence and converting Windows paths to URIs.
     * Prevents unencoded file paths or missing files from reaching JavaFX `Media`.
     */
    private fun resolveMediaSource(source: StreamSource, trackId: String): AppResult<ResolvedDesktopSource> {
        return when (source) {
            is StreamSource.RemoteUrl -> AppResult.Success(
                ResolvedDesktopSource(
                    mediaUri = source.url,
                    headers = source.headers,
                    isLocalFile = false,
                ),
            )
            is StreamSource.LocalFile -> {
                val file = normalizeLocalFile(source.filePath)
                if (!file.exists() || !file.isFile) {
                    return AppResult.Failure(
                        DomainError.StreamNotFound("Local audio file not found for track '$trackId': ${source.filePath}"),
                    )
                }
                if (!file.canRead()) {
                    return AppResult.Failure(
                        DomainError.Storage("Local audio file is not readable for track '$trackId': ${source.filePath}"),
                    )
                }
                AppResult.Success(
                    ResolvedDesktopSource(
                        mediaUri = file.toURI().toASCIIString(),
                        headers = emptyMap(),
                        isLocalFile = true,
                    ),
                )
            }
        }
    }

    /**
     * Normalizes either raw Windows filesystem paths or `file://` URIs into a [File] instance.
     * Falls back to plain path handling when malformed URI syntax appears in legacy local paths.
     */
    private fun normalizeLocalFile(rawPath: String): File {
        if (!rawPath.startsWith("file://", ignoreCase = true)) {
            return File(rawPath)
        }
        return runCatching { File(URI(rawPath)) }
            .getOrElse { File(rawPath.removePrefix("file://")) }
    }

    /**
     * Constructs generation-scoped [MediaHandleCallbacks] routing backend events into [stateMachine].
     * Ensures stale handles can never mutate state or restart polling after replacement or release.
     */
    private fun buildHandleCallbacks(
        generation: Long,
        trackId: String,
        isLocalFile: Boolean,
    ): MediaHandleCallbacks {
        return MediaHandleCallbacks(
            onReady = { durationMs ->
                stateMachine.onReady(generation, durationMs)
            },
            onPlaying = { durationMs ->
                stateMachine.onPlaying(generation, durationMs)
                startPolling(generation)
            },
            onPaused = {
                stopPolling()
                stateMachine.onPaused(generation)
            },
            onStalled = {
                stopPolling()
                stateMachine.onBuffering(generation, 0L)
            },
            onEndOfMedia = {
                stopPolling()
                stateMachine.onCompleted(generation)
            },
            onError = { throwable ->
                stopPolling()
                val mapped = DesktopErrorMapper.map(throwable, trackId, isLocalFile)
                stateMachine.onError(generation, mapped)
            },
        )
    }

    /**
     * Installs [handle] as [activeHandle] under [handleLock] only if [generation] is still active.
     * Returns false when superseded or released so the caller disposes [handle] immediately.
     */
    private fun installHandleIfActive(generation: Long, handle: DesktopMediaHandle): Boolean {
        synchronized(handleLock) {
            if (stateMachine.isReleased() || stateMachine.currentGeneration() != generation) {
                return false
            }
            activeHandle = handle
            return true
        }
    }

    /**
     * Reads [activeHandle] atomically under [handleLock].
     * Returns null when stopped or released.
     */
    private fun activeHandleSnapshot(): DesktopMediaHandle? {
        return synchronized(handleLock) { activeHandle }
    }

    /**
     * Detaches [activeHandle] under [handleLock] and runs deterministic `stop(); dispose()` outside the lock.
     * Ensures JavaFX handle disposal never executes inside [handleLock].
     */
    private fun swapAndDisposeActiveHandle() {
        val previous = synchronized(handleLock) {
            val current = activeHandle
            activeHandle = null
            current
        }
        disposeHandleSafely(previous)
    }

    /**
     * Invokes `stop()` followed unconditionally by `dispose()` on [handle] inside `try/finally`.
     * Preserves `CancellationException` and JVM errors while ensuring disposal runs even if `stop()` throws.
     */
    private fun disposeHandleSafely(handle: DesktopMediaHandle?) {
        if (handle == null) return
        try {
            handle.stop()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Disposal in finally must still execute when stop() raises an exception.
        } finally {
            try {
                handle.dispose()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Best-effort secondary cleanup guard during native handle release.
            }
        }
    }

    /**
     * Starts a 250ms polling loop pushing desktop position and duration updates into [stateMachine].
     * Automatically halts on pause, end-of-media, error, generation change, or engine release.
     */
    private fun startPolling(generation: Long) {
        synchronized(pollingLock) {
            pollingJob?.cancel()
            if (stateMachine.isReleased() || stateMachine.currentGeneration() != generation) return
            pollingJob = engineScope.launch {
                while (isActive && !stateMachine.isReleased() && stateMachine.currentGeneration() == generation) {
                    delay(POSITION_POLL_INTERVAL_MS)
                    if (stateMachine.currentGeneration() != generation || stateMachine.isReleased()) break
                    val handle = activeHandleSnapshot() ?: break
                    if (stateMachine.snapshot.value.status != PlaybackStatus.PLAYING) break
                    stateMachine.onProgress(
                        generation = generation,
                        positionMs = handle.currentPositionMs(),
                        durationMs = handle.durationMs(),
                    )
                }
            }
        }
    }

    /**
     * Cancels any active 250ms position polling coroutine under [pollingLock].
     * Safe to invoke multiple times from any thread.
     */
    private fun stopPolling() {
        synchronized(pollingLock) {
            pollingJob?.cancel()
            pollingJob = null
        }
    }

    /**
     * Normalized desktop media payload pairing a URI string with optional HTTP headers and source type flag.
     * Returned by [resolveMediaSource] before opening [DesktopMediaBackend].
     */
    private data class ResolvedDesktopSource(
        val mediaUri: String,
        val headers: Map<String, String>,
        val isLocalFile: Boolean,
    )

    private companion object {
        const val POSITION_POLL_INTERVAL_MS: Long = 250L
    }
}
