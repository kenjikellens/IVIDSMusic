package com.kenjigames.ividsmusic.ui.screens.profile

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.HistoryEntry
import com.kenjigames.ividsmusic.domain.model.InterestScore
import com.kenjigames.ividsmusic.domain.model.ScoreEntityType
import com.kenjigames.ividsmusic.domain.model.UserProfile
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.domain.port.PreferencesRepository
import com.kenjigames.ividsmusic.domain.port.ScoreRepository
import com.kenjigames.ividsmusic.ui.components.ToastController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for [ProfileScreen] combining listener profile metadata, collection counts,
 * minutes played, last 20 played tracks, and normalized top genre interest bars.
 */
@Immutable
data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val likedCount: Int = 0,
    val playlistCount: Int = 0,
    val minutesPlayed: Long = 0L,
    val recentlyPlayed: List<HistoryEntry> = emptyList(),
    val topGenres: List<Pair<String, Float>> = emptyList(),
    val isEditModalOpen: Boolean = false,
    val editName: String = "",
    val editBio: String = ""
)

/**
 * Aggregates local listener stats, 20-track listening history, top genre weights, and local
 * profile editing via [PreferencesRepository], [LibraryRepository], and [ScoreRepository].
 */
@Stable
class ProfileViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val libraryRepository: LibraryRepository,
    private val scoreRepository: ScoreRepository,
    private val toastController: ToastController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _uiState = MutableStateFlow(ProfileUiState())

    /** Observable listener profile & statistics state consumed by [ProfileScreen]. */
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeProfileData()
    }

    /**
     * Opens or closes the local Edit Profile dialog pre-filled with the saved name and bio.
     */
    fun setEditModalOpen(open: Boolean) {
        val current = _uiState.value.profile
        _uiState.update {
            it.copy(
                isEditModalOpen = open,
                editName = current.displayName,
                editBio = current.bio
            )
        }
    }

    /**
     * Updates the draft listener display name inside the Edit Profile dialog.
     */
    fun onEditNameChanged(name: String) {
        _uiState.update { it.copy(editName = name.take(30)) }
    }

    /**
     * Updates the draft listener bio inside the Edit Profile dialog.
     */
    fun onEditBioChanged(bio: String) {
        _uiState.update { it.copy(editBio = bio.take(120)) }
    }

    /**
     * Persists updated display name and bio locally in DataStore via [PreferencesRepository].
     */
    fun saveProfile() {
        val name = _uiState.value.editName.trim().ifEmpty { "Listener" }
        val bio = _uiState.value.editBio.trim()
        _uiState.update { it.copy(isEditModalOpen = false) }
        scope.launch {
            when (val res = preferencesRepository.updateProfile(name, bio)) {
                is AppResult.Success -> toastController.show("Profile saved")
                is AppResult.Failure -> toastController.show(res.error.message, isError = true)
            }
        }
    }

    /**
     * Cancels active flow observers when disposed.
     */
    fun dispose() {
        scope.cancel()
    }

    /**
     * Combines profile, liked count, playlists count, 20-item history, and genre scores reactively.
     */
    private fun observeProfileData() {
        scope.launch {
            combine(
                preferencesRepository.profile,
                libraryRepository.observeLikedTracks(),
                libraryRepository.observePlaylists(),
                libraryRepository.observeHistory(),
                scoreRepository.observeAllScores()
            ) { profile, liked, playlists, history, scores ->
                val genreScores = scores
                    .filter { it.entityType == ScoreEntityType.GENRE && it.score > 0 }
                    .sortedByDescending { it.score }
                    .take(8)
                val maxScore = genreScores.maxOfOrNull { it.score }?.coerceAtLeast(1) ?: 1
                val normalizedGenres = genreScores.map { entry ->
                    entry.displayName to (entry.score.toFloat() / maxScore.toFloat()).coerceIn(0.08f, 1f)
                }
                ProfileUiState(
                    profile = profile,
                    likedCount = liked.size,
                    playlistCount = playlists.size,
                    minutesPlayed = profile.totalListeningSeconds / 60L,
                    recentlyPlayed = history.take(20),
                    topGenres = normalizedGenres,
                    isEditModalOpen = _uiState.value.isEditModalOpen,
                    editName = _uiState.value.editName,
                    editBio = _uiState.value.editBio
                )
            }.collectLatest { nextState ->
                _uiState.value = nextState
            }
        }
    }
}
