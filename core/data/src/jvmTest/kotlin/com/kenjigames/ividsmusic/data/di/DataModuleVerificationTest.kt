package com.kenjigames.ividsmusic.data.di

import com.kenjigames.ividsmusic.data.local.AppStorageDirectories
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.domain.port.MusicDiscoveryRepository
import com.kenjigames.ividsmusic.domain.port.OfflineDownloadManager
import com.kenjigames.ividsmusic.domain.port.PreferencesRepository
import com.kenjigames.ividsmusic.domain.port.ScoreRepository
import com.kenjigames.ividsmusic.domain.port.StreamResolver
import com.kenjigames.ividsmusic.domain.usecase.PlaybackQueueController
import com.kenjigames.ividsmusic.domain.usecase.RecommendationEngine
import com.kenjigames.ividsmusic.domain.usecase.ResolveTrackStreamUseCase
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Verifies Koin dependency graph wiring across [dataModule] and desktop [platformDataModule].
 */
class DataModuleVerificationTest {

    private var tempDir: File? = null

    /** Tears down Koin context and cleans up temporary test storage folders. */
    @AfterTest
    fun tearDown() {
        stopKoin()
        tempDir?.deleteRecursively()
    }

    /** Confirms every public domain port and use case resolves cleanly from Koin without circular dependencies. */
    @Test
    fun koinGraphResolvesAllDomainPortsAndUseCases() {
        val root = Files.createTempDirectory("ivids_koin_test").toFile().also { tempDir = it }
        val dirs = AppStorageDirectories(
            cacheDir = File(root, "cache").absolutePath,
            filesDir = File(root, "files").absolutePath,
            savedDir = File(root, "saved").absolutePath,
            bundledToolsDir = root.absolutePath
        )

        val koinApp = startKoin {
            modules(dataModule, platformDataModule(dirs))
        }
        val koin = koinApp.koin

        assertNotNull(koin.get<MusicDiscoveryRepository>())
        assertNotNull(koin.get<StreamResolver>())
        assertNotNull(koin.get<LibraryRepository>())
        assertNotNull(koin.get<ScoreRepository>())
        assertNotNull(koin.get<PreferencesRepository>())
        assertNotNull(koin.get<OfflineDownloadManager>())
        assertNotNull(koin.get<PlaybackQueueController>())
        assertNotNull(koin.get<RecommendationEngine>())
        assertNotNull(koin.get<ResolveTrackStreamUseCase>())
    }
}
