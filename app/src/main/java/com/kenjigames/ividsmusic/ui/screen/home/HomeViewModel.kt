package com.kenjigames.ividsmusic.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State object for the Home screen UI */
data class HomeUiState(
    val isLoading: Boolean = true,
    val genreTracks: Map<String, List<Song>> = emptyMap(),
    val recommendedSongs: List<Song> = emptyList(),
    val errorMessage: String? = null
)

/** ViewModel powering the Home screen */
class HomeViewModel(
    private val musicRepository: MusicRepository = MusicRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeContent()
    }

    fun loadHomeContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val chartResult = musicRepository.getGenreChart(20)
            chartResult.fold(
                onSuccess = { songs ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            recommendedSongs = songs,
                            genreTracks = mapOf(
                                "Pop" to songs.take(6),
                                "Rock" to songs.drop(4).take(6),
                                "Hip-Hop" to songs.drop(8).take(6)
                            )
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
