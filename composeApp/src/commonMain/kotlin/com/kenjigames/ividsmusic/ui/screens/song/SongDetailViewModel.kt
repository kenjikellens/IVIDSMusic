package com.kenjigames.ividsmusic.ui.screens.song

import androidx.compose.runtime.Immutable
import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.AppResult
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for [SongDetailScreen] holding detailed track metadata, related tracks,
 * and artist discography shelves.
 */
@Immutable
data class SongDetailUiState(
    val track: Track? = null,
    val artistAlbums: List<Album> = emptyList(),
    val similarTracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Loads enriched track details, similar tracks, and artist discography for [SongDetailScreen].
 */
class SongDetailViewModel(
    private val trackId: String,
    private val discoveryRepository: MusicDiscoveryRepository,
    private val libraryRepository: LibraryRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _uiState = MutableStateFlow(SongDetailUiState())

    /** Observable track detail state rendered by [SongDetailScreen]. */
    val uiState: StateFlow<SongDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Fetches track details from remote API (falling back to SQLite if offline), related tracks,
     * and artist albums concurrently.
     */
    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val localTrack = libraryRepository.getTrack(trackId)
            val remoteRes = discoveryRepository.getTrackDetails(trackId)
            val resolvedTrack = remoteRes.getOrNull() ?: localTrack

            if (resolvedTrack == null) {
                val msg = (remoteRes as? AppResult.Failure)?.error?.message ?: "Track unavailable"
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                return@launch
            }

            val relatedJob = async { discoveryRepository.getRelatedTracks(resolvedTrack.id).getOrNull().orEmpty() }
            val artistJob = async {
                if (resolvedTrack.artistId.isNotBlank() || resolvedTrack.artist.isNotBlank()) {
                    discoveryRepository.getArtistDetails(resolvedTrack.artistId, resolvedTrack.artist).getOrNull()
                } else {
                    null
                }
            }

            val related = relatedJob.await()
            val artistDetails = artistJob.await()

            _uiState.update {
                it.copy(
                    track = resolvedTrack,
                    similarTracks = related,
                    artistAlbums = artistDetails?.albums.orEmpty(),
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
    }

    /**
     * Cancels active coroutines when leaving the route.
     */
    fun dispose() {
        scope.cancel()
    }
}
