package com.kenjigames.ividsmusic.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kenjigames.ividsmusic.data.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing user playback history.
 * Enforces a strict 20-item maximum limit on local playback history.
 */
@Dao
interface HistoryDao {
    /**
     * Gets the list of recently played tracks ordered by playedTimestamp descending.
     */
    @Query("SELECT * FROM listen_history ORDER BY playedTimestamp DESC LIMIT 20")
    fun getRecentHistory(): Flow<List<HistoryEntity>>

    /**
     * Inserts or replaces a track entry in the history table.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: HistoryEntity)

    /**
     * Trims history items exceeding the 20-item cap.
     */
    @Query("DELETE FROM listen_history WHERE trackId NOT IN (SELECT trackId FROM listen_history ORDER BY playedTimestamp DESC LIMIT 20)")
    suspend fun trimExcessHistory()

    /**
     * Transaction helper to insert a history item and immediately trim old entries.
     */
    @Transaction
    suspend fun addTrackToHistory(item: HistoryEntity) {
        insertHistoryItem(item)
        trimExcessHistory()
    }

    /**
     * Purges all playback history.
     */
    @Query("DELETE FROM listen_history")
    suspend fun clearHistory()
}
