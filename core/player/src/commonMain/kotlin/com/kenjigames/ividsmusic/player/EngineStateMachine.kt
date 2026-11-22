package com.kenjigames.ividsmusic.player

import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.EngineSnapshot
import com.kenjigames.ividsmusic.domain.model.PlaybackStatus
import com.kenjigames.ividsmusic.domain.model.Track
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.updateAndGet

/**
 * Lock-free playback state machine that produces [EngineSnapshot] updates and rejects stale callbacks.
 * Mutates an atomic [MutableStateFlow] of generation-tagged state consumed by [AudioEngine] implementations.
 */
internal class EngineStateMachine {

    /**
     * Internal atomic state pairing monotonic playback [generation] and [released] guard with [snapshot].
     * Enables compare-and-set transitions without platform-specific mutex primitives.
     */
    private data class InternalState(
        val generation: Long = 0L,
        val released: Boolean = false,
        val snapshot: EngineSnapshot = EngineSnapshot(),
    )

    private val internalState = MutableStateFlow(InternalState())

    /**
     * Exposes reactive [EngineSnapshot] updates synchronized directly with [internalState].
     * Avoids allocating background coroutines while preserving [StateFlow] conflation semantics.
     */
    @OptIn(ExperimentalForInheritanceCoroutinesApi::class)
    val snapshot: StateFlow<EngineSnapshot> = object : StateFlow<EngineSnapshot> {
        override val replayCache: List<EngineSnapshot>
            get() = listOf(internalState.value.snapshot)

        override val value: EngineSnapshot
            get() = internalState.value.snapshot

        /**
         * Emits distinct [EngineSnapshot] updates by collecting [internalState] until cancellation.
         * Conflates duplicate snapshots so observers only react to actual playback state changes.
         */
        override suspend fun collect(collector: FlowCollector<EngineSnapshot>): Nothing {
            var lastEmitted: EngineSnapshot? = null
            internalState.collect { current ->
                val nextSnapshot = current.snapshot
                if (lastEmitted != nextSnapshot) {
                    lastEmitted = nextSnapshot
                    collector.emit(nextSnapshot)
                }
            }
        }
    }

    /**
     * Returns the current monotonic playback generation token for async callback verification.
     * Reads [internalState] without mutating engine state.
     */
    fun currentGeneration(): Long = internalState.value.generation

    /**
     * Returns true once [onReleased] has permanently terminated this state machine.
     * Reads [internalState] without mutating engine state.
     */
    fun isReleased(): Boolean = internalState.value.released

    /**
     * Starts a new playback generation for [track], transitioning [snapshot] to [PlaybackStatus.BUFFERING].
     * Returns the new generation token, or null if [onReleased] was already called.
     */
    fun beginTrack(track: Track): Long? {
        val estimatedDurationMs = (track.durationSeconds.toLong() * 1000L).coerceAtLeast(0L)
        var assignedGeneration: Long? = null
        internalState.updateAndGet { prev ->
            if (prev.released) {
                assignedGeneration = null
                return@updateAndGet prev
            }
            val nextGeneration = prev.generation + 1L
            assignedGeneration = nextGeneration
            prev.copy(
                generation = nextGeneration,
                snapshot = prev.snapshot.copy(
                    status = PlaybackStatus.BUFFERING,
                    isPlaying = false,
                    isBuffering = true,
                    positionMs = 0L,
                    durationMs = estimatedDurationMs,
                    errorMessage = null,
                ),
            )
        }
        return assignedGeneration
    }

    /**
     * Clears initial buffering when media metadata becomes ready for [generation] and updates [durationMs].
     * Ignores stale generations or released machines to preserve active track state.
     */
    fun onReady(generation: Long, durationMs: Long) {
        updateForGeneration(generation) { prevSnapshot ->
            val resolvedDuration = resolveDuration(durationMs, prevSnapshot.durationMs)
            prevSnapshot.copy(
                isBuffering = false,
                durationMs = resolvedDuration,
            )
        }
    }

    /**
     * Transitions [snapshot] to [PlaybackStatus.BUFFERING] while [generation] remains active.
     * Clears active playing flag and updates [durationMs] when reported by the backend.
     */
    fun onBuffering(generation: Long, durationMs: Long) {
        updateForGeneration(generation) { prevSnapshot ->
            val resolvedDuration = resolveDuration(durationMs, prevSnapshot.durationMs)
            prevSnapshot.copy(
                status = PlaybackStatus.BUFFERING,
                isPlaying = false,
                isBuffering = true,
                durationMs = resolvedDuration,
                errorMessage = null,
            )
        }
    }

    /**
     * Transitions [snapshot] to [PlaybackStatus.PLAYING] for active [generation] and refreshes [durationMs].
     * Clears buffering and error flags once native audio output is actively running.
     */
    fun onPlaying(generation: Long, durationMs: Long) {
        updateForGeneration(generation) { prevSnapshot ->
            val resolvedDuration = resolveDuration(durationMs, prevSnapshot.durationMs)
            prevSnapshot.copy(
                status = PlaybackStatus.PLAYING,
                isPlaying = true,
                isBuffering = false,
                durationMs = resolvedDuration,
                errorMessage = null,
            )
        }
    }

    /**
     * Transitions [snapshot] to [PlaybackStatus.PAUSED] for active [generation].
     * Preserves playback position, duration, and configured volume.
     */
    fun onPaused(generation: Long) {
        updateForGeneration(generation) { prevSnapshot ->
            prevSnapshot.copy(
                status = PlaybackStatus.PAUSED,
                isPlaying = false,
                isBuffering = false,
            )
        }
    }

    /**
     * Updates clamped playback [positionMs] and [durationMs] while [generation] is in an active playback status.
     * Drops progress polls if the track already ended, stopped, failed, or was superseded.
     */
    fun onProgress(generation: Long, positionMs: Long, durationMs: Long) {
        updateForGeneration(generation) { prevSnapshot ->
            val canUpdateProgress = when (prevSnapshot.status) {
                PlaybackStatus.BUFFERING,
                PlaybackStatus.PLAYING,
                PlaybackStatus.PAUSED -> true
                PlaybackStatus.IDLE,
                PlaybackStatus.ENDED,
                PlaybackStatus.ERROR -> false
            }
            if (!canUpdateProgress) return@updateForGeneration prevSnapshot
            val resolvedDuration = resolveDuration(durationMs, prevSnapshot.durationMs)
            val clampedPosition = StreamSourceValidator.clampSeekPosition(positionMs, resolvedDuration)
            prevSnapshot.copy(
                positionMs = clampedPosition,
                durationMs = resolvedDuration,
            )
        }
    }

    /**
     * Persists normalized [volume] inside [snapshot] across track switches and stops.
     * Ignores updates only after [onReleased] has terminated the engine.
     */
    fun onVolumeChanged(volume: Float) {
        val clamped = StreamSourceValidator.clampVolume(volume)
        internalState.updateAndGet { prev ->
            if (prev.released) return@updateAndGet prev
            prev.copy(snapshot = prev.snapshot.copy(volume = clamped))
        }
    }

    /**
     * Transitions [snapshot] to [PlaybackStatus.ENDED] when active [generation] finishes naturally.
     * Leaves [EngineSnapshot.status] at `ENDED` so queue controllers can trigger the next track.
     */
    fun onCompleted(generation: Long) {
        updateForGeneration(generation) { prevSnapshot ->
            prevSnapshot.copy(
                status = PlaybackStatus.ENDED,
                isPlaying = false,
                isBuffering = false,
                positionMs = prevSnapshot.durationMs,
                errorMessage = null,
            )
        }
    }

    /**
     * Resets [snapshot] to [PlaybackStatus.IDLE] and invalidates pending callbacks by advancing generation.
     * Preserves the user's configured volume level.
     */
    fun onStopped() {
        internalState.updateAndGet { prev ->
            if (prev.released) return@updateAndGet prev
            prev.copy(
                generation = prev.generation + 1L,
                snapshot = EngineSnapshot(volume = prev.snapshot.volume),
            )
        }
    }

    /**
     * Transitions [snapshot] to [PlaybackStatus.ERROR] with [error] message if [generation] matches or is null.
     * Advances generation on unattributed errors so stale callbacks from prior playback are ignored.
     */
    fun onError(generation: Long?, error: DomainError) {
        internalState.updateAndGet { prev ->
            if (prev.released) return@updateAndGet prev
            if (generation != null && prev.generation != generation) return@updateAndGet prev
            val nextGeneration = if (generation == null) prev.generation + 1L else prev.generation
            prev.copy(
                generation = nextGeneration,
                snapshot = prev.snapshot.copy(
                    status = PlaybackStatus.ERROR,
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = error.message,
                ),
            )
        }
    }

    /**
     * Permanently marks this machine released, advances generation, and resets [snapshot] to [PlaybackStatus.IDLE].
     * Ensures no subsequent track starts or async callbacks mutate [snapshot] after teardown.
     */
    fun onReleased() {
        internalState.updateAndGet { prev ->
            prev.copy(
                generation = prev.generation + 1L,
                released = true,
                snapshot = EngineSnapshot(volume = prev.snapshot.volume),
            )
        }
    }

    /**
     * Applies [transform] to [EngineSnapshot] only when [generation] matches active state and engine is not released.
     * Keeps all generation-scoped transitions atomic via [MutableStateFlow.updateAndGet].
     */
    private inline fun updateForGeneration(
        generation: Long,
        crossinline transform: (EngineSnapshot) -> EngineSnapshot,
    ) {
        internalState.updateAndGet { prev ->
            if (prev.released || prev.generation != generation) return@updateAndGet prev
            prev.copy(snapshot = transform(prev.snapshot))
        }
    }

    /**
     * Prefers positive [reportedDurationMs] over [existingDurationMs] when native media metadata loads.
     * Prevents zero/unset backend durations from wiping out track metadata estimates.
     */
    private fun resolveDuration(reportedDurationMs: Long, existingDurationMs: Long): Long {
        return if (reportedDurationMs > 0L) reportedDurationMs else existingDurationMs
    }
}
