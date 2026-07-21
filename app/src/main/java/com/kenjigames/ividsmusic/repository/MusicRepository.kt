package com.kenjigames.ividsmusic.repository

import android.content.Context
import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.Artist
import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.network.NetworkModule
import com.kenjigames.ividsmusic.network.NetworkMonitor
import com.kenjigames.ividsmusic.network.api.DeezerApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository coordinating online music discovery queries via the Deezer API.
 */
class MusicRepository(
    private val deezerApiService: DeezerApiService = NetworkModule.deezerApiService,
    private val context: Context? = null
) {
    /** Searches for tracks matching a keyword query with automatic slow network detection */
    suspend fun searchTracks(query: String, limit: Int = 25): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            val isSlow = NetworkMonitor.isSlowConnection(context)
            val response = deezerApiService.searchTracks(query, limit)
            val songs = response.data.map { it.toDomainModel(isSlow) }
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Searches for albums matching a keyword query */
    suspend fun searchAlbums(query: String, limit: Int = 15): Result<List<Album>> = withContext(Dispatchers.IO) {
        try {
            val response = deezerApiService.searchAlbums(query, limit)
            val albums = response.data.map { it.toDomainModel() }
            Result.success(albums)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Searches for artists matching a keyword query */
    suspend fun searchArtists(query: String, limit: Int = 15): Result<List<Artist>> = withContext(Dispatchers.IO) {
        try {
            val response = deezerApiService.searchArtists(query, limit)
            val artists = response.data.map { it.toDomainModel() }
            Result.success(artists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Fetches default genre chart recommendations with automatic slow network detection */
    suspend fun getGenreChart(genreId: Int = 0, limit: Int = 20): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            val isSlow = NetworkMonitor.isSlowConnection(context)
            val response = deezerApiService.getGenreChart(genreId, limit)
            val songs = response.data.map { it.toDomainModel(isSlow) }
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
