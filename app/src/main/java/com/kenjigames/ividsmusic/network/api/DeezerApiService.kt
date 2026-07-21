package com.kenjigames.ividsmusic.network.api

import com.kenjigames.ividsmusic.network.dto.DeezerAlbumResponseDto
import com.kenjigames.ividsmusic.network.dto.DeezerArtistResponseDto
import com.kenjigames.ividsmusic.network.dto.DeezerSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service interface defining endpoints for music discovery via the Deezer API.
 */
interface DeezerApiService {

    /** Performs a track search query */
    @GET("search")
    suspend fun searchTracks(
        @Query("q") query: String,
        @Query("limit") limit: Int = 25,
        @Query("index") index: Int = 0
    ): DeezerSearchResponseDto

    /** Performs an album search query */
    @GET("search/album")
    suspend fun searchAlbums(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): DeezerAlbumResponseDto

    /** Performs an artist search query */
    @GET("search/artist")
    suspend fun searchArtists(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): DeezerArtistResponseDto

    /** Fetches genre charts */
    @GET("chart/{genreId}/tracks")
    suspend fun getGenreChart(
        @Path("genreId") genreId: Int = 0,
        @Query("limit") limit: Int = 20
    ): DeezerSearchResponseDto

    /** Fetches detailed metadata for a track by ID */
    @GET("track/{id}")
    suspend fun getTrackDetails(
        @Path("id") id: String
    ): DeezerSearchResponseDto.TrackDataDto
}
