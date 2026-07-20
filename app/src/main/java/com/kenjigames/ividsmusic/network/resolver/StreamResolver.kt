package com.kenjigames.ividsmusic.network.resolver

/**
 * Strategy interface defining stream resolution for YouTube audio tracks.
 */
interface StreamResolver {
    /** Resolves search query to a YouTube video ID */
    suspend fun resolveVideoId(query: String): String?

    /** Resolves YouTube video ID to a direct playable MP3/AAC stream URL */
    suspend fun resolveAudioUrl(videoId: String): String?
}
