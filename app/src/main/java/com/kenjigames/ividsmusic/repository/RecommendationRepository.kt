package com.kenjigames.ividsmusic.repository

import com.kenjigames.ividsmusic.data.dao.DiscoveryScoreDao
import com.kenjigames.ividsmusic.data.entity.DiscoveryScoreEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository coordinating local interest scoring and recommendation expansion.
 */
class RecommendationRepository(private val discoveryScoreDao: DiscoveryScoreDao) {

    /** Increments interest score for an entity */
    suspend fun incrementScore(key: String, type: String, delta: Int = 1) = withContext(Dispatchers.IO) {
        val existing = discoveryScoreDao.getScore(key)
        val currentScore = existing?.score ?: 0
        val newScore = (currentScore + delta).coerceIn(0, 100)
        val updated = DiscoveryScoreEntity(
            entityKey = key,
            entityType = type,
            score = newScore,
            lastUpdated = System.currentTimeMillis()
        )
        discoveryScoreDao.updateScore(updated)
    }

    /** Retrieves top scoring interest entities */
    suspend fun getTopInterests(type: String, limit: Int = 5): List<DiscoveryScoreEntity> = withContext(Dispatchers.IO) {
        discoveryScoreDao.getTopScoringEntities(type, limit)
    }

    /** Clears all discovery recommendation scores */
    suspend fun resetScores() = withContext(Dispatchers.IO) {
        discoveryScoreDao.clearScores()
    }
}
