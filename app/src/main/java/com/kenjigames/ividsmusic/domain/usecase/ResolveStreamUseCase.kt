package com.kenjigames.ividsmusic.domain.usecase

import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.network.NetworkModule
import com.kenjigames.ividsmusic.network.resolver.StreamResolver

/**
 * Use case orchestrating YouTube stream resolution by chaining Invidious, scraper, and preview fallbacks.
 */
class ResolveStreamUseCase(
    private val primaryResolver: StreamResolver = NetworkModule.invidiousStreamResolver,
    private val fallbackResolver: StreamResolver = NetworkModule.youtubeHtmlScraper
) {
    /** Resolves stream URL for a given song */
    suspend operator fun invoke(song: Song): String? {
        // 1. Local offline download
        if (song.isDownloaded && song.localFilePath != null) {
            return song.localFilePath
        }

        // 2. Stream resolving
        var videoId = song.videoId
        if (videoId.isEmpty()) {
            val query = "${song.artistName} - ${song.title}"
            videoId = primaryResolver.resolveVideoId(query)
                ?: fallbackResolver.resolveVideoId(query)
                ?: ""
        }
        if (videoId.isNotEmpty()) {
            val streamUrl = primaryResolver.resolveAudioUrl(videoId)
            if (streamUrl != null) return streamUrl
        }

        // 3. Fallback to Deezer preview URL
        if (song.previewUrl.isNotEmpty()) {
            return song.previewUrl
        }

        return null
    }
}
