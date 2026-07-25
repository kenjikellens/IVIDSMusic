package com.kenjigames.ividsmusic.domain.usecase

import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.network.NetworkModule
import com.kenjigames.ividsmusic.network.resolver.StreamResolver

/**
 * Use case orchestrating YouTube stream resolution by chaining Innertube, Piped, and Invidious resolvers.
 */
class ResolveStreamUseCase(
    private val primaryResolver: StreamResolver = NetworkModule.youtubeInnertubeResolver,
    private val fallbackResolver: StreamResolver = NetworkModule.pipedStreamResolver
) {
    /** Resolves full YouTube stream URL for a given song */
    suspend operator fun invoke(song: Song): String? {
        // 1. Local offline download
        if (song.isDownloaded && song.localFilePath != null) {
            return song.localFilePath
        }

        // 2. Stream resolving via native yt-dlp Android Innertube protocol
        var videoId = song.videoId
        if (videoId.isEmpty()) {
            val query = "${song.artistName} - ${song.title}"
            videoId = primaryResolver.resolveVideoId(query)
                ?: fallbackResolver.resolveVideoId(query)
                ?: ""
        }
        if (videoId.isNotEmpty()) {
            val streamUrl = primaryResolver.resolveAudioUrl(videoId)
                ?: fallbackResolver.resolveAudioUrl(videoId)
            if (streamUrl != null) return streamUrl
        }

        // 3. Fallback to preview URL if stream resolution failed
        if (song.previewUrl.isNotEmpty()) {
            return song.previewUrl
        }

        return null
    }
}
