package com.kenjigames.ividsmusic.repository

import android.util.Log
import com.kenjigames.ividsmusic.data.dao.HistoryDao
import com.kenjigames.ividsmusic.data.entity.HistoryEntity
import com.kenjigames.ividsmusic.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Repository coordinating playback history logging and fetching from Room.
 */
class HistoryRepository(private val historyDao: HistoryDao) {

    /** Flow emitting recently played history capped at 20 items safely */
    val recentHistory: Flow<List<Song>> = historyDao.getRecentHistory()
        .map { entities -> entities.map { it.toDomainModel() } }
        .catch { e ->
            Log.e("HistoryRepository", "Error fetching history: ${e.message}")
            emit(emptyList())
        }

    /** Adds a track to history and automatically enforces 20-item cap */
    suspend fun addSongToHistory(song: Song) {
        try {
            val entity = HistoryEntity(
                trackId = song.id,
                title = song.title,
                artistName = song.artistName,
                albumTitle = song.albumTitle,
                coverUrl = song.coverUrl
            )
            historyDao.addTrackToHistory(entity)
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Error adding history: ${e.message}")
        }
    }

    /** Clears all listening history */
    suspend fun clearHistory() {
        try {
            historyDao.clearHistory()
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Error clearing history: ${e.message}")
        }
    }
}
