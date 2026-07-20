package com.kenjigames.ividsmusic.domain.model

/**
 * Data class representing a musician or band in the domain layer.
 * Extends [MusicItem] for circular avatar UI rendering.
 *
 * @property id Unique artist identifier
 * @property name Artist name
 * @property imageUrl High-resolution artist picture URL
 * @property fanCount Number of followers/listeners on Deezer
 * @property genres Associated music genre tags
 */
data class Artist(
    override val id: String,
    val name: String,
    val imageUrl: String,
    val fanCount: Int = 0,
    val genres: List<String> = emptyList()
) : MusicItem() {
    override val displayTitle: String get() = name
    override val coverUrl: String get() = imageUrl
}
