package com.kenjigames.ividsmusic.ui.screens.library

import androidx.compose.runtime.Immutable
import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.HistoryEntry
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.ui.components.ToastController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Navigation tabs displayed inside [LibraryScreen].
 */
enum class LibraryTab {
    LIKED_SONGS,
    SAVED_ALBUMS,
    PLAYLISTS
}

/**
 * Immutable UI state for [LibraryScreen] holding filtered liked tracks, saved albums, custom
 * playlists, and recent listening history.
 */
@Immutable
data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.LIKED_SONGS,
    val searchQuery: String = "",
    val likedTracks: List<Track> = emptyList(),
    val savedAlbums: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val recentlyListened: List<HistoryEntry> = emptyList(),
    val isCreatePlaylistOpen: Boolean = false
) {
    /** Liked tracks filtered dynamically by [searchQuery]. */
    val filteredLikedTracks: List<Track>
        get() = if (searchQuery.isBlank()) {
            likedTracks
        } else {
            likedTracks.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
            }
        }

    /** Saved albums filtered dynamically by [searchQuery]. */
    val filteredSavedAlbums: List<Album>
        get() = if (searchQuery.isBlank()) {
            savedAlbums
        } else {
            savedAlbums.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
            }
        }

    /** Custom playlists filtered dynamically by [searchQuery]. */
    val filteredPlaylists: List<Playlist>
        get() = if (searchQuery.isBlank()) {
            playlists
        } else {
            playlists.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
}

/**
 * Observes local SQLite collections ([LibraryRepository]) and drives real-time filtering,
 * history clearing, and playlist creation inside [LibraryScreen].
 */
class LibraryViewModel(
    private val libraryRepository: LibraryRepository,
    private val toastController: ToastController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _uiState = MutableStateFlow(LibraryUiState())

    /** Observable library UI state consumed by [LibraryScreen]. */
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        observeLibraryCollections()
    }

    /**
     * Switches the active [LibraryTab] (`LIKED_SONGS`, `SAVED_ALBUMS`, or `PLAYLISTS`).
     */
    fun selectTab(tab: LibraryTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    /**
     * Updates the real-time client-side library search query.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Shows or hides the Create Playlist modal dialog.
     */
    fun setCreatePlaylistDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isCreatePlaylistOpen = visible) }
    }

    /**
     * Creates a new custom playlist with [title] and [description] in [LibraryRepository].
     */
    fun createPlaylist(title: String, description: String) {
        _uiState.update { it.copy(isCreatePlaylistOpen = false) }
        scope.launch {
            when (val res = libraryRepository.createPlaylist(title, description)) {
                is AppResult.Success -> toastController.show("Created ${res.data.title}")
                is AppResult.Failure -> toastController.show(res.error.message, isError = true)
            }
        }
    }

    /**
     * Clears all listening history entries in local SQLite storage.
     */
    fun clearHistory() {
        scope.launch {
            libraryRepository.clearHistory()
        }
    }

    /**
     * Cancels active observation coroutines when disposed.
     */
    fun dispose() {
        scope.cancel()
    }

    /**
     * Combines liked tracks, saved albums, playlists, and recent history flows into [_uiState].
     */
    private fun observeLibraryCollections() {
        scope.launch {
            combine(
                libraryRepository.observeLikedTracks(),
                libraryRepository.observeSavedAlbums(),
                libraryRepository.observePlaylists(),
                libraryRepository.observeHistory()
            ) { liked, albums, playlists, history ->
                arrayOf<Any>(liked, albums, playlists, history)
            }.collectLatest { tuple ->
                @Suppress("UNCHECKED_CAST")
                _uiState.update {
                    it.copy(
                        likedTracks = tuple[0] as List<Track>,
                        savedAlbums = tuple[1] as List<Album>,
                        playlists = tuple[2] as List<Playlist>,
                        recentlyListened = tuple[3] as List<HistoryEntry>
                    )
                }
            }
        }
    }
}
