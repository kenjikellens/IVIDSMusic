package com.kenjigames.ividsmusic.domain.usecase

import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.repository.LibraryRepository

/**
 * Use case toggling song liked state in library repository.
 */
class ToggleLikeUseCase(
    private val libraryRepository: LibraryRepository
) {
    /** Toggles like state for a song */
    suspend operator fun invoke(song: Song) {
        if (song.isLiked) {
            libraryRepository.deleteTrack(song)
        } else {
            libraryRepository.saveTrack(song.copy(isLiked = true))
        }
    }
}
