package com.kenjigames.ividsmusic.ui.screen.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kenjigames.ividsmusic.data.preferences.UserPreferences
import com.kenjigames.ividsmusic.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI State for Settings screen */
data class SettingsUiState(
    val userPreferences: UserPreferences = UserPreferences(),
    val appVersion: String = "v0.2.3 Native",
    val updateStatus: String = "App is up to date"
)

/** ViewModel powering Settings screen */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = UserPreferencesRepository(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.userPreferences.collect { prefs ->
                _uiState.update { it.copy(userPreferences = prefs) }
            }
        }
    }

    fun setUiScale(scale: Float) {
        viewModelScope.launch {
            preferencesRepository.setUiScale(scale)
        }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch {
            preferencesRepository.setLanguageCode(code)
        }
    }
}
