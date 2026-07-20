package com.kenjigames.ividsmusic.domain.model

/**
 * Data class representing a single music track in the domain layer.
 * Extends [MusicItem] for polymorphic UI rendering.
 *
 * @property id Unique track identifier (e.g., Deezer ID or YouTube Video ID)
 * @property title Name of the song
 * @property artistName Name of the performing artist
 * @property albumTitle Title of the album containing this track
 * @property coverUrl High-resolution cover artwork URL
 * @property durationSeconds Track duration in seconds
 * @property videoId Resolved YouTube Video ID for audio streaming
 * @property isLiked Indicates if the track is in the user's liked library
 * @property isDownloaded Indicates if the track audio is cached locally for offline playback
 * @property localFilePath Storage path if the track is downloaded
 */
data class Song(
    override val id: String,
    val title: String,
    val artistName: String,
    val albumTitle: String = "",
    override val coverUrl: String,
    val durationSeconds: Int = 0,
    val videoId: String = "",
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null
) : MusicItem() {
    override val displayTitle: String get() = title
}
