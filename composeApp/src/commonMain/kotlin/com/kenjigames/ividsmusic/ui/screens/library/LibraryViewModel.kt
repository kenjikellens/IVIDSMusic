package com.kenjigames.ividsmusic.ui.screens.library

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.HistoryEntry
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.model.TrackSortOption
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
 * Pure helper sorting and filtering [tracks] by text [query], [sortOption], and [offlineOnly].
 */
fun sortAndFilterTracks(
    tracks: List<Track>,
    query: String,
    sortOption: TrackSortOption,
    offlineOnly: Boolean,
    downloadedIds: Set<String>
): List<Track> {
    val trimmed = query.trim()
    val filtered = tracks.filter { track ->
        val matchesQuery = trimmed.isEmpty() ||
            track.title.contains(trimmed, ignoreCase = true) ||
            track.artist.contains(trimmed, ignoreCase = true) ||
            track.album.contains(trimmed, ignoreCase = true)
        val matchesOffline = !offlineOnly || track.isDownloaded || track.id in downloadedIds
        matchesQuery && matchesOffline
    }
    return when (sortOption) {
        TrackSortOption.RECENT -> filtered
        TrackSortOption.TITLE -> filtered.sortedBy { it.title.lowercase() }
        TrackSortOption.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
        TrackSortOption.DURATION -> filtered.sortedByDescending { it.durationSeconds }
    }
}

/**
 * Immutable UI state for [LibraryScreen] holding filtered liked tracks, saved albums, custom
 * playlists, sort/offline filters, and recent listening history.
 */
@Immutable
data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.LIKED_SONGS,
    val searchQuery: String = "",
    val sortOption: TrackSortOption = TrackSortOption.RECENT,
    val offlineOnly: Boolean = false,
    val likedTracks: List<Track> = emptyList(),
    val savedAlbums: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val recentlyListened: List<HistoryEntry> = emptyList(),
    val isCreatePlaylistOpen: Boolean = false
) {
    /** Liked tracks filtered dynamically by [searchQuery], [sortOption], and optional offline filter. */
    fun filteredLikedTracks(downloadedIds: Set<String> = emptySet()): List<Track> =
        sortAndFilterTracks(likedTracks, searchQuery, sortOption, offlineOnly, downloadedIds)

    /** Backward-compatible filtered liked tracks property using track-embedded download flags. */
    val filteredLikedTracks: List<Track>
        get() = filteredLikedTracks(emptySet())

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
@Stable
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
     * Changes the active [TrackSortOption] used for Liked Songs ordering.
     */
    fun setSortOption(option: TrackSortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    /**
     * Toggles whether Liked Songs only displays tracks downloaded for offline playback.
     */
    fun toggleOfflineOnly() {
        _uiState.update { it.copy(offlineOnly = !it.offlineOnly) }
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
     * Typed snapshot aggregating the four SQLite library flows prior to projecting [LibraryUiState].
     */
    private data class LibrarySnapshot(
        val liked: List<Track>,
        val albums: List<Album>,
        val playlists: List<Playlist>,
        val history: List<HistoryEntry>
    )

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
                LibrarySnapshot(liked, albums, playlists, history)
            }.collectLatest { snap ->
                _uiState.update {
                    it.copy(
                        likedTracks = snap.liked,
                        savedAlbums = snap.albums,
                        playlists = snap.playlists,
                        recentlyListened = snap.history
                    )
                }
            }
        }
    }
}
