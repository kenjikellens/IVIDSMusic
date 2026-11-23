package com.kenjigames.ividsmusic.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kenjigames.ividsmusic.data.db.IvidsDatabase
import com.kenjigames.ividsmusic.data.download.AudioTranscoder
import com.kenjigames.ividsmusic.data.download.DesktopFfmpegTranscoder
import com.kenjigames.ividsmusic.data.download.NativeDownloadManager
import com.kenjigames.ividsmusic.data.local.AppStorageDirectories
import com.kenjigames.ividsmusic.data.local.FilePlaybackSessionRepository
import com.kenjigames.ividsmusic.data.stream.DesktopNativeToolsManager
import com.kenjigames.ividsmusic.data.stream.JvmLocalAudioLocator
import com.kenjigames.ividsmusic.data.stream.LocalAudioLocator
import com.kenjigames.ividsmusic.data.stream.NewPipeStreamResolver
import com.kenjigames.ividsmusic.data.stream.PlatformStreamFallback
import com.kenjigames.ividsmusic.data.stream.StreamTierResolver
import com.kenjigames.ividsmusic.data.stream.YouTubeMetadataPreviewer
import com.kenjigames.ividsmusic.data.stream.YtDlpDesktopResolver
import com.kenjigames.ividsmusic.domain.port.OfflineDownloadManager
import com.kenjigames.ividsmusic.domain.port.PlaybackSessionRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File
import java.util.Properties
import java.util.concurrent.TimeUnit

/**
 * Desktop JVM implementation of [platformDataModule] wiring `JdbcSqliteDriver`, DataStore, OkHttp,
 * NewPipeExtractor, `yt-dlp.exe` fallback, optional `ffmpeg` transcoder, and [NativeDownloadManager].
 */
actual fun platformDataModule(directories: AppStorageDirectories): Module = module {
    single { directories }

    single<OkHttpClient> {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    single<HttpClient> {
        HttpClient(OkHttp) {
            engine {
                preconfigured = get<OkHttpClient>()
            }
        }
    }

    single<SqlDriver> {
        val filesDir = File(directories.filesDir).apply { mkdirs() }
        val dbFile = File(filesDir, "ivids_music.db")
        val props = Properties().apply { put("foreign_keys", "true") }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}", props)
        initializeOrMigrateJdbcDatabase(driver)
        driver
    }

    single<DataStore<Preferences>> {
        val filesDir = File(directories.filesDir).apply { mkdirs() }
        PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                File(filesDir, PREFERENCES_DATASTORE_FILENAME).absolutePath.toPath()
            }
        )
    }

    single<LocalAudioLocator> { JvmLocalAudioLocator() }
    single { NewPipeStreamResolver(okHttpClient = get()) }
    single<YouTubeMetadataPreviewer> { get<NewPipeStreamResolver>() }

    single {
        val newPipe = get<NewPipeStreamResolver>()
        YtDlpDesktopResolver(
            bundledToolsDir = directories.bundledToolsDir,
            json = get(),
            videoUrlLookup = { track -> newPipe.findMatchingWatchUrl(track) }
        )
    }
    single<PlatformStreamFallback> { get<YtDlpDesktopResolver>() }
    single<StreamTierResolver>(named(PRIMARY_STREAM_EXTRACTOR_QUALIFIER)) {
        val ytDlp = get<YtDlpDesktopResolver>()
        val newPipe = get<NewPipeStreamResolver>()
        if (ytDlp.findYtDlpExecutable() != null) ytDlp else newPipe
    }
    single<AudioTranscoder> { DesktopFfmpegTranscoder(bundledToolsDir = directories.bundledToolsDir) }

    single<PlaybackSessionRepository> {
        val filesDir = File(directories.filesDir).apply { mkdirs() }
        FilePlaybackSessionRepository(sessionFile = File(filesDir, "session_state.json"), json = get())
    }

    single {
        DesktopNativeToolsManager(
            directories = get(),
            preferencesRepository = get(),
            okHttpClient = get(),
            json = get()
        )
    }

    single<OfflineDownloadManager> {
        NativeDownloadManager(
            directories = get(),
            saveDirectoryHolder = get(),
            streamResolver = get(),
            metadataPreviewer = get(),
            libraryRepository = get(),
            preferencesRepository = get(),
            streamTtlCache = get(),
            okHttpClient = get(),
            audioTranscoder = get(),
            desktopPreviewFallback = { url -> get<YtDlpDesktopResolver>().previewPlaylistOrVideo(url) }
        )
    }
}

/**
 * Ensures Desktop SQLite schema creation or migration and enables `PRAGMA foreign_keys = ON` deterministically.
 */
internal fun initializeOrMigrateJdbcDatabase(driver: JdbcSqliteDriver) {
    driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
    val currentVersion = driver.executeQuery(
        identifier = null,
        sql = "PRAGMA user_version;",
        mapper = { cursor: SqlCursor ->
            val value = if (cursor.next().value) cursor.getLong(0) else 0L
            QueryResult.Value(value)
        },
        parameters = 0,
        binders = null
    ).value ?: 0L

    val targetVersion = IvidsDatabase.Schema.version
    if (currentVersion == 0L) {
        IvidsDatabase.Schema.create(driver).value
        driver.execute(null, "PRAGMA user_version = $targetVersion;", 0)
    } else if (currentVersion < targetVersion) {
        IvidsDatabase.Schema.migrate(driver, currentVersion, targetVersion).value
        driver.execute(null, "PRAGMA user_version = $targetVersion;", 0)
    }
}
