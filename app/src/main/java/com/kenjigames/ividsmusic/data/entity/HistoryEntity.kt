package com.kenjigames.ividsmusic.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kenjigames.ividsmusic.domain.model.Song

/**
 * Room entity representing a recently played song entry in history.
 * Automatically capped at 20 entries via HistoryDao transactions.
 */
@Entity(tableName = "listen_history")
data class HistoryEntity(
    @PrimaryKey val trackId: String,
    val title: String,
    val artistName: String,
    val albumTitle: String,
    val coverUrl: String,
    val playedTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Maps history entity to domain [Song] model.
     */
    fun toDomainModel(): Song = Song(
        id = trackId,
        title = title,
        artistName = artistName,
        albumTitle = albumTitle,
        coverUrl = coverUrl
    )
}
