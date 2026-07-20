package com.kenjigames.ividsmusic.network.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for video search item returned by Invidious API endpoints.
 */
data class InvidiousVideoDto(
    @SerializedName("videoId") val videoId: String = "",
    @SerializedName("title") val title: String = ""
)

/**
 * DTO for video stream details and adaptive audio format options.
 */
data class InvidiousStreamDetailsDto(
    @SerializedName("adaptiveFormats") val adaptiveFormats: List<AdaptiveFormatDto> = emptyList()
) {
    /** DTO for an individual adaptive audio stream format */
    data class AdaptiveFormatDto(
        @SerializedName("type") val type: String = "",
        @SerializedName("url") val url: String = ""
    )
}
