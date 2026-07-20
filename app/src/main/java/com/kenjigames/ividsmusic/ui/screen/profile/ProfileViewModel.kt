package com.kenjigames.ividsmusic.ui.screen.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kenjigames.ividsmusic.data.AppDatabase
import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for Profile ("You") screen */
data class ProfileUiState(
    val userName: String = "Listener",
    val userBio: String = "Ad-free music enthusiast",
    val likedCount: Int = 0,
    val playlistsCount: Int = 0,
    val minutesPlayed: Int = 142,
    val recentHistory: List<Song> = emptyList()
)

/** ViewModel powering Profile screen */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val historyRepository = HistoryRepository(AppDatabase.getInstance(application).historyDao())

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            historyRepository.recentHistory.collect { history ->
                _uiState.update { it.copy(recentHistory = history) }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }
}
