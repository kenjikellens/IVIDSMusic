package com.kenjigames.ividsmusic.domain.usecase

import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.network.NetworkModule
import com.kenjigames.ividsmusic.network.resolver.StreamResolver

/**
 * Use case orchestrating YouTube stream resolution by chaining Invidious and scraper strategies.
 */
class ResolveStreamUseCase(
    private val primaryResolver: StreamResolver = NetworkModule.invidiousStreamResolver,
    private val fallbackResolver: StreamResolver = NetworkModule.youtubeHtmlScraper
) {
    /** Resolves stream URL for a given song */
    suspend operator fun invoke(song: Song): String? {
        var videoId = song.videoId
        if (videoId.isEmpty()) {
            val query = "${song.artistName} - ${song.title}"
            videoId = primaryResolver.resolveVideoId(query)
                ?: fallbackResolver.resolveVideoId(query)
                ?: return null
        }
        return primaryResolver.resolveAudioUrl(videoId)
    }
}
