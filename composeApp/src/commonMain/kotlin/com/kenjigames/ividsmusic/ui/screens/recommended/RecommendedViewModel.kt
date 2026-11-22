package com.kenjigames.ividsmusic.ui.screens.recommended

import androidx.compose.runtime.Immutable
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.InterestScore
import com.kenjigames.ividsmusic.domain.model.RecommendationFeed
import com.kenjigames.ividsmusic.domain.model.ScoreEntityType
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.domain.port.ScoreRepository
import com.kenjigames.ividsmusic.domain.usecase.RecommendationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for [RecommendedScreen] containing the personalized [RecommendationFeed],
 * top interest chips, and loading/error status.
 */
@Immutable
data class RecommendedUiState(
    val feed: RecommendationFeed = RecommendationFeed(
        discoveryMix = emptyList(),
        genreFusionTitle = null,
        genreFusionTracks = emptyList(),
        tasteMatchTitle = null,
        tasteMatchTracks = emptyList(),
        hasHistory = false
    ),
    val topInterests: List<InterestScore> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Triggers cosine-similarity personalized discovery via [RecommendationEngine] and surfaces
 * local interest weights from [ScoreRepository].
 */
class RecommendedViewModel(
    private val recommendationEngine: RecommendationEngine,
    private val scoreRepository: ScoreRepository,
    private val libraryRepository: LibraryRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _uiState = MutableStateFlow(RecommendedUiState())

    /** Observable personalized recommendation feed consumed by [RecommendedScreen]. */
    val uiState: StateFlow<RecommendedUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /**
     * Recomputes personalized recommendations from local SQLite interest scores and downloaded IDs.
     */
    fun refresh() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val scores = scoreRepository.getAllScores()
                .filter { it.score > 0 && (it.entityType == ScoreEntityType.GENRE || it.entityType == ScoreEntityType.ARTIST) }
                .sortedByDescending { it.score }
                .take(10)
            val downloadedIds = libraryRepository.observeDownloadedTracks().first().map { it.id }.toSet()

            when (val res = recommendationEngine.generateRecommendations(savedTrackIds = downloadedIds)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        feed = res.data,
                        topInterests = scores,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(
                        topInterests = scores,
                        isLoading = false,
                        errorMessage = res.error.message
                    )
                }
            }
        }
    }

    /**
     * Cancels active recommendation computations when disposed.
     */
    fun dispose() {
        scope.cancel()
    }
}
