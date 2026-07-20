package com.kenjigames.ividsmusic.domain.model

/**
 * Data class representing a user-created custom playlist.
 *
 * @property id Auto-generated unique playlist ID
 * @property title Name of the playlist
 * @property description Optional user description
 * @property coverPath Path or URL for custom playlist artwork
 * @property tracks List of songs contained in the playlist
 * @property createdTimestamp Epoch timestamp when created
 */
data class Playlist(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val coverPath: String? = null,
    val tracks: List<Song> = emptyList(),
    val createdTimestamp: Long = System.currentTimeMillis()
)
