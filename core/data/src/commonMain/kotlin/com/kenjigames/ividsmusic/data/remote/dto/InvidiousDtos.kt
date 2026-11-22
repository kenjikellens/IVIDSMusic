package com.kenjigames.ividsmusic.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Invidious `/api/v1/search` video search item containing video ID and duration in seconds.
 */
@Serializable
data class InvidiousSearchVideoDto(
    val type: String? = null,
    val videoId: String = "",
    val title: String = "",
    val author: String = "",
    val lengthSeconds: Int = 0
)

/**
 * Individual adaptive audio/video stream format entry inside Invidious `/api/v1/videos/{id}` responses.
 */
@Serializable
data class InvidiousAdaptiveFormatDto(
    val url: String? = null,
    val type: String? = null,
    val bitrate: String? = null,
    val container: String? = null,
    val encoding: String? = null
)

/**
 * Invidious `/api/v1/videos/{id}` payload exposing available adaptive audio stream formats.
 */
@Serializable
data class InvidiousVideoDetailsDto(
    val videoId: String = "",
    val title: String = "",
    val lengthSeconds: Int = 0,
    val adaptiveFormats: List<InvidiousAdaptiveFormatDto> = emptyList()
)
