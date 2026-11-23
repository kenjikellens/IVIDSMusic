package com.kenjigames.ividsmusic.di

import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.StreamSource
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.StreamPrefetcher
import com.kenjigames.ividsmusic.domain.port.StreamResolver
import com.kenjigames.ividsmusic.platform.AppUpdateChecker
import com.kenjigames.ividsmusic.platform.PlatformBridge
import com.kenjigames.ividsmusic.player.desktop.DesktopAudioPrefetcher
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Windows Desktop Koin UI bindings providing Swing directory selection, update checks, and MP3 next-track prefetching.
 */
actual fun platformUiModule(): Module = module {
    single { PlatformBridge() }
    single { AppUpdateChecker(platformBridge = get(), json = get()) }
    single<StreamPrefetcher> {
        val resolver = get<StreamResolver>()
        object : StreamPrefetcher {
            /** Resolves upcoming track URLs and warms the local JavaFX `.mp3` transcode cache. */
            override suspend fun prefetch(track: Track, preferredQuality: AudioQuality) {
                val res = resolver.resolve(track, preferredQuality)
                val source = (res as? AppResult.Success)?.data
                if (source is StreamSource.RemoteUrl) {
                    val hint = track.videoId?.takeIf { it.isNotBlank() } ?: track.id
                    DesktopAudioPrefetcher.prefetchToMp3Cache(source.url, source.headers, hint)
                }
            }
        }
    }
}
