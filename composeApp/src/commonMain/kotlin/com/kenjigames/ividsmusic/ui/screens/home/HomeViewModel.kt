package com.kenjigames.ividsmusic.ui.screens.home

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.Artist
import com.kenjigames.ividsmusic.domain.model.CategoryRow
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.model.getOrNull
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.domain.port.MusicDiscoveryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for the Home discovery screen covering featured hero, listening history,
 * genre charts, top artists, and top albums.
 */
@Immutable
data class HomeUiState(
    val heroTrack: Track? = null,
    val continueListening: List<Track> = emptyList(),
    val genreSections: List<CategoryRow> = emptyList(),
    val topArtists: List<Artist> = emptyList(),
    val topAlbums: List<Album> = emptyList(),
    val isLoading: Boolean = true,
    val isHeroDismissed: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Loads curated genre shelves, top artists, top albums, and recent listening history for [HomeScreen].
 */
@Stable
class HomeViewModel(
    private val discoveryRepository: MusicDiscoveryRepository,
    private val libraryRepository: LibraryRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _uiState = MutableStateFlow(HomeUiState())

    /** Observable Home discovery state consumed by [HomeScreen]. */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeHistory()
        refresh()
    }

    /**
     * Fetches curated genre charts alongside featured artists and albums concurrently.
     */
    fun refresh() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val chartsJob = async { discoveryRepository.getGenreCharts() }
            val artistsJob = async { discoveryRepository.searchArtists("Top", limit = 12) }
            val albumsJob = async { discoveryRepository.searchAlbums("Hits", limit = 12) }

            val chartsResult = chartsJob.await()
            val artists = artistsJob.await().getOrNull().orEmpty()
            val albums = albumsJob.await().getOrNull().orEmpty()

            when (chartsResult) {
                is AppResult.Success -> {
                    val rows = chartsResult.data.filter { it.tracks.isNotEmpty() }
                    val featured = rows.firstOrNull()?.tracks?.firstOrNull()
                    _uiState.update {
                        it.copy(
                            heroTrack = featured,
                            genreSections = rows,
                            topArtists = artists,
                            topAlbums = albums,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            topArtists = artists,
                            topAlbums = albums,
                            isLoading = false,
                            errorMessage = chartsResult.error.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Dismisses the featured hero banner for the active session without affecting genre rows.
     */
    fun dismissHero() {
        _uiState.update { it.copy(isHeroDismissed = true) }
    }

    /**
     * Cancels active coroutines when the view model scope is disposed.
     */
    fun dispose() {
        scope.cancel()
    }

    /**
     * Keeps the Continue Listening row synchronized with local 20-item history persistence.
     */
    private fun observeHistory() {
        scope.launch {
            libraryRepository.observeHistory().collectLatest { entries ->
                _uiState.update { it.copy(continueListening = entries.map { entry -> entry.track }) }
            }
        }
    }
}
