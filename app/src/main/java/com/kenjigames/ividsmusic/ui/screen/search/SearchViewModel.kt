package com.kenjigames.ividsmusic.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.Artist
import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI State for Search screen with categorized results: Songs -> Albums -> Artists */
data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val errorMessage: String? = null
) {
    val isEmpty: Boolean get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty()
}

/** ViewModel powering Search screen */
class SearchViewModel(
    private val musicRepository: MusicRepository = MusicRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _uiState.update {
                it.copy(
                    songs = emptyList(),
                    albums = emptyList(),
                    artists = emptyList(),
                    isLoading = false
                )
            }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350) // Debounce search
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            musicRepository.searchTracks(newQuery, limit = 30).fold(
                onSuccess = { trackList ->
                    // Extract unique songs
                    val songs = trackList

                    // Extract unique albums from track list
                    val albums = trackList
                        .filter { it.albumTitle.isNotEmpty() }
                        .distinctBy { it.albumTitle }
                        .map { song ->
                            Album(
                                id = "album_${song.id}",
                                title = song.albumTitle,
                                artistName = song.artistName,
                                coverUrl = song.coverUrl
                            )
                        }

                    // Extract unique artists from track list
                    val artists = trackList
                        .filter { it.artistName.isNotEmpty() && it.artistName != "Unknown Artist" }
                        .distinctBy { it.artistName }
                        .map { song ->
                            Artist(
                                id = "artist_${song.id}",
                                name = song.artistName,
                                imageUrl = song.coverUrl
                            )
                        }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            songs = songs,
                            albums = albums,
                            artists = artists
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }
}
