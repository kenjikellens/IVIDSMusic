package com.kenjigames.ividsmusic.data.entity

import androidx.room.Entity

/**
 * Join table entity establishing a many-to-many relationship between playlists and tracks.
 */
@Entity(
    tableName = "playlist_track_cross_ref",
    primaryKeys = ["playlistId", "trackId"]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: String,
    val positionInPlaylist: Int = 0
)
