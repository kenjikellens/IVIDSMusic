package com.kenjigames.ividsmusic.ui.screen.song

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kenjigames.ividsmusic.data.AppDatabase
import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.player.PlaybackManager
import com.kenjigames.ividsmusic.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI State for SongDetail screen */
data class SongDetailUiState(
    val song: Song? = null,
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false
)

/** ViewModel powering the SongDetail (Track Information & Full Player) screen */
class SongDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SongDetailUiState())
    val uiState: StateFlow<SongDetailUiState> = _uiState.asStateFlow()

    private var libraryRepository: LibraryRepository? = null

    init {
        viewModelScope.launch {
            try {
                val db = AppDatabase.getInstance(getApplication())
                libraryRepository = LibraryRepository(db.trackDao())
            } catch (e: Exception) {
                // Ignore init error
            }
        }
        viewModelScope.launch {
            PlaybackManager.instance.playerState.collect { playerState ->
                playerState.currentSong?.let { song ->
                    _uiState.update {
                        it.copy(
                            song = song,
                            isLiked = song.isLiked,
                            isDownloaded = song.isDownloaded
                        )
                    }
                }
            }
        }
    }

    fun toggleLike() {
        val current = _uiState.value.song ?: return
        viewModelScope.launch {
            val updated = current.copy(isLiked = !current.isLiked)
            _uiState.update { it.copy(isLiked = updated.isLiked) }
            if (updated.isLiked) {
                libraryRepository?.saveTrack(updated)
            } else {
                libraryRepository?.deleteTrack(updated)
            }
        }
    }
}
