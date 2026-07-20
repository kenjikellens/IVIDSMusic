package com.kenjigames.ividsmusic.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kenjigames.ividsmusic.data.entity.DiscoveryScoreEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing local recommendation interest scores.
 */
@Dao
interface DiscoveryScoreDao {
    /**
     * Retrieves top scoring entities filtered by type.
     */
    @Query("SELECT * FROM discovery_scores WHERE entityType = :type ORDER BY score DESC LIMIT :limit")
    suspend fun getTopScoringEntities(type: String, limit: Int = 5): List<DiscoveryScoreEntity>

    /**
     * Gets a single discovery score entity by key.
     */
    @Query("SELECT * FROM discovery_scores WHERE entityKey = :key LIMIT 1")
    suspend fun getScore(key: String): DiscoveryScoreEntity?

    /**
     * Inserts or updates an interest score.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateScore(entity: DiscoveryScoreEntity)

    /**
     * Purges all interest scores.
     */
    @Query("DELETE FROM discovery_scores")
    suspend fun clearScores()
}
