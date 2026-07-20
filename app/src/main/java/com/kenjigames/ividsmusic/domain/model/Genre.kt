package com.kenjigames.ividsmusic.domain.model

/**
 * Data class representing a music category or genre.
 *
 * @property id Unique genre identifier
 * @property name Display name of the genre (e.g., Pop, Rock, Hip-Hop)
 * @property pictureUrl Background image URL for genre exploration cards
 */
data class Genre(
    val id: String,
    val name: String,
    val pictureUrl: String = ""
)
