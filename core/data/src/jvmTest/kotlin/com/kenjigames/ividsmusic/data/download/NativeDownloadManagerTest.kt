package com.kenjigames.ividsmusic.data.download

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kenjigames.ividsmusic.data.db.IvidsDatabase
import com.kenjigames.ividsmusic.data.di.initializeOrMigrateJdbcDatabase
import com.kenjigames.ividsmusic.data.local.ActiveSaveDirectoryHolder
import com.kenjigames.ividsmusic.data.local.AppStorageDirectories
import com.kenjigames.ividsmusic.data.local.DataStorePreferencesRepository
import com.kenjigames.ividsmusic.data.local.SqlLibraryRepository
import com.kenjigames.ividsmusic.data.stream.InMemoryStreamTtlCache
import com.kenjigames.ividsmusic.data.stream.YouTubeMetadataPreviewer
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.DownloadState
import com.kenjigames.ividsmusic.domain.model.StreamSource
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.StreamResolver
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okio.Path.Companion.toPath
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Integration tests verifying real OkHttp streaming to `.part`, atomic `.m4a` promotion, missing-ffmpeg fallback,
 * `saved/` startup scanning, storage usage metrics, custom folder switching, and cache clearing.
 */
class NativeDownloadManagerTest {

    private lateinit var rootDir: File
    private lateinit var cacheDir: File
    private lateinit var filesDir: File
    private lateinit var savedDir: File
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var libraryRepository: SqlLibraryRepository
    private lateinit var preferencesRepository: DataStorePreferencesRepository
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var httpServer: HttpServer
    private var serverPort: Int = 0
    private val payloadBytes = ByteArray(128 * 1024) { (it % 127).toByte() }

    /** Starts an embedded local HTTP audio stream server and initializes SQLite/DataStore fixtures. */
    @BeforeTest
    fun setup() {
        rootDir = Files.createTempDirectory("ivids_dl_test").toFile()
        cacheDir = File(rootDir, "cache").apply { mkdirs() }
        filesDir = File(rootDir, "files").apply { mkdirs() }
        savedDir = File(filesDir, "saved").apply { mkdirs() }

        driver = JdbcSqliteDriver(
            JdbcSqliteDriver.IN_MEMORY,
            Properties().apply { put("foreign_keys", "true") }
        )
        initializeOrMigrateJdbcDatabase(driver)
        libraryRepository = SqlLibraryRepository(IvidsDatabase(driver))

        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.createWithPath(
            scope = dataStoreScope,
            produceFile = { File(filesDir, "dl_prefs.preferences_pb").absolutePath.toPath() }
        )
        preferencesRepository = DataStorePreferencesRepository(dataStore)

        httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/audio.m4a") { exchange ->
                exchange.responseHeaders.add("Content-Type", "audio/mp4")
                exchange.sendResponseHeaders(200, payloadBytes.size.toLong())
                exchange.responseBody.use { out ->
                    out.write(payloadBytes)
                }
            }
            start()
        }
        serverPort = httpServer.address.port
    }

    /** Stops the embedded HTTP server and deletes temporary test directories. */
    @AfterTest
    fun tearDown() {
        httpServer.stop(0)
        dataStoreScope.cancel()
        driver.close()
        rootDir.deleteRecursively()
    }

    /** Verifies real streaming download writes `.part` then atomically renames to `.m4a` when ffmpeg is absent. */
    @Test
    fun enqueueStreamsToPartAtomicallyPromotesAndGracefullySkipsAbsentFfmpeg() = runBlocking {
        val dirs = AppStorageDirectories(
            cacheDir = cacheDir.absolutePath,
            filesDir = filesDir.absolutePath,
            savedDir = savedDir.absolutePath
        )
        val stubResolver = object : StreamResolver {
            override suspend fun resolve(track: Track, preferredQuality: AudioQuality): AppResult<StreamSource> =
                AppResult.Success(
                    StreamSource.RemoteUrl(
                        url = "http://127.0.0.1:$serverPort/audio.m4a",
                        mimeType = "audio/mp4",
                        quality = AudioQuality.Q128
                    )
                )

            override suspend fun resolveUrl(videoUrlOrId: String, preferredQuality: AudioQuality): AppResult<StreamSource> =
                resolve(Track(id = "url", title = "Url", artist = "YouTube"), preferredQuality)

            override fun invalidate(trackId: String) = Unit
        }
        val stubPreviewer = object : YouTubeMetadataPreviewer {
            override suspend fun previewUrl(youtubeUrl: String): AppResult<List<Track>> =
                AppResult.Success(listOf(Track(id = "yt_abc", title = "Preview Track", artist = "YT")))
        }
        val absentTranscoder = object : AudioTranscoder {
            override fun isAvailable(): Boolean = false
            override suspend fun transcodeIfNeeded(
                sourcePartFile: File,
                sourceExtension: String,
                sourceQuality: AudioQuality,
                targetQuality: AudioQuality
            ): TranscodeOutcome? = null
        }

        val manager = NativeDownloadManager(
            directories = dirs,
            saveDirectoryHolder = ActiveSaveDirectoryHolder(dirs.savedDir),
            streamResolver = stubResolver,
            metadataPreviewer = stubPreviewer,
            libraryRepository = libraryRepository,
            preferencesRepository = preferencesRepository,
            streamTtlCache = InMemoryStreamTtlCache(),
            okHttpClient = OkHttpClient(),
            audioTranscoder = absentTranscoder
        )

        val track = Track(id = "dl_1", title = "Cyan Waves", artist = "Kenji")
        val observedStates = mutableListOf<DownloadState>()
        val collectorJob = launch {
            manager.downloads.collect { list ->
                list.firstOrNull()?.state?.let { observedStates.add(it) }
            }
        }
        val enqueueRes = manager.enqueue(track, AudioQuality.Q320)
        val downloadId = (enqueueRes as AppResult.Success).data

        val completedItem = withTimeout(5_000L) {
            manager.downloads.first { items ->
                val state = items.firstOrNull { it.id == downloadId }?.state
                state is DownloadState.Completed || state is DownloadState.Failed
            }.first { it.id == downloadId }
        }
        collectorJob.cancel()

        val finalState = assertIs<DownloadState.Completed>(completedItem.state)
        assertTrue(observedStates.any { it is DownloadState.Downloading && it.progress >= 0f })
        assertEquals(AudioQuality.Q128, finalState.quality)
        val outFile = File(finalState.filePath)
        assertTrue(outFile.exists())
        assertEquals(payloadBytes.size.toLong(), outFile.length())
        assertTrue(outFile.name.endsWith(".m4a"))
        assertFalse(File(savedDir, "Kenji - Cyan Waves.part").exists())

        val persisted = libraryRepository.getTrack("dl_1")!!
        assertTrue(persisted.isDownloaded)
        assertEquals(128, persisted.bitrateKbps)
        assertEquals(payloadBytes.size.toLong(), manager.getSavedStorageBytes())
    }

    /** Verifies startup scan registers pre-existing `.mp3`/`.m4a` files, prunes deleted files, and switches custom dirs. */
    @Test
    fun startupScanRegistersSavedAudioFilesAndSwitchesCustomDirectory() = runTest {
        File(savedDir, "Daft Punk - Voyager.mp3").writeBytes(ByteArray(2048) { 1 })
        File(savedDir, "Porter Robinson - Nurture.m4a").writeBytes(ByteArray(4096) { 2 })
        File(savedDir, "incomplete.part").writeBytes(ByteArray(1024) { 3 })

        val dirs = AppStorageDirectories(cacheDir.absolutePath, filesDir.absolutePath, savedDir.absolutePath)
        val ttlCache = InMemoryStreamTtlCache()
        ttlCache.put("track:x:196", "x", StreamSource.RemoteUrl("https://cdn.example/x.m4a"))
        File(cacheDir, "cached_chunk.tmp").writeText("stale-cache")

        val manager = NativeDownloadManager(
            directories = dirs,
            saveDirectoryHolder = ActiveSaveDirectoryHolder(dirs.savedDir),
            streamResolver = object : StreamResolver {
                override suspend fun resolve(track: Track, preferredQuality: AudioQuality) =
                    AppResult.Success(StreamSource.LocalFile(""))
                override suspend fun resolveUrl(videoUrlOrId: String, preferredQuality: AudioQuality) =
                    AppResult.Success(StreamSource.LocalFile(""))
                override fun invalidate(trackId: String) = Unit
            },
            metadataPreviewer = object : YouTubeMetadataPreviewer {
                override suspend fun previewUrl(youtubeUrl: String): AppResult<List<Track>> = AppResult.Success(emptyList())
            },
            libraryRepository = libraryRepository,
            preferencesRepository = preferencesRepository,
            streamTtlCache = ttlCache,
            okHttpClient = OkHttpClient(),
            audioTranscoder = object : AudioTranscoder {
                override fun isAvailable() = false
                override suspend fun transcodeIfNeeded(
                    sourcePartFile: File,
                    sourceExtension: String,
                    sourceQuality: AudioQuality,
                    targetQuality: AudioQuality
                ): TranscodeOutcome? = null
            }
        )

        val scanned = (manager.scanSavedDirectory() as AppResult.Success).data
        assertEquals(2, scanned.size)
        assertTrue(scanned.any { it.artist == "Daft Punk" && it.title == "Voyager" })
        assertTrue(scanned.any { it.artist == "Porter Robinson" && it.title == "Nurture" })
        assertEquals(6144L, manager.getSavedStorageBytes())

        // Delete one file externally and re-scan -> SQLite prunes stale row
        File(savedDir, "Daft Punk - Voyager.mp3").delete()
        val afterDelete = (manager.scanSavedDirectory() as AppResult.Success).data
        assertEquals(1, afterDelete.size)
        assertEquals("Nurture", afterDelete.single().title)

        // Custom save directory switch + cache clearing
        val customDir = File(rootDir, "custom_music").apply { mkdirs() }
        File(customDir, "Custom Artist - Custom Song.m4a").writeBytes(ByteArray(1024) { 7 })
        assertTrue(manager.setCustomSaveDirectory(customDir.absolutePath) is AppResult.Success)
        assertEquals(customDir.absolutePath, manager.getSavedDirectoryPath())
        assertTrue(libraryRepository.observeDownloadedTracks().first().any { it.title == "Custom Song" })

        val initialCacheBytes = manager.getAudioCacheBytes()
        val nestedCache = File(cacheDir, "nested").apply { mkdirs() }
        File(nestedCache, "chunk.bin").writeBytes(ByteArray(2048) { 3 })
        assertEquals(initialCacheBytes + 2048L, manager.getAudioCacheBytes())

        assertTrue(manager.clearAudioCache() is AppResult.Success)
        assertEquals(0, cacheDir.listFiles()?.size ?: 0)
        assertEquals(0L, manager.getAudioCacheBytes())
    }
}
