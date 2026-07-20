package com.kenjigames.ividsmusic.repository

import com.kenjigames.ividsmusic.data.dao.HistoryDao
import com.kenjigames.ividsmusic.data.entity.HistoryEntity
import com.kenjigames.ividsmusic.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository coordinating playback history logging and fetching from Room.
 */
class HistoryRepository(private val historyDao: HistoryDao) {

    /** Flow emitting recently played history capped at 20 items */
    val recentHistory: Flow<List<Song>> = historyDao.getRecentHistory().map { entities ->
        entities.map { it.toDomainModel() }
    }

    /** Adds a track to history and automatically enforces 20-item cap */
    suspend fun addSongToHistory(song: Song) {
        val entity = HistoryEntity(
            trackId = song.id,
            title = song.title,
            artistName = song.artistName,
            albumTitle = song.albumTitle,
            coverUrl = song.coverUrl
        )
        historyDao.addTrackToHistory(entity)
    }

    /** Clears all listening history */
    suspend fun clearHistory() {
        historyDao.clearHistory()
    }
}
