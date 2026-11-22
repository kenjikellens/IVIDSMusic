package com.kenjigames.ividsmusic.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kenjigames.ividsmusic.data.db.IvidsDatabase
import com.kenjigames.ividsmusic.data.di.initializeOrMigrateJdbcDatabase
import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests verifying SQLDelight schema, 20-item history cap, cascade deletes, and playlist reordering.
 */
class SqlLibraryRepositoryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: IvidsDatabase
    private lateinit var repository: SqlLibraryRepository
    private var clock = 10_000L

    /** Initializes an in-memory SQLite database with foreign-key enforcement enabled before each test. */
    @BeforeTest
    fun setup() {
        val props = Properties().apply { put("foreign_keys", "true") }
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, props)
        initializeOrMigrateJdbcDatabase(driver)
        database = IvidsDatabase(driver)
        repository = SqlLibraryRepository(database = database, clockMs = { clock++ })
    }

    /** Closes the SQLite driver cleanly after each test case. */
    @AfterTest
    fun tearDown() {
        driver.close()
    }

    /** Verifies listen_history stays capped at 20 tracks newest-first and promotes replayed tracks to the top. */
    @Test
    fun historyEnforcesTwentyTrackCapAtomically() = runTest {
        for (i in 1..25) {
            val res = repository.recordHistory(
                Track(id = "t$i", title = "Song $i", artist = "Artist $i"),
                playedTimestampMs = i * 100L
            )
            assertTrue(res is AppResult.Success)
        }

        val entries = repository.observeHistory().first()
        assertEquals(20, entries.size)
        assertEquals("t25", entries.first().track.id)
        assertEquals("t6", entries.last().track.id)

        // Replaying t10 moves it to index 0 without growing past 20
        repository.recordHistory(Track(id = "t10", title = "Song 10", artist = "Artist 10"), 99_999L)
        val afterReplay = repository.observeHistory().first()
        assertEquals(20, afterReplay.size)
        assertEquals("t10", afterReplay.first().track.id)
    }

    /** Verifies playlist creation, track addition, drag reordering, and ON DELETE CASCADE cleanup. */
    @Test
    fun playlistReorderAndForeignKeyCascadeDelete() = runTest {
        val playlist = (repository.createPlaylist("Roadtrip", "Chill drive") as AppResult.Success).data
        val t1 = Track(id = "1", title = "One", artist = "A")
        val t2 = Track(id = "2", title = "Two", artist = "B")
        val t3 = Track(id = "3", title = "Three", artist = "C")

        repository.addTrackToPlaylist(playlist.id, t1)
        repository.addTrackToPlaylist(playlist.id, t2)
        repository.addTrackToPlaylist(playlist.id, t3)
        // Duplicate track add should be ignored cleanly
        repository.addTrackToPlaylist(playlist.id, t2)

        val beforeReorder = repository.observePlaylist(playlist.id).first()!!
        assertEquals(listOf("1", "2", "3"), beforeReorder.tracks.map { it.id })

        repository.reorderPlaylistTracks(playlist.id, listOf("3", "1", "2"))
        val afterReorder = repository.observePlaylist(playlist.id).first()!!
        assertEquals(listOf("3", "1", "2"), afterReorder.tracks.map { it.id })

        repository.renamePlaylist(playlist.id, "Highway Mix")
        assertEquals("Highway Mix", repository.observePlaylist(playlist.id).first()?.title)

        repository.deletePlaylist(playlist.id)
        assertNull(repository.observePlaylist(playlist.id).first())

        val orphanedLinks = driver.executeQuery(
            identifier = null,
            sql = "SELECT COUNT(*) FROM playlist_tracks WHERE playlist_id = ${playlist.id};",
            mapper = { cursor: SqlCursor ->
                QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L)
            },
            parameters = 0,
            binders = null
        ).value
        assertEquals(0L, orphanedLinks)
    }

    /** Verifies likes, offline bitrate persistence, and saved albums survive metadata updates. */
    @Test
    fun likesDownloadsAndSavedAlbumsPersistFlagsAcrossMetadataUpserts() = runTest {
        val track = Track(id = "tr_1", title = "Deep Blue", artist = "Ocean", genres = listOf("Electronic"))
        repository.setTrackLiked(track, liked = true)
        repository.markTrackDownloaded(track, "/saved/Ocean - Deep Blue.m4a", AudioQuality.Q256)

        // Upserting fresh remote metadata preserves local like & download flags
        repository.upsertTrack(track.copy(coverUrl = "https://img.example/cover.jpg"))

        val stored = repository.getTrack("tr_1")!!
        assertTrue(stored.isLiked)
        assertTrue(stored.isDownloaded)
        assertEquals("/saved/Ocean - Deep Blue.m4a", stored.localFilePath)
        assertEquals(256, stored.bitrateKbps)
        assertEquals("https://img.example/cover.jpg", stored.coverUrl)

        val album = Album(id = "al_1", title = "Horizon", artist = "Ocean", trackCount = 10, releaseYear = 2024)
        repository.setAlbumSaved(album, saved = true)
        assertEquals(1, repository.observeSavedAlbums().first().size)
        repository.setAlbumSaved(album, saved = false)
        assertFalse(repository.observeSavedAlbums().first().any { it.id == "al_1" })
    }
}
