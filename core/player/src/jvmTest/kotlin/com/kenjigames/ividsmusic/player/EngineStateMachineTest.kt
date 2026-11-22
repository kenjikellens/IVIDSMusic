package com.kenjigames.ividsmusic.player

import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.EngineSnapshot
import com.kenjigames.ividsmusic.domain.model.PlaybackStatus
import com.kenjigames.ividsmusic.domain.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies state transitions, generation-token isolation, and release lockouts in [EngineStateMachine].
 * Tests run purely on JVM without native audio backends.
 */
class EngineStateMachineTest {

    private val sampleTrack = Track(
        id = "track-1",
        title = "Ocean Pulse",
        artist = "Kenji",
        album = "Synth Waves",
        durationSeconds = 180,
    )

    /**
     * Verifies default [EngineStateMachine.snapshot] begins idle at position zero and full volume.
     * Ensures initial state matches domain contract defaults before any track loads.
     */
    @Test
    fun initialSnapshotStartsIdle() {
        val machine = EngineStateMachine()
        assertEquals(EngineSnapshot(), machine.snapshot.value)
        assertFalse(machine.isReleased())
    }

    /**
     * Verifies happy-path transitions across buffering, ready, playing, progress polling, paused, and ended.
     * Ensures duration estimates from [Track.durationSeconds] update cleanly once native duration loads.
     */
    @Test
    fun playbackLifecycleTransitionsThroughEnded() {
        val machine = EngineStateMachine()
        val gen = machine.beginTrack(sampleTrack)
        assertNotNull(gen)

        assertEquals(PlaybackStatus.BUFFERING, machine.snapshot.value.status)
        assertTrue(machine.snapshot.value.isBuffering)
        assertEquals(180_000L, machine.snapshot.value.durationMs)

        machine.onReady(gen, 185_500L)
        assertFalse(machine.snapshot.value.isBuffering)
        assertEquals(185_500L, machine.snapshot.value.durationMs)

        machine.onPlaying(gen, 185_500L)
        assertEquals(PlaybackStatus.PLAYING, machine.snapshot.value.status)
        assertTrue(machine.snapshot.value.isPlaying)

        machine.onProgress(gen, 42_000L, 185_500L)
        assertEquals(42_000L, machine.snapshot.value.positionMs)

        machine.onPaused(gen)
        assertEquals(PlaybackStatus.PAUSED, machine.snapshot.value.status)
        assertFalse(machine.snapshot.value.isPlaying)

        machine.onCompleted(gen)
        assertEquals(PlaybackStatus.ENDED, machine.snapshot.value.status)
        assertFalse(machine.snapshot.value.isPlaying)
        assertEquals(185_500L, machine.snapshot.value.positionMs)
    }

    /**
     * Verifies stale async callbacks from an earlier generation cannot mutate a newer active track.
     * Protects against out-of-order end-of-media, progress, or error events during rapid skips.
     */
    @Test
    fun staleGenerationCallbacksAreIgnored() {
        val machine = EngineStateMachine()
        val gen1 = requireNotNull(machine.beginTrack(sampleTrack))
        val gen2 = requireNotNull(machine.beginTrack(sampleTrack.copy(id = "track-2", durationSeconds = 90)))

        machine.onPlaying(gen2, 90_000L)
        machine.onProgress(gen1, 75_000L, 180_000L)
        machine.onError(gen1, DomainError.Network("Stale error"))
        machine.onCompleted(gen1)

        val current = machine.snapshot.value
        assertEquals(PlaybackStatus.PLAYING, current.status)
        assertTrue(current.isPlaying)
        assertEquals(0L, current.positionMs)
        assertEquals(90_000L, current.durationMs)
        assertNull(current.errorMessage)
    }

    /**
     * Verifies volume clamping persists across [EngineStateMachine.onStopped] and [EngineStateMachine.onError].
     * Ensures error transitions clear playing/buffering flags and expose the error message.
     */
    @Test
    fun volumePersistsAcrossStopAndError() {
        val machine = EngineStateMachine()
        machine.onVolumeChanged(0.45f)
        val gen = requireNotNull(machine.beginTrack(sampleTrack))
        machine.onPlaying(gen, 180_000L)
        machine.onError(gen, DomainError.StreamNotFound("Stream missing"))

        assertEquals(PlaybackStatus.ERROR, machine.snapshot.value.status)
        assertEquals("Stream missing", machine.snapshot.value.errorMessage)
        assertEquals(0.45f, machine.snapshot.value.volume)

        machine.onStopped()
        assertEquals(PlaybackStatus.IDLE, machine.snapshot.value.status)
        assertNull(machine.snapshot.value.errorMessage)
        assertEquals(0.45f, machine.snapshot.value.volume)
    }

    /**
     * Verifies [EngineStateMachine.onReleased] permanently rejects further track starts and state updates.
     * Prevents zombie callbacks from reviving an already released engine.
     */
    @Test
    fun releasedMachineRejectsSubsequentMutations() {
        val machine = EngineStateMachine()
        val gen = requireNotNull(machine.beginTrack(sampleTrack))
        machine.onPlaying(gen, 180_000L)

        machine.onReleased()
        assertTrue(machine.isReleased())
        assertEquals(PlaybackStatus.IDLE, machine.snapshot.value.status)
        assertNull(machine.beginTrack(sampleTrack))

        machine.onPlaying(gen, 180_000L)
        assertEquals(PlaybackStatus.IDLE, machine.snapshot.value.status)
    }
}
