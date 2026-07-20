package com.kenjigames.ividsmusic.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kenjigames.ividsmusic.data.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) interface defining database query methods for the Track entity.
 */
@Dao
interface TrackDao {

    /** Retrieves all tracks in the database ordered by timestamp descending */
    @Query("SELECT * FROM tracks ORDER BY addedTimestamp DESC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    /** Retrieves all downloaded tracks for offline playback */
    @Query("SELECT * FROM tracks WHERE isDownloaded = 1 ORDER BY addedTimestamp DESC")
    suspend fun getDownloadedTracks(): List<TrackEntity>

    /** Finds a single track by its unique identifier */
    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): TrackEntity?

    /** Inserts or replaces a track record in the database */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    /** Deletes a track record by its unique identifier */
    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteTrackById(id: String)
}
