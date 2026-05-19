package com.kenjigames.ividsmusic.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object (DAO) interface defining database query methods for the Track entity.
 * Provides APIs to query, insert, update, and delete cached track data.
 */
@Dao
public interface TrackDao {

    /**
     * Retrieves all tracks in the database, ordered by timestamp descending.
     *
     * @return LiveData containing the list of all stored tracks.
     */
    @Query("SELECT * FROM tracks ORDER BY timestamp DESC")
    LiveData<List<Track>> getAllTracks();

    /**
     * Retrieves all successfully downloaded tracks, ordered by timestamp descending.
     *
     * @return List of downloaded tracks.
     */
    @Query("SELECT * FROM tracks WHERE isDownloaded = 1 ORDER BY timestamp DESC")
    List<Track> getDownloadedTracks();

    /**
     * Finds a single track by its unique identifier.
     *
     * @param id The track ID.
     * @return The matching Track, or null if not found.
     */
    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    Track getTrackById(String id);

    /**
     * Inserts a track into the database, replacing it if the ID already exists.
     *
     * @param track The Track to insert or update.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTrack(Track track);

    /**
     * Deletes a track record from the database.
     *
     * @param track The Track to delete.
     */
    @Delete
    void deleteTrack(Track track);

    /**
     * Deletes a track record by its unique identifier.
     *
     * @param id The track ID.
     */
    @Query("DELETE FROM tracks WHERE id = :id")
    void deleteTrackById(String id);
}
