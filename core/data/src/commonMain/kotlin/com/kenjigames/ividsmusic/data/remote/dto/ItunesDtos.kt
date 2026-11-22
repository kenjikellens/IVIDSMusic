package com.kenjigames.ividsmusic.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Envelope returned by Apple iTunes Search and Lookup endpoints.
 */
@Serializable
data class ItunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ItunesItemDto> = emptyList()
)

/**
 * Unified iTunes catalog entry covering songs, artists, and collections returned by `/search` and `/lookup`.
 */
@Serializable
data class ItunesItemDto(
    val wrapperType: String? = null,
    val kind: String? = null,
    val trackId: Long? = null,
    val trackName: String? = null,
    val artistId: Long? = null,
    val artistName: String? = null,
    val collectionId: Long? = null,
    val collectionName: String? = null,
    val artworkUrl100: String? = null,
    val previewUrl: String? = null,
    val trackTimeMillis: Long? = null,
    val trackCount: Int? = null,
    val primaryGenreName: String? = null,
    val releaseDate: String? = null
)
