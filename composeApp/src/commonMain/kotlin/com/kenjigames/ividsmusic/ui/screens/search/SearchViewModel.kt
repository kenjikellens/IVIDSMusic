package com.kenjigames.ividsmusic.ui.screens.search

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.Artist
import com.kenjigames.ividsmusic.domain.model.SearchFilterType
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.MusicDiscoveryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for [SearchScreen] supporting debounced queries, [SearchFilterType] tabs,
 * release-year range filtering, and offset pagination.
 */
@Immutable
data class SearchUiState(
    val query: String = "",
    val filterType: SearchFilterType = SearchFilterType.ALL,
    val yearFilterEnabled: Boolean = false,
    val yearRange: ClosedFloatingPointRange<Float> = 1970f..2026f,
    val artists: List<Artist> = emptyList(),
    val songs: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Coordinates debounced Deezer/iTunes searches, category tabs, year filtering, and pagination
 * for [SearchScreen].
 */
@Stable
class SearchViewModel(
    private val discoveryRepository: MusicDiscoveryRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _uiState = MutableStateFlow(SearchUiState())

    /** Observable search query, active filters, and result lists. */
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var debounceJob: Job? = null
    private var currentOffset = 0

    /**
     * Updates the query string and triggers a debounced search after 350ms (or resets when blank).
     */
    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        debounceJob?.cancel()
        if (newQuery.isBlank()) {
            _uiState.update {
                it.copy(
                    artists = emptyList(),
                    songs = emptyList(),
                    albums = emptyList(),
                    isLoading = false,
                    canLoadMore = false,
                    errorMessage = null
                )
            }
            return
        }
        debounceJob = scope.launch {
            delay(350L)
            executeSearch(append = false)
        }
    }

    /**
     * Applies an explicit [query] immediately without debounce (e.g. from category cards or Ctrl+K).
     */
    fun submitQuery(query: String) {
        debounceJob?.cancel()
        _uiState.update { it.copy(query = query) }
        if (query.isNotBlank()) {
            scope.launch { executeSearch(append = false) }
        }
    }

    /**
     * Switches the active [SearchFilterType] chip and refreshes search results.
     */
    fun setFilterType(type: SearchFilterType) {
        if (_uiState.value.filterType == type) return
        _uiState.update { it.copy(filterType = type) }
        if (_uiState.value.query.isNotBlank()) {
            scope.launch { executeSearch(append = false) }
        }
    }

    /**
     * Toggles release-year range filtering and re-runs the active search when a query exists.
     */
    fun setYearFilterEnabled(enabled: Boolean) {
        _uiState.update { it.copy(yearFilterEnabled = enabled) }
        if (_uiState.value.query.isNotBlank()) {
            scope.launch { executeSearch(append = false) }
        }
    }

    /**
     * Updates the dual-thumb release year [range] (`1950f..2026f`) and triggers search refresh.
     */
    fun setYearRange(range: ClosedFloatingPointRange<Float>) {
        _uiState.update { it.copy(yearRange = range, yearFilterEnabled = true) }
        debounceJob?.cancel()
        if (_uiState.value.query.isNotBlank()) {
            debounceJob = scope.launch {
                delay(280L)
                executeSearch(append = false)
            }
        }
    }

    /**
     * Loads the next paginated batch for single-category filtered search views.
     */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore || state.query.isBlank()) return
        scope.launch { executeSearch(append = true) }
    }

    /**
     * Cancels active search jobs on disposal.
     */
    fun dispose() {
        debounceJob?.cancel()
        scope.cancel()
    }

    /**
     * Executes overview or category-paginated search queries via [MusicDiscoveryRepository].
     */
    private suspend fun executeSearch(append: Boolean) {
        val state = _uiState.value
        val trimmed = state.query.trim()
        if (trimmed.isEmpty()) return

        val yearRange = if (state.yearFilterEnabled) {
            state.yearRange.start.toInt()..state.yearRange.endInclusive.toInt()
        } else {
            null
        }

        if (!append) {
            currentOffset = 0
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        } else {
            _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
        }

        when (state.filterType) {
            SearchFilterType.ALL -> {
                when (val res = discoveryRepository.searchOverview(trimmed, limitPerSection = 12, yearRange = yearRange)) {
                    is AppResult.Success -> _uiState.update {
                        it.copy(
                            artists = res.data.artists,
                            songs = res.data.songs,
                            albums = res.data.albums,
                            isLoading = false,
                            isLoadingMore = false,
                            canLoadMore = false,
                            errorMessage = null
                        )
                    }
                    is AppResult.Failure -> _uiState.update {
                        it.copy(isLoading = false, isLoadingMore = false, errorMessage = res.error.message)
                    }
                }
            }
            SearchFilterType.SONG -> {
                val nextOffset = if (append) currentOffset + PAGE_SIZE else 0
                when (val res = discoveryRepository.searchTracks(trimmed, limit = PAGE_SIZE, offset = nextOffset, yearRange = yearRange)) {
                    is AppResult.Success -> {
                        currentOffset = nextOffset
                        _uiState.update {
                            val merged = if (append) (it.songs + res.data).distinctBy { t -> t.id } else res.data
                            it.copy(
                                songs = merged,
                                artists = emptyList(),
                                albums = emptyList(),
                                isLoading = false,
                                isLoadingMore = false,
                                canLoadMore = res.data.size >= PAGE_SIZE
                            )
                        }
                    }
                    is AppResult.Failure -> _uiState.update {
                        it.copy(isLoading = false, isLoadingMore = false, errorMessage = res.error.message)
                    }
                }
            }
            SearchFilterType.ARTIST -> {
                val nextOffset = if (append) currentOffset + PAGE_SIZE else 0
                when (val res = discoveryRepository.searchArtists(trimmed, limit = PAGE_SIZE, offset = nextOffset)) {
                    is AppResult.Success -> {
                        currentOffset = nextOffset
                        _uiState.update {
                            val merged = if (append) (it.artists + res.data).distinctBy { a -> a.id } else res.data
                            it.copy(
                                artists = merged,
                                songs = emptyList(),
                                albums = emptyList(),
                                isLoading = false,
                                isLoadingMore = false,
                                canLoadMore = res.data.size >= PAGE_SIZE
                            )
                        }
                    }
                    is AppResult.Failure -> _uiState.update {
                        it.copy(isLoading = false, isLoadingMore = false, errorMessage = res.error.message)
                    }
                }
            }
            SearchFilterType.ALBUM -> {
                val nextOffset = if (append) currentOffset + PAGE_SIZE else 0
                when (val res = discoveryRepository.searchAlbums(trimmed, limit = PAGE_SIZE, offset = nextOffset, yearRange = yearRange)) {
                    is AppResult.Success -> {
                        currentOffset = nextOffset
                        _uiState.update {
                            val merged = if (append) (it.albums + res.data).distinctBy { a -> a.id } else res.data
                            it.copy(
                                albums = merged,
                                songs = emptyList(),
                                artists = emptyList(),
                                isLoading = false,
                                isLoadingMore = false,
                                canLoadMore = res.data.size >= PAGE_SIZE
                            )
                        }
                    }
                    is AppResult.Failure -> _uiState.update {
                        it.copy(isLoading = false, isLoadingMore = false, errorMessage = res.error.message)
                    }
                }
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 24
    }
}
