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
            @SerializedName("picture_small") val pictureSmall: String = "",
            @SerializedName("picture_medium") val pictureMedium: String = "",
            @SerializedName("picture_big") val pictureBig: String = ""
        )

        /** DTO for album details within a track search result */
        data class AlbumDataDto(
            @SerializedName("id") val id: String = "",
            @SerializedName("title") val title: String = "",
            @SerializedName("cover_small") val coverSmall: String = "",
            @SerializedName("cover_medium") val coverMedium: String = "",
            @SerializedName("cover_big") val coverBig: String = "",
            @SerializedName("cover_xl") val coverXl: String = ""
        )

        /** Converts DTO to domain [Song] model with automatic resolution optimization for slow connections (< 0.8 Mbps) */
        fun toDomainModel(isSlowNetwork: Boolean = false): Song {
            val rawCover = if (isSlowNetwork) {
                album?.coverSmall.takeIf { !it.isNullOrEmpty() }
                    ?: album?.coverMedium.takeIf { !it.isNullOrEmpty() }
                    ?: artist?.pictureSmall.takeIf { !it.isNullOrEmpty() }
                    ?: artist?.pictureMedium ?: ""
            } else {
                album?.coverBig.takeIf { !it.isNullOrEmpty() }
                    ?: album?.coverXl.takeIf { !it.isNullOrEmpty() }
                    ?: album?.coverMedium.takeIf { !it.isNullOrEmpty() }
                    ?: artist?.pictureBig.takeIf { !it.isNullOrEmpty() }
                    ?: artist?.pictureMedium ?: ""
            }

            return Song(
                id = id,
                title = title,
                artistName = artist?.name ?: "Unknown Artist",
                albumTitle = album?.title ?: "",
                coverUrl = if (isSlowNetwork) com.kenjigames.ividsmusic.network.NetworkMonitor.optimizeCoverUrlDirect(rawCover) else rawCover,
                durationSeconds = duration,
                previewUrl = preview
            )
        }
    }
}
