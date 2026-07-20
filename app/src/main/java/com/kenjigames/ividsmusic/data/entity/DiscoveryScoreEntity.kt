package com.kenjigames.ividsmusic.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity tracking local recommendation interest scores for artists, tracks, and genres.
 * Scores are updated based on user interactions (play complete, like, skip, click).
 */
@Entity(tableName = "discovery_scores")
data class DiscoveryScoreEntity(
    @PrimaryKey val entityKey: String,
    val entityType: String,
    val score: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
