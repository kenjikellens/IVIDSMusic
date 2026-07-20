package com.kenjigames.ividsmusic.repository

import com.kenjigames.ividsmusic.data.dao.PlaylistDao
import com.kenjigames.ividsmusic.data.entity.PlaylistEntity
import com.kenjigames.ividsmusic.data.entity.PlaylistTrackCrossRef
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository coordinating custom playlist management via Room.
 */
class PlaylistRepository(private val playlistDao: PlaylistDao) {

    /** Flow emitting all user created playlists */
    val playlists: Flow<List<Playlist>> = playlistDao.getAllPlaylists().map { entities ->
        entities.map { it.toDomainModel() }
    }

    /** Creates a new custom playlist */
    suspend fun createPlaylist(title: String, description: String = ""): Long {
        val entity = PlaylistEntity(title = title, description = description)
        return playlistDao.insertPlaylist(entity)
    }

    /** Deletes a playlist by ID */
    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    /** Adds a song to a playlist */
    suspend fun addSongToPlaylist(playlistId: Long, songId: String, position: Int = 0) {
        val crossRef = PlaylistTrackCrossRef(
            playlistId = playlistId,
            trackId = songId,
            positionInPlaylist = position
        )
        playlistDao.insertTrackCrossRef(crossRef)
    }

    /** Removes a song from a playlist */
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        playlistDao.removeTrackFromPlaylist(playlistId, songId)
    }

    /** Retrieves songs contained in a specific playlist */
    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getTracksForPlaylist(playlistId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
}
