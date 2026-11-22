package com.kenjigames.ividsmusic.ui.screens.playlist

import androidx.compose.runtime.Immutable
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.Playlist
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for [PlaylistDetailScreen] supporting inline rename/edit, track reordering,
 * track removal, and playlist deletion confirmation.
 */
@Immutable
data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val isEditing: Boolean = false,
    val editTitle: String = "",
    val editDescription: String = "",
    val showDeleteConfirm: Boolean = false,
    val isDeleted: Boolean = false
)

/**
 * Observes a custom [Playlist] by [playlistId] and persists edits, reordering, track removal,
 * and deletion via [LibraryRepository].
 */
class PlaylistDetailViewModel(
    private val playlistId: Long,
    private val libraryRepository: LibraryRepository,
    private val toastController: ToastController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _uiState = MutableStateFlow(PlaylistDetailUiState())

    /** Observable playlist detail state consumed by [PlaylistDetailScreen]. */
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    init {
        observePlaylist()
    }

    /**
     * Toggles inline metadata edit mode pre-populated with current title and description.
     */
    fun setEditing(editing: Boolean) {
        val current = _uiState.value.playlist
        _uiState.update {
            it.copy(
                isEditing = editing,
                editTitle = current?.title.orEmpty(),
                editDescription = current?.description.orEmpty()
            )
        }
    }

    /**
     * Updates the draft playlist title text field during inline editing.
     */
    fun onEditTitleChanged(value: String) {
        _uiState.update { it.copy(editTitle = value) }
    }

    /**
     * Updates the draft playlist description text field during inline editing.
     */
    fun onEditDescriptionChanged(value: String) {
        _uiState.update { it.copy(editDescription = value) }
    }

    /**
     * Persists updated playlist title and description to SQLite via [LibraryRepository].
     */
    fun saveMetadata() {
        val title = _uiState.value.editTitle.trim()
        if (title.isBlank()) return
        val desc = _uiState.value.editDescription.trim()
        _uiState.update { it.copy(isEditing = false) }
        scope.launch {
            when (val res = libraryRepository.renamePlaylist(playlistId, title, desc)) {
                is AppResult.Success -> toastController.show("Playlist updated")
                is AppResult.Failure -> toastController.show(res.error.message, isError = true)
            }
        }
    }

    /**
     * Moves track at [index] one slot up or down (`delta = -1` or `+1`) and persists ordering.
     */
    fun moveTrack(index: Int, delta: Int) {
        val tracks = _uiState.value.playlist?.tracks ?: return
        val target = index + delta
        if (index !in tracks.indices || target !in tracks.indices) return
        val reordered = tracks.toMutableList()
        val moved = reordered.removeAt(index)
        reordered.add(target, moved)
        scope.launch {
            libraryRepository.reorderPlaylistTracks(playlistId, reordered.map { it.id })
        }
    }

    /**
     * Removes [trackId] from this playlist cleanly via [LibraryRepository].
     */
    fun removeTrack(trackId: String) {
        scope.launch {
            libraryRepository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    /**
     * Controls visibility of the delete playlist confirmation modal.
     */
    fun setDeleteConfirmVisible(visible: Boolean) {
        _uiState.update { it.copy(showDeleteConfirm = visible) }
    }

    /**
     * Permanently deletes the playlist from SQLite storage and marks the route for pop navigation.
     */
    fun deletePlaylist() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
        scope.launch {
            when (val res = libraryRepository.deletePlaylist(playlistId)) {
                is AppResult.Success -> {
                    toastController.show("Playlist deleted")
                    _uiState.update { it.copy(isDeleted = true) }
                }
                is AppResult.Failure -> toastController.show(res.error.message, isError = true)
            }
        }
    }

    /**
     * Cancels observation coroutines when the screen exits.
     */
    fun dispose() {
        scope.cancel()
    }

    /**
     * Subscribes to reactive playlist changes from [LibraryRepository.observePlaylist].
     */
    private fun observePlaylist() {
        scope.launch {
            libraryRepository.observePlaylist(playlistId).collectLatest { pl ->
                _uiState.update { it.copy(playlist = pl) }
            }
        }
    }
}
