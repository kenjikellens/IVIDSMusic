package com.kenjigames.ividsmusic.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.kenjigames.ividsmusic.data.db.IvidsDatabase
import com.kenjigames.ividsmusic.data.download.AndroidNoOpTranscoder
import com.kenjigames.ividsmusic.data.download.AudioTranscoder
import com.kenjigames.ividsmusic.data.download.NativeDownloadManager
import com.kenjigames.ividsmusic.data.local.AppStorageDirectories
import com.kenjigames.ividsmusic.data.local.FilePlaybackSessionRepository
import com.kenjigames.ividsmusic.data.stream.AndroidStreamFallback
import com.kenjigames.ividsmusic.data.stream.JvmLocalAudioLocator
import com.kenjigames.ividsmusic.data.stream.LocalAudioLocator
import com.kenjigames.ividsmusic.data.stream.NewPipeStreamResolver
import com.kenjigames.ividsmusic.data.stream.PlatformStreamFallback
import com.kenjigames.ividsmusic.data.stream.StreamTierResolver
import com.kenjigames.ividsmusic.data.stream.YouTubeMetadataPreviewer
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
import java.util.concurrent.TimeUnit

/**
 * Android implementation of [platformDataModule] wiring `AndroidSqliteDriver` (resolving [Context] from Koin's
 * `androidContext`), DataStore, OkHttp, NewPipeExtractor, no-op fallbacks, and [NativeDownloadManager].
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
        val context: Context = get()
        AndroidSqliteDriver(
            schema = IvidsDatabase.Schema,
            context = context,
            name = "ivids_music.db",
            callback = object : AndroidSqliteDriver.Callback(IvidsDatabase.Schema) {
                /** Enables SQLite foreign-key constraints on Android API 24+ for ON DELETE CASCADE support. */
                override fun onConfigure(db: SupportSQLiteDatabase) {
                    super.onConfigure(db)
                    db.setForeignKeyConstraintsEnabled(true)
                }
            }
        )
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
    single<StreamTierResolver>(named(PRIMARY_STREAM_EXTRACTOR_QUALIFIER)) { get<NewPipeStreamResolver>() }
    single<YouTubeMetadataPreviewer> { get<NewPipeStreamResolver>() }

    single<PlatformStreamFallback> { AndroidStreamFallback() }
    single<AudioTranscoder> { AndroidNoOpTranscoder() }

    single<PlaybackSessionRepository> {
        val filesDir = File(directories.filesDir).apply { mkdirs() }
        FilePlaybackSessionRepository(sessionFile = File(filesDir, "session_state.json"), json = get())
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
            audioTranscoder = get()
        )
    }
}
