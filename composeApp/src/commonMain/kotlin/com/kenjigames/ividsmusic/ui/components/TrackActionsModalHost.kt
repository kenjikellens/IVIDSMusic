package com.kenjigames.ividsmusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import kotlinx.coroutines.launch

/**
 * Manages modal state for adding any [Track] to an existing or newly created custom playlist.
 */
class TrackPlaylistModalState(
    private val libraryRepository: LibraryRepository,
    private val toastController: ToastController,
    private val scope: kotlinx.coroutines.CoroutineScope
) {
    var pendingTrack by mutableStateOf<Track?>(null)
        private set
    var showCreateDialog by mutableStateOf(false)
        private set

    /**
     * Opens the playlist picker modal targeting [track].
     */
    fun openPicker(track: Track) {
        pendingTrack = track
    }

    /**
     * Dismisses any open playlist picker or creation dialog cleanly.
     */
    fun dismiss() {
        pendingTrack = null
        showCreateDialog = false
    }

    /**
     * Transitions from the playlist picker into the create-playlist dialog while preserving [pendingTrack].
     */
    fun switchToCreate() {
        showCreateDialog = true
    }

    /**
     * Adds [pendingTrack] into [playlist] via [LibraryRepository] and displays a status toast.
     */
    fun selectPlaylist(playlist: Playlist) {
        val target = pendingTrack ?: return
        pendingTrack = null
        scope.launch {
            when (val res = libraryRepository.addTrackToPlaylist(playlist.id, target)) {
                is AppResult.Success -> toastController.show("Added to ${playlist.title}")
                is AppResult.Failure -> toastController.show(res.error.message, isError = true)
            }
        }
    }

    /**
     * Creates a new playlist with [title] and [description] and immediately inserts [pendingTrack] if set.
     */
    fun createAndAdd(title: String, description: String) {
        val target = pendingTrack
        showCreateDialog = false
        pendingTrack = null
        scope.launch {
            when (val created = libraryRepository.createPlaylist(title, description)) {
                is AppResult.Success -> {
                    if (target != null) {
                        libraryRepository.addTrackToPlaylist(created.data.id, target)
                    }
                    toastController.show("Created ${created.data.title}")
                }
                is AppResult.Failure -> toastController.show(created.error.message, isError = true)
            }
        }
    }
}

/**
 * Remembers a [TrackPlaylistModalState] and renders its picker/creation dialogs automatically.
 */
@Composable
fun rememberTrackPlaylistModalState(
    playlists: List<Playlist>,
    libraryRepository: LibraryRepository,
    toastController: ToastController
): TrackPlaylistModalState {
    val scope = rememberCoroutineScope()
    val state = remember(libraryRepository, toastController) {
        TrackPlaylistModalState(libraryRepository, toastController, scope)
    }

    if (state.showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = state::dismiss,
            onCreate = state::createAndAdd
        )
    } else if (state.pendingTrack != null) {
        PlaylistPickerDialog(
            playlists = playlists,
            onSelectPlaylist = state::selectPlaylist,
            onCreateNew = state::switchToCreate,
            onDismiss = state::dismiss
        )
    }
    return state
}
