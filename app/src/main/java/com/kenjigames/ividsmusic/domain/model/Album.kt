package com.kenjigames.ividsmusic.domain.model

/**
 * Data class representing a music album in the domain layer.
 * Extends [MusicItem] for album card rendering.
 *
 * @property id Unique album identifier
 * @property title Name of the album
 * @property artistName Name of the performing artist
 * @property artist Typed reference to performing [Artist] entity
 * @property coverUrl High-resolution album cover artwork URL
 * @property trackCount Total number of songs in the album
 * @property releaseDate Release year or date string
 * @property songs List of tracks included in the album
 */
data class Album(
    override val id: String,
    val title: String,
    val artistName: String,
    val artist: Artist? = null,
    override val coverUrl: String,
    val trackCount: Int = 0,
    val releaseDate: String = "",
    val songs: List<Song> = emptyList()
) : MusicItem() {
    override val displayTitle: String get() = title
}
