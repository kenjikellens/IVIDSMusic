package com.kenjigames.ividsmusic.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kenjigames.ividsmusic.domain.model.Playlist

/**
 * Room entity representing a user-created custom playlist.
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val playlistId: Long = 0,
    val title: String,
    val description: String = "",
    val coverPath: String? = null,
    val createdTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Maps database playlist entity to domain [Playlist] model.
     */
    fun toDomainModel(): Playlist = Playlist(
        id = playlistId,
        title = title,
        description = description,
        coverPath = coverPath,
        createdTimestamp = createdTimestamp
    )
}
