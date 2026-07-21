package com.kenjigames.ividsmusic.network.dto

import com.google.gson.annotations.SerializedName
import com.kenjigames.ividsmusic.domain.model.Artist

/**
 * Data Transfer Object (DTO) modeling the JSON response from Deezer artist search endpoints.
 */
data class DeezerArtistResponseDto(
    @SerializedName("data") val data: List<ArtistDataDto> = emptyList()
) {
    /** DTO for individual artist result */
    data class ArtistDataDto(
        @SerializedName("id") val id: String = "",
        @SerializedName("name") val name: String = "",
        @SerializedName("picture_small") val pictureSmall: String = "",
        @SerializedName("picture_medium") val pictureMedium: String = "",
        @SerializedName("picture_big") val pictureBig: String = "",
        @SerializedName("picture_xl") val pictureXl: String = "",
        @SerializedName("nb_fan") val nbFan: Int = 0
    ) {
        /** Converts DTO to domain [Artist] model */
        fun toDomainModel(): Artist {
            val img = pictureBig.takeIf { it.isNotEmpty() }
                ?: pictureXl.takeIf { it.isNotEmpty() }
                ?: pictureMedium

            return Artist(
                id = id,
                name = name,
                imageUrl = img,
                fanCount = nbFan
            )
        }
    }
}
