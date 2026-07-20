package com.kenjigames.ividsmusic.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kenjigames.ividsmusic.domain.model.Song

/**
 * Room entity class representing a track stored in local SQLite database.
 * Holds track metadata, offline download paths, and liked status.
 */
@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistName: String,
    val albumTitle: String,
    val coverUrl: String,
    val durationSeconds: Int,
    val videoId: String = "",
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Converts Room database entity into clean domain [Song] model.
     */
    fun toDomainModel(): Song = Song(
        id = id,
        title = title,
        artistName = artistName,
        albumTitle = albumTitle,
        coverUrl = coverUrl,
        durationSeconds = durationSeconds,
        videoId = videoId,
        isLiked = isLiked,
        isDownloaded = isDownloaded,
        localFilePath = localFilePath
    )

    companion object {
        /**
         * Creates a [TrackEntity] from a domain [Song] model.
         */
        fun fromDomainModel(song: Song): TrackEntity = TrackEntity(
            id = song.id,
            title = song.title,
            artistName = song.artistName,
            albumTitle = song.albumTitle,
            coverUrl = song.coverUrl,
            durationSeconds = song.durationSeconds,
            videoId = song.videoId,
            isLiked = song.isLiked,
            isDownloaded = song.isDownloaded,
            localFilePath = song.localFilePath
        )
    }
}
