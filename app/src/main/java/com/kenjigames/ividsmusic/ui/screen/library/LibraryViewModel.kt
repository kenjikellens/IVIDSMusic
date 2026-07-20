package com.kenjigames.ividsmusic.ui.screen.library

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kenjigames.ividsmusic.data.AppDatabase
import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI State for Library screen */
data class LibraryUiState(
    val likedSongs: List<Song> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

/** ViewModel powering Library screen */
class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var repository: LibraryRepository? = null

    init {
        viewModelScope.launch {
            try {
                val db = AppDatabase.getInstance(getApplication())
                val repo = LibraryRepository(db.trackDao())
                repository = repo
                repo.savedTracks.collect { songs ->
                    _uiState.update { it.copy(likedSongs = songs) }
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Init error: ${e.message}")
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun deleteTrack(song: Song) {
        viewModelScope.launch {
            try {
                repository?.deleteTrack(song)
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Delete error: ${e.message}")
            }
        }
    }
}
