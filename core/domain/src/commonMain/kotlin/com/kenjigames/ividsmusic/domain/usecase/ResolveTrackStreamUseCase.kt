package com.kenjigames.ividsmusic.domain.usecase

import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.StreamSource
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.domain.port.StreamResolver

/**
 * Coordinates local offline lookup hydration with multi-tier stream resolution prior to audio playback.
 * Ensures a downloaded track plays locally even when the caller holds a fresh remote search [Track] instance.
 */
class ResolveTrackStreamUseCase(
    private val libraryRepository: LibraryRepository,
    private val streamResolver: StreamResolver
) {
    /**
     * Hydrates any locally persisted offline file path for [track] and resolves the optimal [StreamSource].
     * Reads [LibraryRepository.getTrack] without mutating state before invoking [StreamResolver.resolve].
     */
    suspend operator fun invoke(
        track: Track,
        preferredQuality: AudioQuality = AudioQuality.Q196
    ): AppResult<StreamSource> {
        val persisted = libraryRepository.getTrack(track.id)
        val hydratedTrack = if (persisted != null && !persisted.localFilePath.isNullOrBlank()) {
            track.copy(
                isDownloaded = persisted.isDownloaded,
                localFilePath = persisted.localFilePath,
                bitrateKbps = persisted.bitrateKbps ?: track.bitrateKbps
            )
        } else {
            track
        }
        return streamResolver.resolve(hydratedTrack, preferredQuality)
    }

    /**
     * Evicts cached stream URLs for [trackId] inside [StreamResolver] so retries fetch fresh endpoints.
     */
    fun invalidate(trackId: String) {
        streamResolver.invalidate(trackId)
    }
}
