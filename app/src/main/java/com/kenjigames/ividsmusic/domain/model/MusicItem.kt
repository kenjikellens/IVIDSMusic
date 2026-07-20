package com.kenjigames.ividsmusic.domain.model

/**
 * Sealed class representing the base item for all music domain models (Song, Artist, Album).
 * Enables type-safe pattern matching in ViewModels and Compose UI components.
 */
sealed class MusicItem {
    /** Unique identifier for the music item */
    abstract val id: String
    
    /** Display title or name of the music item */
    abstract val displayTitle: String
    
    /** Cover or avatar image URL */
    abstract val coverUrl: String
}
