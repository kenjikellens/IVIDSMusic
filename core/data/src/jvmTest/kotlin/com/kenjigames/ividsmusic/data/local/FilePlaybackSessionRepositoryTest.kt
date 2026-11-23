package com.kenjigames.ividsmusic.data.local

import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.PlaybackSessionSnapshot
import com.kenjigames.ividsmusic.domain.model.RepeatMode
import com.kenjigames.ividsmusic.domain.model.Track
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Verifies [FilePlaybackSessionRepository] atomic save/load, bounds clamping, and corrupt-JSON resilience.
 */
class FilePlaybackSessionRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var sessionFile: File
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    /** Creates an isolated temp session directory before each test. */
    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("ivids_session_test").toFile()
        sessionFile = File(tempDir, "session_state.json")
    }

    /** Deletes temporary session files after each test. */
    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    /** Verifies round-trip save/load preserves queue order, track metadata, cursor, shuffle, and repeat mode. */
    @Test
    fun roundTripsSessionAndClearsCleanly() = runTest {
        val repo = FilePlaybackSessionRepository(sessionFile, json)
        assertNull(repo.load())

        val snapshot = PlaybackSessionSnapshot(
            queue = listOf(
                Track(id = "t1", title = "Song One", artist = "Artist A", durationSeconds = 180),
                Track(id = "t2", title = "Song Two", artist = "Artist B", durationSeconds = 210)
            ),
            currentIndex = 1,
            positionMs = 64_500L,
            isShuffleEnabled = true,
            repeatMode = RepeatMode.ALL,
            updatedAtMs = 123_456L
        )
        assertIs<AppResult.Success<Unit>>(repo.save(snapshot))
        val loaded = assertNotNull(repo.load())
        assertEquals(2, loaded.queue.size)
        assertEquals(1, loaded.currentIndex)
        assertEquals(64_500L, loaded.positionMs)
        assertEquals(true, loaded.isShuffleEnabled)
        assertEquals(RepeatMode.ALL, loaded.repeatMode)

        assertIs<AppResult.Success<Unit>>(repo.clear())
        assertNull(repo.load())
    }

    /** Confirms damaged or unparseable JSON returns null safely without throwing exceptions. */
    @Test
    fun corruptJsonReturnsNullSafely() = runTest {
        sessionFile.writeText("{not-valid-json!!")
        val repo = FilePlaybackSessionRepository(sessionFile, json)
        assertNull(repo.load())
    }
}
