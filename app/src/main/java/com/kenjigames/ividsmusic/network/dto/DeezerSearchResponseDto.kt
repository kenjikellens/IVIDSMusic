package com.kenjigames.ividsmusic.network.dto

import com.google.gson.annotations.SerializedName
import com.kenjigames.ividsmusic.domain.model.Song

/**
 * Data Transfer Object (DTO) modeling the JSON response from the Deezer API search endpoints.
 */
data class DeezerSearchResponseDto(
    @SerializedName("data") val data: List<TrackDataDto> = emptyList()
) {
    /** DTO for individual track item */
    data class TrackDataDto(
        @SerializedName("id") val id: String = "",
        @SerializedName("title") val title: String = "",
        @SerializedName("duration") val duration: Int = 0,
        @SerializedName("preview") val preview: String = "",
        @SerializedName("artist") val artist: ArtistDataDto? = null,
        @SerializedName("album") val album: AlbumDataDto? = null
    ) {
        /** DTO for artist details within a track search result */
        data class ArtistDataDto(
            @SerializedName("id") val id: String = "",
            @SerializedName("name") val name: String = "",
            @SerializedName("picture_medium") val pictureMedium: String = ""
        )

        /** DTO for album details within a track search result */
        data class AlbumDataDto(
            @SerializedName("id") val id: String = "",
            @SerializedName("title") val title: String = "",
            @SerializedName("cover_medium") val coverMedium: String = ""
        )

        /** Converts DTO to domain [Song] model */
        fun toDomainModel(): Song = Song(
            id = id,
            title = title,
            artistName = artist?.name ?: "Unknown Artist",
            albumTitle = album?.title ?: "",
            coverUrl = album?.coverMedium ?: artist?.pictureMedium ?: "",
            durationSeconds = duration,
            previewUrl = preview
        )
    }
}
