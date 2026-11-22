package com.kenjigames.ividsmusic.ui.screens.album

import androidx.compose.runtime.Immutable
import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.domain.port.MusicDiscoveryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for [AlbumDetailScreen] containing album header metadata, ordered tracklist,
 * and saved status.
 */
@Immutable
data class AlbumUiState(
    val album: Album? = null,
    val tracks: List<Track> = emptyList(),
    val isSaved: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Loads album metadata and ordered tracks from [MusicDiscoveryRepository] and manages saved album
 * persistence via [LibraryRepository].
 */
class AlbumViewModel(
    private val albumId: String,
    private val discoveryRepository: MusicDiscoveryRepository,
    private val libraryRepository: LibraryRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _uiState = MutableStateFlow(AlbumUiState())

    /** Observable album details and tracklist state. */
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    init {
        observeSavedStatus()
        load()
    }

    /**
     * Fetches album metadata and tracks from [MusicDiscoveryRepository].
     */
    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = discoveryRepository.getAlbumDetails(albumId)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        album = res.data.album,
                        tracks = res.data.tracks,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = res.error.message)
                }
            }
        }
    }

    /**
     * Toggles whether the active album is saved in local SQLite storage.
     */
    fun toggleSaved() {
        val album = _uiState.value.album ?: return
        val nextSaved = !_uiState.value.isSaved
        scope.launch {
            libraryRepository.setAlbumSaved(album, nextSaved)
        }
    }

    /**
     * Cancels active coroutines when leaving the route.
     */
    fun dispose() {
        scope.cancel()
    }

    /**
     * Observes saved album IDs to keep [AlbumUiState.isSaved] synchronized reactively.
     */
    private fun observeSavedStatus() {
        scope.launch {
            libraryRepository.observeSavedAlbums().collectLatest { saved ->
                _uiState.update { it.copy(isSaved = saved.any { a -> a.id == albumId }) }
            }
        }
    }
}
