package com.kenjigames.ividsmusic.player.desktop

/**
 * Exposes safe desktop audio pre-transcoding from `:core:player` to `:composeApp`'s [com.kenjigames.ividsmusic.domain.port.StreamPrefetcher].
 * Warms cached local `.mp3` files ahead of track transitions without blocking playback start.
 */
object DesktopAudioPrefetcher {

    /**
     * Resolves and transcodes a remote stream into the local JavaFX `.mp3` cache using [cacheKeyHint].
     * Safe no-op when `ffmpeg` is absent or the target file is already cached.
     */
    suspend fun prefetchToMp3Cache(
        mediaUri: String,
        headers: Map<String, String>,
        cacheKeyHint: String? = null,
    ) {
        DesktopAudioCacheTranscoder.prepareJavaFxCompatibleUri(
            mediaUri = mediaUri,
            headers = headers,
            cacheKeyHint = cacheKeyHint,
        )
    }
}
