package com.kenjigames.ividsmusic.repository

import com.kenjigames.ividsmusic.data.dao.TrackDao
import com.kenjigames.ividsmusic.data.entity.TrackEntity
import com.kenjigames.ividsmusic.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Repository coordinating local Room database queries for liked songs, saved albums, and offline downloads.
 */
class LibraryRepository(private val trackDao: TrackDao) {

    /** Flow emitting all saved/liked tracks from Room database */
    val savedTracks: Flow<List<Song>> = trackDao.getAllTracks().map { entities ->
        entities.map { it.toDomainModel() }
    }

    /** Saves a track to Room database */
    suspend fun saveTrack(song: Song) {
        trackDao.insertTrack(TrackEntity.fromDomainModel(song))
    }

    /** Deletes a track from Room database and purges offline audio file if present */
    suspend fun deleteTrack(song: Song) {
        trackDao.deleteTrackById(song.id)
        song.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) file.delete()
        }
    }
}
