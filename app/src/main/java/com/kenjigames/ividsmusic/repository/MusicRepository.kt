package com.kenjigames.ividsmusic.repository

import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.network.NetworkModule
import com.kenjigames.ividsmusic.network.api.DeezerApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository coordinating online music discovery queries via the Deezer API.
 */
class MusicRepository(
    private val deezerApiService: DeezerApiService = NetworkModule.deezerApiService
) {
    /** Searches for tracks matching a keyword query */
    suspend fun searchTracks(query: String, limit: Int = 25): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            val response = deezerApiService.searchTracks(query, limit)
            val songs = response.data.map { it.toDomainModel() }
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Fetches default genre chart recommendations */
    suspend fun getGenreChart(limit: Int = 20): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            val response = deezerApiService.getGenreChart(limit)
            val songs = response.data.map { it.toDomainModel() }
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
