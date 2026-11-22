package com.kenjigames.ividsmusic.data.di

import com.kenjigames.ividsmusic.data.db.IvidsDatabase
import com.kenjigames.ividsmusic.data.local.ActiveSaveDirectoryHolder
import com.kenjigames.ividsmusic.data.local.AppStorageDirectories
import com.kenjigames.ividsmusic.data.local.DataStorePreferencesRepository
import com.kenjigames.ividsmusic.data.local.SqlLibraryRepository
import com.kenjigames.ividsmusic.data.local.SqlScoreRepository
import com.kenjigames.ividsmusic.data.remote.CompositeMusicDiscoveryRepository
import com.kenjigames.ividsmusic.data.remote.DeezerApiClient
import com.kenjigames.ividsmusic.data.remote.ItunesApiClient
import com.kenjigames.ividsmusic.data.stream.CompositeStreamResolver
import com.kenjigames.ividsmusic.data.stream.InMemoryStreamTtlCache
import com.kenjigames.ividsmusic.data.stream.InvidiousFallbackResolver
import com.kenjigames.ividsmusic.data.stream.StreamTierResolver
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.domain.port.MusicDiscoveryRepository
import com.kenjigames.ividsmusic.domain.port.PreferencesRepository
import com.kenjigames.ividsmusic.domain.port.ScoreRepository
import com.kenjigames.ividsmusic.domain.port.StreamResolver
import com.kenjigames.ividsmusic.domain.usecase.PlaybackQueueController
import com.kenjigames.ividsmusic.domain.usecase.RecommendationEngine
import com.kenjigames.ividsmusic.domain.usecase.ResolveTrackStreamUseCase
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Koin qualifier identifying the primary NewPipeExtractor stream resolver tier. */
internal const val PRIMARY_STREAM_EXTRACTOR_QUALIFIER = "primaryStreamExtractor"

/** DataStore file name persisted under the application files directory. */
internal const val PREFERENCES_DATASTORE_FILENAME = "ivids_preferences.preferences_pb"

/** Provides platform-independent wall-clock epoch milliseconds for KMP domain use cases. */
internal expect fun currentEpochMillis(): Long

/**
 * Provides platform-specific Koin bindings for SQLDelight drivers, DataStore, HTTP engines, NewPipe, and downloaders.
 * Callers supply resolved [AppStorageDirectories] deterministically without passing Android `Context` in the signature.
 */
expect fun platformDataModule(directories: AppStorageDirectories): Module

/**
 * Shared Koin dependency module registering discovery clients, stream resolution tiers, SQLDelight repositories,
 * DataStore preferences, and domain use cases consumed by `:composeApp`.
 */
val dataModule: Module = module {
    single<Json> {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    single { ActiveSaveDirectoryHolder(get<AppStorageDirectories>().savedDir) }

    single { DeezerApiClient(httpClient = get(), json = get()) }
    single { ItunesApiClient(httpClient = get(), json = get()) }
    single<MusicDiscoveryRepository> {
        CompositeMusicDiscoveryRepository(
            deezerClient = get(),
            itunesClient = get(),
            clockMs = ::currentEpochMillis
        )
    }

    single { InMemoryStreamTtlCache(clockMs = ::currentEpochMillis) }
    single {
        InvidiousFallbackResolver(
            httpClient = get(),
            json = get(),
            clockMs = ::currentEpochMillis
        )
    }
    single<StreamResolver> {
        CompositeStreamResolver(
            localAudioLocator = get(),
            savedDirectoryProvider = { get<ActiveSaveDirectoryHolder>().current() },
            streamTtlCache = get(),
            primaryExtractor = get<StreamTierResolver>(named(PRIMARY_STREAM_EXTRACTOR_QUALIFIER)),
            invidiousFallback = get<InvidiousFallbackResolver>(),
            platformFallback = get()
        )
    }

    single { IvidsDatabase(driver = get()) }
    single<LibraryRepository> { SqlLibraryRepository(database = get(), clockMs = ::currentEpochMillis) }
    single<ScoreRepository> { SqlScoreRepository(database = get()) }

    single { DataStorePreferencesRepository(dataStore = get()) }
    single<PreferencesRepository> { get<DataStorePreferencesRepository>() }

    single { PlaybackQueueController() }
    single {
        RecommendationEngine(
            scoreRepository = get(),
            discoveryRepository = get(),
            clockMs = ::currentEpochMillis
        )
    }
    single {
        ResolveTrackStreamUseCase(
            libraryRepository = get(),
            streamResolver = get()
        )
    }
}
