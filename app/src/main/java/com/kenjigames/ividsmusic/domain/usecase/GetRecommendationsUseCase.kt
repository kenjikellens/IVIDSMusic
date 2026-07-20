package com.kenjigames.ividsmusic.domain.usecase

import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.repository.MusicRepository

/**
 * Use case fetching personalized music recommendations.
 */
class GetRecommendationsUseCase(
    private val musicRepository: MusicRepository = MusicRepository()
) {
    /** Fetches recommended tracks */
    suspend operator fun invoke(limit: Int = 20): Result<List<Song>> {
        return musicRepository.getGenreChart(limit)
    }
}
