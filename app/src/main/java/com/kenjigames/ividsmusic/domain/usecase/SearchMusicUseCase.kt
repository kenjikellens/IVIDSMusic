package com.kenjigames.ividsmusic.domain.usecase

import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.repository.MusicRepository

/**
 * Use case orchestrating music search queries across Deezer API and redundant fallbacks.
 */
class SearchMusicUseCase(
    private val musicRepository: MusicRepository = MusicRepository()
) {
    /** Executes search query for music songs */
    suspend operator fun invoke(query: String, limit: Int = 25): Result<List<Song>> {
        if (query.isBlank()) return Result.success(emptyList())
        return musicRepository.searchTracks(query, limit)
    }
}
