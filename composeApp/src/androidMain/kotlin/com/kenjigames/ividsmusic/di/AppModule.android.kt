package com.kenjigames.ividsmusic.di

import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.StreamPrefetcher
import com.kenjigames.ividsmusic.domain.port.StreamResolver
import com.kenjigames.ividsmusic.platform.AppUpdateChecker
import com.kenjigames.ividsmusic.platform.PlatformBridge
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android Koin UI bindings providing [PlatformBridge], [AppUpdateChecker], and stream metadata prefetching.
 */
actual fun platformUiModule(): Module = module {
    single { PlatformBridge(context = androidContext()) }
    single { AppUpdateChecker(context = androidContext(), platformBridge = get(), json = get()) }
    single<StreamPrefetcher> {
        val resolver = get<StreamResolver>()
        object : StreamPrefetcher {
            /** Pre-resolves upcoming track URLs into the 30-minute TTL stream cache. */
            override suspend fun prefetch(track: Track, preferredQuality: AudioQuality) {
                resolver.resolve(track, preferredQuality)
            }
        }
    }
}
