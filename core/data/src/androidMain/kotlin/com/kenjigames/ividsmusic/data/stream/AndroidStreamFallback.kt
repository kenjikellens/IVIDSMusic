package com.kenjigames.ividsmusic.data.stream

import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.StreamSource
import com.kenjigames.ividsmusic.domain.model.Track

/**
 * Android no-op implementation of [PlatformStreamFallback] because Desktop CLI `yt-dlp.exe` is unavailable on Android.
 * Fails cleanly so [CompositeStreamResolver] reports upstream NewPipe and Invidious errors accurately.
 */
class AndroidStreamFallback : PlatformStreamFallback {

    /** Returns [DomainError.StreamNotFound] cleanly when upstream Android stream tiers fail. */
    override suspend fun resolveTrack(
        track: Track,
        preferredQuality: AudioQuality
    ): AppResult<StreamSource.RemoteUrl> =
        AppResult.Failure(DomainError.StreamNotFound("No additional Android CLI fallback available"))

    /** Returns [DomainError.StreamNotFound] cleanly when upstream direct URL tiers fail on Android. */
    override suspend fun resolveDirectUrl(
        videoUrlOrId: String,
        preferredQuality: AudioQuality
    ): AppResult<StreamSource.RemoteUrl> =
        AppResult.Failure(DomainError.StreamNotFound("No additional Android CLI fallback available"))
}
