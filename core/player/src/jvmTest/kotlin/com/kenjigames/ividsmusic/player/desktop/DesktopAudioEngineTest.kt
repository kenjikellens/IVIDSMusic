package com.kenjigames.ividsmusic.player.desktop

import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.AudioEngineCommand
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.PlaybackStatus
import com.kenjigames.ividsmusic.domain.model.StreamSource
import com.kenjigames.ividsmusic.domain.model.Track
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.io.File
import java.net.UnknownHostException
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies [DesktopAudioEngine] state machine transitions, error mapping, polling, and deterministic disposal.
 * Uses an in-memory [FakeDesktopMediaBackend] paired with [StandardTestDispatcher] for zero-flakiness timing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DesktopAudioEngineTest {

    private val trackOne = Track(
        id = "t1",
        title = "Neon Harbor",
        artist = "Wave",
        durationSeconds = 100,
    )

    private val trackTwo = Track(
        id = "t2",
        title = "Cyan Horizon",
        artist = "Pulse",
        durationSeconds = 200,
    )

    /**
     * Verifies full track playback lifecycle including 250ms position polling and end-of-media transition.
     * Confirms `AppResult.Success` is returned and `PlaybackStatus.ENDED` triggers cleanly.
     */
    @Test
    fun playPollsPositionAndCompletesToEnded() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val backend = FakeDesktopMediaBackend()
        val engine = DesktopAudioEngine(backend = backend, pollingDispatcher = dispatcher)

        val result = engine.play(
            source = StreamSource.RemoteUrl("https://cdn.example.com/track1.m4a"),
            track = trackOne,
        )
        assertIs<AppResult.Success<Unit>>(result)
        assertEquals(PlaybackStatus.BUFFERING, engine.snapshot.value.status)

        val handle = backend.handles.single()
        handle.callbacks.onPlaying(100_000L)
        assertEquals(PlaybackStatus.PLAYING, engine.snapshot.value.status)

        handle.positionMs = 1_250L
        advanceTimeBy(250L)
        runCurrent()
        assertEquals(1_250L, engine.snapshot.value.positionMs)

        handle.callbacks.onEndOfMedia()
        assertEquals(PlaybackStatus.ENDED, engine.snapshot.value.status)
        assertEquals(false, engine.snapshot.value.isPlaying)
        assertEquals(100_000L, engine.snapshot.value.positionMs)

        engine.release()
    }

    /**
     * Verifies switching tracks deterministically stops and disposes the previous handle before starting the next.
     * Also ensures stale end-of-media or error callbacks from the disposed handle are ignored.
     */
    @Test
    fun trackSwitchDeterministicallyStopsAndDisposesPreviousHandle() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val backend = FakeDesktopMediaBackend()
        val engine = DesktopAudioEngine(backend = backend, pollingDispatcher = dispatcher)

        engine.play(StreamSource.RemoteUrl("https://cdn.example.com/1.m4a"), trackOne)
        val firstHandle = backend.handles[0]
        firstHandle.callbacks.onPlaying(100_000L)

        engine.play(StreamSource.RemoteUrl("https://cdn.example.com/2.m4a"), trackTwo)
        val secondHandle = backend.handles[1]

        assertEquals(listOf("stop", "dispose"), firstHandle.lifecycleEvents)
        assertTrue(backend.creationOrderRecordedAfter(firstHandle))

        // Late callbacks from stale handle1 must not alter track2 state.
        firstHandle.callbacks.onError(UnknownHostException("stale host failure"))
        firstHandle.callbacks.onEndOfMedia()

        secondHandle.callbacks.onPlaying(200_000L)
        assertEquals(PlaybackStatus.PLAYING, engine.snapshot.value.status)
        assertNull(engine.snapshot.value.errorMessage)

        engine.release()
    }

    /**
     * Verifies `dispose()` executes unconditionally in `finally` even if `handle.stop()` throws an exception.
     * Confirms subsequent `play()` calls fail with [DomainError.Cancelled] once released.
     */
    @Test
    fun releaseAlwaysDisposesEvenWhenStopThrows() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val backend = FakeDesktopMediaBackend()
        val engine = DesktopAudioEngine(backend = backend, pollingDispatcher = dispatcher)

        engine.play(StreamSource.RemoteUrl("https://cdn.example.com/1.m4a"), trackOne)
        val handle = backend.handles.single()
        handle.throwOnStop = true

        engine.release()

        assertEquals(listOf("stop-threw", "dispose"), handle.lifecycleEvents)
        val afterRelease = engine.play(StreamSource.RemoteUrl("https://cdn.example.com/2.m4a"), trackTwo)
        val failure = assertIs<AppResult.Failure>(afterRelease)
        assertIs<DomainError.Cancelled>(failure.error)
    }

    /**
     * Verifies volume clamping persists across tracks and seek positions clamp within known track bounds.
     * Ensures immediate scrubber position updates occur before waiting for the next polling tick.
     */
    @Test
    fun volumeAndSeekClampingEnforcedAcrossTracks() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val backend = FakeDesktopMediaBackend()
        val engine = DesktopAudioEngine(backend = backend, pollingDispatcher = dispatcher)

        engine.setVolume(1.75f)
        assertEquals(1.0f, engine.snapshot.value.volume)
        engine.setVolume(-0.25f)
        assertEquals(0.0f, engine.snapshot.value.volume)
        engine.setVolume(0.4f)

        engine.play(StreamSource.RemoteUrl("https://cdn.example.com/1.m4a"), trackOne)
        val handle = backend.handles.single()
        assertEquals(0.4f, handle.initialVolume)

        handle.callbacks.onPlaying(100_000L)
        engine.seekTo(-200L)
        assertEquals(0L, handle.lastSeekMs)
        assertEquals(0L, engine.snapshot.value.positionMs)

        engine.seekTo(250_000L)
        assertEquals(100_000L, handle.lastSeekMs)
        assertEquals(100_000L, engine.snapshot.value.positionMs)

        engine.stop()
        assertEquals(listOf("stop", "dispose"), handle.lifecycleEvents)
        assertEquals(PlaybackStatus.IDLE, engine.snapshot.value.status)
        assertEquals(0.4f, engine.snapshot.value.volume)

        engine.release()
    }

    /**
     * Verifies existing local files convert cleanly to `file:` URIs while missing files fail without backend calls.
     * Confirms spaces in local file paths are properly URI-encoded for JavaFX `Media`.
     */
    @Test
    fun localFilesValidateExistenceAndEncodeSpaces() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val backend = FakeDesktopMediaBackend()
        val engine = DesktopAudioEngine(backend = backend, pollingDispatcher = dispatcher)

        val missingResult = engine.play(
            source = StreamSource.LocalFile("C:\\nonexistent_ivids_dir\\missing track.m4a", AudioQuality.Q196),
            track = trackOne,
        )
        val missingFailure = assertIs<AppResult.Failure>(missingResult)
        assertIs<DomainError.StreamNotFound>(missingFailure.error)
        assertEquals(0, backend.handles.size)
        assertEquals(PlaybackStatus.ERROR, engine.snapshot.value.status)

        val tempDir = createTempDirectory("ivids-player-test").toFile()
        try {
            val audioFile = File(tempDir, "my favorite track.m4a").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
            val okResult = engine.play(
                source = StreamSource.LocalFile(audioFile.absolutePath, AudioQuality.Q320),
                track = trackOne,
            )
            assertIs<AppResult.Success<Unit>>(okResult)
            val created = backend.handles.single()
            assertTrue(created.mediaUri.startsWith("file:/"))
            assertTrue(created.mediaUri.contains("%20"))
        } finally {
            engine.release()
            tempDir.deleteRecursively()
        }
    }

    /**
     * Verifies backend initialization, rate-limit, HTTP 404, and network errors map to expected [DomainError] types.
     * Ensures [EngineSnapshot.status] moves to [PlaybackStatus.ERROR] with descriptive messages.
     */
    @Test
    fun errorsMapToCanonicalDomainErrors() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val failingBackend = FakeDesktopMediaBackend(
            createFailure = UnsatisfiedLinkError("Headless JavaFX toolkit unavailable"),
        )
        val failingEngine = DesktopAudioEngine(backend = failingBackend, pollingDispatcher = dispatcher)
        val initResult = failingEngine.play(StreamSource.RemoteUrl("https://cdn.example.com/a.m4a"), trackOne)
        val initFailure = assertIs<AppResult.Failure>(initResult)
        assertIs<DomainError.Unknown>(initFailure.error)
        assertEquals(PlaybackStatus.ERROR, failingEngine.snapshot.value.status)
        failingEngine.release()

        val rateLimitError = DesktopErrorMapper.map(RuntimeException("HTTP 429 Too Many Requests"), "t1", false)
        assertEquals(429, assertIs<DomainError.Network>(rateLimitError).statusCode)

        val notFoundError = DesktopErrorMapper.map(RuntimeException("HTTP 404 Not Found"), "t1", false)
        assertIs<DomainError.StreamNotFound>(notFoundError)

        val networkError = DesktopErrorMapper.map(UnknownHostException("offline.example"), "t1", false)
        assertIs<DomainError.Network>(networkError)
    }

    /**
     * Verifies [DesktopAudioEngine.externalCommands] emits [AudioEngineCommand] events to collectors.
     * Ensures external media key transport wiring conforms to [AudioEngine.externalCommands].
     */
    @Test
    fun externalCommandsEmitToObservers() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val engine = DesktopAudioEngine(backend = FakeDesktopMediaBackend(), pollingDispatcher = dispatcher)

        val deferred = async { engine.externalCommands.first() }
        runCurrent()
        assertTrue(engine.emitExternalCommand(AudioEngineCommand.NEXT))
        assertEquals(AudioEngineCommand.NEXT, deferred.await())

        engine.release()
    }

    /**
     * In-memory [DesktopMediaBackend] recording handle creation and lifecycle call ordering for assertions.
     * Supports injecting startup failures to simulate headless or broken runtime environments.
     */
    private class FakeDesktopMediaBackend(
        private val createFailure: Throwable? = null,
    ) : DesktopMediaBackend {

        val handles = mutableListOf<FakeDesktopMediaHandle>()
        private val events = mutableListOf<String>()

        /**
         * Records creation and returns a [FakeDesktopMediaHandle] or [createFailure] when configured.
         * Logs handle creation ordering to verify prior handles stopped and disposed first.
         */
        override suspend fun createAndStart(
            mediaUri: String,
            headers: Map<String, String>,
            initialVolume: Float,
            callbacks: MediaHandleCallbacks,
            cacheKeyHint: String?,
        ): Result<DesktopMediaHandle> {
            createFailure?.let { return Result.failure(it) }
            val index = handles.size
            events += "create-$index"
            val handle = FakeDesktopMediaHandle(
                index = index,
                mediaUri = mediaUri,
                initialVolume = initialVolume,
                callbacks = callbacks,
                globalEvents = events,
            )
            handles += handle
            return Result.success(handle)
        }

        /**
         * Verifies that [priorHandle] finished disposing before the following handle was created.
         * Used by rapid track-switching tests to assert leak-free sequencing.
         */
        fun creationOrderRecordedAfter(priorHandle: FakeDesktopMediaHandle): Boolean {
            val disposeIndex = events.indexOf("dispose-${priorHandle.index}")
            val nextCreateIndex = events.indexOf("create-${priorHandle.index + 1}")
            return disposeIndex >= 0 && nextCreateIndex > disposeIndex
        }
    }

    /**
     * Recorded fake [DesktopMediaHandle] tracking stop/dispose ordering, volume, and seek state.
     * Allows tests to simulate `stop()` exceptions and mutate playback positions deterministically.
     */
    private class FakeDesktopMediaHandle(
        val index: Int,
        val mediaUri: String,
        val initialVolume: Float,
        val callbacks: MediaHandleCallbacks,
        private val globalEvents: MutableList<String>,
    ) : DesktopMediaHandle {

        val lifecycleEvents = mutableListOf<String>()
        var throwOnStop: Boolean = false
        var positionMs: Long = 0L
        var durationMsValue: Long = 100_000L
        var lastSeekMs: Long? = null
        var currentVolume: Float = initialVolume

        /** Records play invocation without external side effects. */
        override fun play() {
            lifecycleEvents += "play"
        }

        /** Records pause invocation without external side effects. */
        override fun pause() {
            lifecycleEvents += "pause"
        }

        /** Updates [lastSeekMs] and simulated [positionMs] for polling assertions. */
        override fun seekTo(positionMs: Long) {
            lastSeekMs = positionMs
            this.positionMs = positionMs
        }

        /** Updates [currentVolume] for volume persistence assertions. */
        override fun setVolume(volume: Float) {
            currentVolume = volume
        }

        /** Records stop invocation or throws when [throwOnStop] is enabled to test `finally` safety. */
        override fun stop() {
            if (throwOnStop) {
                lifecycleEvents += "stop-threw"
                throw IllegalStateException("Simulated stop failure")
            }
            lifecycleEvents += "stop"
        }

        /** Records disposal invocation into both per-handle and global lifecycle event logs. */
        override fun dispose() {
            lifecycleEvents += "dispose"
            globalEvents += "dispose-$index"
        }

        /** Returns simulated playback position in milliseconds. */
        override fun currentPositionMs(): Long = positionMs

        /** Returns simulated track duration in milliseconds. */
        override fun durationMs(): Long = durationMsValue
    }
}
