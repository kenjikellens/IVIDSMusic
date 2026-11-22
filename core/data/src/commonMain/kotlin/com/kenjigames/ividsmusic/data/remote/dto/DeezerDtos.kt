package com.kenjigames.ividsmusic.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Error payload returned inside Deezer HTTP 200 JSON responses when quotas or lookups fail.
 */
@Serializable
data class DeezerErrorDto(
    val type: String? = null,
    val message: String? = null,
    val code: Int? = null
)

/**
 * Generic paginated list envelope returned by Deezer search, chart, radio, and artist sub-endpoints.
 */
@Serializable
data class DeezerListResponse<T>(
    val data: List<T> = emptyList(),
    val total: Int = 0,
    val error: DeezerErrorDto? = null
)

/**
 * Deezer genre tag descriptor returned inside album and genre endpoints.
 */
@Serializable
data class DeezerGenreDto(
    val id: Long = 0L,
    val name: String = ""
)

/**
 * Nested wrapper around genre lists returned by Deezer album endpoints.
 */
@Serializable
data class DeezerGenreListDto(
    val data: List<DeezerGenreDto> = emptyList()
)

/**
 * Deezer artist object returned across search, track, album, and artist detail responses.
 */
@Serializable
data class DeezerArtistDto(
    val id: Long = 0L,
    val name: String = "",
    @SerialName("picture_medium") val pictureMedium: String? = null,
    @SerialName("picture_big") val pictureBig: String? = null,
    @SerialName("picture_xl") val pictureXl: String? = null,
    @SerialName("nb_fan") val nbFan: Long = 0L,
    @SerialName("nb_album") val nbAlbum: Int = 0,
    val error: DeezerErrorDto? = null
)

/**
 * Deezer track object returned by search, chart, radio, track detail, and album tracklist endpoints.
 */
@Serializable
data class DeezerTrackDto(
    val id: Long = 0L,
    val title: String = "",
    val duration: Int = 0,
    val rank: Int = 0,
    val preview: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val artist: DeezerArtistDto? = null,
    val album: DeezerAlbumSummaryDto? = null,
    val error: DeezerErrorDto? = null
)

/**
 * Lightweight nested album object embedded inside Deezer track responses.
 */
@Serializable
data class DeezerAlbumSummaryDto(
    val id: Long = 0L,
    val title: String = "",
    @SerialName("cover_medium") val coverMedium: String? = null,
    @SerialName("cover_big") val coverBig: String? = null,
    @SerialName("cover_xl") val coverXl: String? = null,
    @SerialName("release_date") val releaseDate: String? = null
)

/**
 * Full Deezer album payload returned by `/search/album`, `/artist/{id}/albums`, and `/album/{id}`.
 */
@Serializable
data class DeezerAlbumDto(
    val id: Long = 0L,
    val title: String = "",
    @SerialName("cover_medium") val coverMedium: String? = null,
    @SerialName("cover_big") val coverBig: String? = null,
    @SerialName("cover_xl") val coverXl: String? = null,
    @SerialName("nb_tracks") val nbTracks: Int = 0,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("genre_id") val genreId: Long? = null,
    val genres: DeezerGenreListDto? = null,
    val artist: DeezerArtistDto? = null,
    val tracks: DeezerListResponse<DeezerTrackDto>? = null,
    val error: DeezerErrorDto? = null
)
