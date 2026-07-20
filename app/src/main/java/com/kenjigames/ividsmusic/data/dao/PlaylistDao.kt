package com.kenjigames.ividsmusic.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kenjigames.ividsmusic.data.entity.PlaylistEntity
import com.kenjigames.ividsmusic.data.entity.PlaylistTrackCrossRef
import com.kenjigames.ividsmusic.data.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing custom playlists and track cross-references.
 */
@Dao
interface PlaylistDao {
    /**
     * Retrieves all user playlists ordered by createdTimestamp descending.
     */
    @Query("SELECT * FROM playlists ORDER BY createdTimestamp DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    /**
     * Gets a single playlist by ID.
     */
    @Query("SELECT * FROM playlists WHERE playlistId = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    /**
     * Inserts a new playlist or updates an existing one.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    /**
     * Deletes a playlist by ID.
     */
    @Query("DELETE FROM playlists WHERE playlistId = :id")
    suspend fun deletePlaylist(id: Long)

    /**
     * Inserts a track cross-reference into a playlist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackCrossRef(crossRef: PlaylistTrackCrossRef)

    /**
     * Removes a track from a playlist.
     */
    @Query("DELETE FROM playlist_track_cross_ref WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String)

    /**
     * Retrieves all tracks associated with a specific playlist ID ordered by position.
     */
    @Transaction
    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_track_cross_ref ref ON t.id = ref.trackId
        WHERE ref.playlistId = :playlistId
        ORDER BY ref.positionInPlaylist ASC
    """)
    fun getTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>>
}
