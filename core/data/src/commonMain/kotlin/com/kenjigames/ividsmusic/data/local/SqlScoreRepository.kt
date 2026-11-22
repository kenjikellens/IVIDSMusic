package com.kenjigames.ividsmusic.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.kenjigames.ividsmusic.data.db.Discovery_scores
import com.kenjigames.ividsmusic.data.db.IvidsDatabase
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.InterestScore
import com.kenjigames.ividsmusic.domain.model.ScoreEntityType
import com.kenjigames.ividsmusic.domain.port.ScoreRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * SQLDelight-backed implementation of [ScoreRepository] persisting clamped (`0..100`) discovery interest scores locally.
 * Reads and writes through `discovery_scores` on [ioDispatcher] with zero external telemetry.
 */
class SqlScoreRepository(
    private val database: IvidsDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ScoreRepository {

    private val queries = database.ividsMusicQueries

    /** Observes all local discovery scores ordered by score descending. */
    override fun observeAllScores(): Flow<List<InterestScore>> =
        queries.selectAllScores()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.mapNotNull(::mapScore) }

    /** Fetches a snapshot of all persisted [InterestScore] records on [ioDispatcher]. */
    override suspend fun getAllScores(): List<InterestScore> = withContext(ioDispatcher) {
        queries.selectAllScores().executeAsList().mapNotNull(::mapScore)
    }

    /** Retrieves the score entry for [entityKey], or `null` when none exists yet. */
    override suspend fun getScore(entityKey: String): InterestScore? = withContext(ioDispatcher) {
        if (entityKey.isBlank()) return@withContext null
        queries.selectScoreByKey(entityKey).executeAsOneOrNull()?.let(::mapScore)
    }

    /** Inserts or replaces [score] after enforcing the `0..100` domain and SQL CHECK bounds. */
    override suspend fun upsertScore(score: InterestScore): AppResult<Unit> = withContext(ioDispatcher) {
        if (score.entityKey.isBlank() || score.entityId.isBlank()) {
            return@withContext AppResult.Failure(DomainError.Validation("Score key and entityId are required"))
        }
        try {
            val clampedScore = score.score.coerceIn(0, 100)
            queries.upsertScore(
                entity_key = score.entityKey,
                entity_type = score.entityType.name,
                entity_id = score.entityId,
                display_name = score.displayName,
                score = clampedScore.toLong(),
                last_updated_ms = score.lastUpdatedMs
            )
            AppResult.Success(Unit)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            AppResult.Failure(DomainError.Storage(error.message ?: "Failed to upsert discovery score"))
        }
    }

    /** Deletes all stored discovery scores from SQLite. */
    override suspend fun clearAllScores(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            queries.clearAllScores()
            AppResult.Success(Unit)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            AppResult.Failure(DomainError.Storage(error.message ?: "Failed to clear discovery scores"))
        }
    }

    private fun mapScore(row: Discovery_scores): InterestScore? {
        val type = ScoreEntityType.entries.firstOrNull { it.name == row.entity_type } ?: return null
        return InterestScore(
            entityKey = row.entity_key,
            entityType = type,
            entityId = row.entity_id,
            displayName = row.display_name,
            score = row.score.toInt().coerceIn(0, 100),
            lastUpdatedMs = row.last_updated_ms
        )
    }
}
