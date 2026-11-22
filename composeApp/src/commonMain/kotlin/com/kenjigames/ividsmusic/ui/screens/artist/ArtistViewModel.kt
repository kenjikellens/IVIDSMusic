package com.kenjigames.ividsmusic.ui.screens.artist

import androidx.compose.runtime.Immutable
import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.Artist
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.MusicDiscoveryRepository
import com.kenjigames.ividsmusic.domain.usecase.RecommendationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for [ArtistDetailScreen] holding artist profile, top tracks, albums,
 * and related artists.
 */
@Immutable
data class ArtistUiState(
    val artist: Artist? = null,
    val topTracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val relatedArtists: List<Artist> = emptyList(),
    val isLoading: Boolean = true,
    val isLikedArtist: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Loads artist profile metadata, popular songs, discography albums, and related peers for
 * [ArtistDetailScreen] while recording artist discovery affinity in [RecommendationEngine].
 */
class ArtistViewModel(
    private val artistId: String,
    private val artistNameHint: String,
    private val discoveryRepository: MusicDiscoveryRepository,
    private val recommendationEngine: RecommendationEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _uiState = MutableStateFlow(ArtistUiState())

    /** Observable artist detail payload consumed by [ArtistDetailScreen]. */
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * Fetches artist details from [MusicDiscoveryRepository] and logs an artist click score.
     */
    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = discoveryRepository.getArtistDetails(artistId, artistNameHint)) {
                is AppResult.Success -> {
                    val details = res.data
                    _uiState.update {
                        it.copy(
                            artist = details.artist,
                            topTracks = details.topTracks,
                            albums = details.albums,
                            relatedArtists = details.relatedArtists,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    recommendationEngine.recordArtistClick(details.artist.id, details.artist.name)
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.error.message) }
                }
            }
        }
    }

    /**
     * Records a +3 local interest score for the artist when the user likes them.
     */
    fun likeArtist() {
        val artist = _uiState.value.artist ?: return
        _uiState.update { it.copy(isLikedArtist = true) }
        scope.launch {
            recommendationEngine.recordLikeArtist(artist)
        }
    }

    /**
     * Cancels active coroutines when the route exits the back-stack.
     */
    fun dispose() {
        scope.cancel()
    }
}
