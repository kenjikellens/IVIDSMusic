package com.kenjigames.ividsmusic.network.dto

import com.google.gson.annotations.SerializedName
import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.Artist

/**
 * Data Transfer Object (DTO) modeling the JSON response from Deezer album search endpoints.
 */
data class DeezerAlbumResponseDto(
    @SerializedName("data") val data: List<AlbumDataDto> = emptyList()
) {
    /** DTO for individual album result */
    data class AlbumDataDto(
        @SerializedName("id") val id: String = "",
        @SerializedName("title") val title: String = "",
        @SerializedName("cover_small") val coverSmall: String = "",
        @SerializedName("cover_medium") val coverMedium: String = "",
        @SerializedName("cover_big") val coverBig: String = "",
        @SerializedName("cover_xl") val coverXl: String = "",
        @SerializedName("nb_tracks") val nbTracks: Int = 0,
        @SerializedName("release_date") val releaseDate: String = "",
        @SerializedName("artist") val artist: DeezerSearchResponseDto.TrackDataDto.ArtistDataDto? = null
    ) {
        /** Converts DTO to domain [Album] model */
        fun toDomainModel(): Album {
            val cover = coverBig.takeIf { it.isNotEmpty() }
                ?: coverXl.takeIf { it.isNotEmpty() }
                ?: coverMedium

            val artistModel = artist?.let {
                Artist(
                    id = it.id,
                    name = it.name,
                    imageUrl = it.pictureBig.takeIf { p -> p.isNotEmpty() } ?: it.pictureMedium
                )
            }

            return Album(
                id = id,
                title = title,
                artistName = artist?.name ?: "Unknown Artist",
                artist = artistModel,
                coverUrl = cover,
                trackCount = nbTracks,
                releaseDate = releaseDate
            )
        }
    }
}
