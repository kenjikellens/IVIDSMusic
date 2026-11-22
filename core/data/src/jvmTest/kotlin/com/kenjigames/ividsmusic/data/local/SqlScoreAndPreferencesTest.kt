package com.kenjigames.ividsmusic.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kenjigames.ividsmusic.data.db.IvidsDatabase
import com.kenjigames.ividsmusic.data.di.initializeOrMigrateJdbcDatabase
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.InterestScore
import com.kenjigames.ividsmusic.domain.model.LayoutMode
import com.kenjigames.ividsmusic.domain.model.ScoreEntityType
import com.kenjigames.ividsmusic.domain.model.UpdateMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import java.io.File
import java.nio.file.Files
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests verifying SQLDelight score clamping (`0..100`) and DataStore preferences persistence.
 */
class SqlScoreAndPreferencesTest {

    private lateinit var tempDir: File
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var scoreRepository: SqlScoreRepository
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var preferencesRepository: DataStorePreferencesRepository

    /** Creates temporary SQLite and DataStore storage fixtures before each test. */
    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("ivids_prefs_test").toFile()
        driver = JdbcSqliteDriver(
            JdbcSqliteDriver.IN_MEMORY,
            Properties().apply { put("foreign_keys", "true") }
        )
        initializeOrMigrateJdbcDatabase(driver)
        scoreRepository = SqlScoreRepository(IvidsDatabase(driver))

        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.createWithPath(
            scope = dataStoreScope,
            produceFile = { File(tempDir, "test_prefs.preferences_pb").absolutePath.toPath() }
        )
        preferencesRepository = DataStorePreferencesRepository(dataStore)
    }

    /** Closes SQLite and DataStore scopes and removes temporary directories after each test. */
    @AfterTest
    fun tearDown() {
        dataStoreScope.cancel()
        driver.close()
        tempDir.deleteRecursively()
    }

    /** Confirms [SqlScoreRepository] clamps values cleanly within 0..100 and orders highest scores first. */
    @Test
    fun scoreRepositoryClampsBoundsAndOrdersByScore() = runTest {
        scoreRepository.upsertScore(
            InterestScore("track:1", ScoreEntityType.TRACK, "1", "Low", score = -25, lastUpdatedMs = 100L)
        )
        scoreRepository.upsertScore(
            InterestScore("artist:2", ScoreEntityType.ARTIST, "2", "High", score = 180, lastUpdatedMs = 200L)
        )

        val scores = scoreRepository.getAllScores()
        assertEquals(2, scores.size)
        assertEquals(100, scores[0].score)
        assertEquals(0, scores[1].score)
    }

    /** Confirms DataStore settings clamp UI scale (75%-150%) and persist all preference fields & profile stats. */
    @Test
    fun dataStorePersistsAllPreferencesAndClampsUiScale() = runTest {
        val initial = preferencesRepository.preferences.first()
        assertEquals(AudioQuality.Q196, initial.downloadQuality)
        assertEquals(1.0f, initial.uiScale)
        assertEquals(LayoutMode.AUTO, initial.layoutMode)

        preferencesRepository.setDownloadQuality(AudioQuality.Q320)
        preferencesRepository.setUiScale(2.4f)
        preferencesRepository.setLanguageCode("nl")
        preferencesRepository.setDynamicTintEnabled(false)
        preferencesRepository.setAutoUpdateEnabled(false)
        preferencesRepository.setLayoutMode(LayoutMode.TV)

        val updated = preferencesRepository.preferences.first()
        assertEquals(AudioQuality.Q320, updated.downloadQuality)
        assertEquals(1.50f, updated.uiScale)
        assertEquals("nl", updated.languageCode)
        assertFalse(updated.dynamicTintEnabled)
        assertFalse(updated.autoUpdateEnabled)
        assertEquals(UpdateMode.MANUAL, updated.updateMode)
        assertEquals(LayoutMode.TV, updated.layoutMode)

        preferencesRepository.setUiScale(0.3f)
        assertEquals(0.75f, preferencesRepository.preferences.first().uiScale)

        assertTrue(preferencesRepository.updateProfile("Kenji", "Hi-Fi audio fan") is AppResult.Success)
        preferencesRepository.addListeningSeconds(125L)
        val profile = preferencesRepository.profile.first()
        assertEquals("Kenji", profile.displayName)
        assertEquals("Hi-Fi audio fan", profile.bio)
        assertEquals(125L, profile.totalListeningSeconds)
    }
}
