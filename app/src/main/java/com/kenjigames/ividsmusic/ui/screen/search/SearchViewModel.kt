package com.kenjigames.ividsmusic.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI State for Search screen */
data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val searchResults: List<Song> = emptyList(),
    val errorMessage: String? = null
)

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
            _uiState.update { it.copy(searchResults = emptyList(), isLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400) // Debounce search
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            musicRepository.searchTracks(newQuery).fold(
                onSuccess = { songs ->
                    _uiState.update { it.copy(isLoading = false, searchResults = songs) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }
}
