package com.kenjigames.ividsmusic.ui.screens.settings

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.LayoutMode
import com.kenjigames.ividsmusic.domain.model.UpdateMode
import com.kenjigames.ividsmusic.domain.port.OfflineDownloadManager
import com.kenjigames.ividsmusic.domain.port.PreferencesRepository
import com.kenjigames.ividsmusic.domain.port.ScoreRepository
import com.kenjigames.ividsmusic.platform.AppUpdateChecker
import com.kenjigames.ividsmusic.platform.AppUpdateState
import com.kenjigames.ividsmusic.platform.PlatformBridge
import com.kenjigames.ividsmusic.ui.components.ToastController
import com.kenjigames.ividsmusic.ui.i18n.LocalizationManager
import com.kenjigames.ividsmusic.ui.i18n.SupportedLanguage
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
import kotlin.math.roundToInt

/**
 * Immutable UI state for [SettingsScreen] covering scale factor (75%..150%), 16-language picker,
 * default download bitrate (`4..320` kbps), dynamic tinting, layout mode, and native app updates.
 */
@Immutable
data class SettingsUiState(
    val uiScale: Float = 1.0f,
    val language: SupportedLanguage = SupportedLanguage.EN,
    val defaultQuality: AudioQuality = AudioQuality.Q196,
    val dynamicTintEnabled: Boolean = true,
    val autoplayEnabled: Boolean = true,
    val audioCacheBytes: Long = 0L,
    val layoutMode: LayoutMode = LayoutMode.AUTO,
    val updateMode: UpdateMode = UpdateMode.AUTO,
    val appVersion: String = "0.2.5",
    val updateState: AppUpdateState = AppUpdateState.Idle,
    val showResetScoresConfirm: Boolean = false,
    val showResetDefaultsConfirm: Boolean = false
)

/**
 * Coordinates persisted user settings ([PreferencesRepository]), instant 16-language hot-swapping
 * ([LocalizationManager]), cache/score resets, and native self-updates ([AppUpdateChecker]).
 */
@Stable
class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val scoreRepository: ScoreRepository,
    private val offlineDownloadManager: OfflineDownloadManager,
    private val localizationManager: LocalizationManager,
    private val appUpdateChecker: AppUpdateChecker,
    private val platformBridge: PlatformBridge,
    private val toastController: ToastController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _uiState = MutableStateFlow(
        SettingsUiState(appVersion = platformBridge.appVersion)
    )

    /** Observable settings UI state consumed by [SettingsScreen]. */
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observePreferencesAndUpdates()
        refreshAudioCacheSize()
    }

    /**
     * Adjusts UI scale factor clamped within `0.75f..1.50f` (75%..150%) and persists it immediately.
     */
    fun setUiScale(scale: Float) {
        val rounded = ((scale.coerceIn(0.75f, 1.50f)) * 100f).roundToInt() / 100f
        scope.launch {
            preferencesRepository.setUiScale(rounded)
        }
    }

    /**
     * Switches the active language across all 16 JSON bundles immediately and saves the preference.
     */
    fun selectLanguage(language: SupportedLanguage) {
        scope.launch {
            localizationManager.setLanguage(language.code)
            preferencesRepository.setLanguageCode(language.code)
            toastController.show("${language.displayName}")
        }
    }

    /**
     * Persists the default [AudioQuality] bitrate (`4..320` kbps) for offline track downloads.
     */
    fun setDefaultDownloadQuality(quality: AudioQuality) {
        scope.launch {
            preferencesRepository.setDownloadQuality(quality)
        }
    }

    /**
     * Enables or disables 450ms dynamic album-art color tinting across backgrounds and accents.
     */
    fun setDynamicTintEnabled(enabled: Boolean) {
        scope.launch {
            preferencesRepository.setDynamicTintEnabled(enabled)
        }
    }

    /**
     * Enables or disables infinite recommendation radio autoplay when the queue ends.
     */
    fun setAutoplayEnabled(enabled: Boolean) {
        scope.launch {
            preferencesRepository.setAutoplayEnabled(enabled)
        }
    }

    /**
     * Overrides the responsive shell [LayoutMode] (`AUTO`, `MOBILE`, `DESKTOP`, or `TV`).
     */
    fun setLayoutMode(mode: LayoutMode) {
        scope.launch {
            preferencesRepository.setLayoutMode(mode)
        }
    }

    /**
     * Updates the application [UpdateMode] strategy (`OFF`, `AUTO`, `MANUAL`, `DEVELOPER`).
     */
    fun setUpdateMode(mode: UpdateMode) {
        scope.launch {
            preferencesRepository.setUpdateMode(mode)
        }
    }

    /**
     * Triggers an immediate GitHub Releases check via [AppUpdateChecker].
     */
    fun checkForUpdates() {
        scope.launch {
            appUpdateChecker.checkForUpdates()
        }
    }

    /**
     * Downloads and launches the platform update package for the currently discovered release.
     */
    fun downloadAndInstallUpdate() {
        scope.launch {
            appUpdateChecker.downloadAndInstallUpdate()
        }
    }

    /**
     * Clears cached streaming audio files, refreshes the displayed cache byte counter, and notifies user.
     */
    fun clearAudioCache() {
        scope.launch {
            when (val res = offlineDownloadManager.clearAudioCache()) {
                is AppResult.Success -> {
                    refreshAudioCacheSize()
                    toastController.show(localizationManager.strings.value["cache_cleared"])
                }
                is AppResult.Failure -> toastController.show(res.error.message, isError = true)
            }
        }
    }

    /**
     * Queries [OfflineDownloadManager.getAudioCacheBytes] and updates [SettingsUiState.audioCacheBytes].
     */
    fun refreshAudioCacheSize() {
        scope.launch {
            val bytes = offlineDownloadManager.getAudioCacheBytes()
            _uiState.update { it.copy(audioCacheBytes = bytes) }
        }
    }

    /**
     * Controls visibility of the destructive score-reset confirmation modal.
     */
    fun setResetScoresConfirmOpen(open: Boolean) {
        _uiState.update { it.copy(showResetScoresConfirm = open) }
    }

    /**
     * Purges all local recommendation interest scores in [ScoreRepository].
     */
    fun confirmResetScores() {
        _uiState.update { it.copy(showResetScoresConfirm = false) }
        scope.launch {
            when (val res = scoreRepository.clearAllScores()) {
                is AppResult.Success -> toastController.show(localizationManager.strings.value["scores_cleared"])
                is AppResult.Failure -> toastController.show(res.error.message, isError = true)
            }
        }
    }

    /**
     * Controls visibility of the restore-default-preferences confirmation modal.
     */
    fun setResetDefaultsConfirmOpen(open: Boolean) {
        _uiState.update { it.copy(showResetDefaultsConfirm = open) }
    }

    /**
     * Restores default UI scale (`100%`), English language, `Q196` bitrate, dynamic tint, and `AUTO` layout.
     */
    fun confirmResetDefaults() {
        _uiState.update { it.copy(showResetDefaultsConfirm = false) }
        scope.launch {
            preferencesRepository.setUiScale(1.0f)
            preferencesRepository.setLanguageCode("en")
            localizationManager.setLanguage("en")
            preferencesRepository.setDownloadQuality(AudioQuality.Q196)
            preferencesRepository.setDynamicTintEnabled(true)
            preferencesRepository.setLayoutMode(LayoutMode.AUTO)
            preferencesRepository.setUpdateMode(UpdateMode.AUTO)
            toastController.show(localizationManager.strings.value["defaults_restored"])
        }
    }

    /**
     * Cancels active observation coroutines when disposed.
     */
    fun dispose() {
        scope.cancel()
    }

    /**
     * Keeps preferences and update checker state synchronized with [_uiState].
     */
    private fun observePreferencesAndUpdates() {
        scope.launch {
            combine(
                preferencesRepository.preferences,
                appUpdateChecker.state
            ) { prefs, update ->
                prefs to update
            }.collectLatest { (prefs, update) ->
                _uiState.update { current ->
                    current.copy(
                        uiScale = prefs.uiScale,
                        language = SupportedLanguage.fromCode(prefs.languageCode),
                        defaultQuality = prefs.downloadQuality,
                        dynamicTintEnabled = prefs.dynamicTintEnabled,
                        autoplayEnabled = prefs.autoplayEnabled,
                        layoutMode = prefs.layoutMode,
                        updateMode = prefs.updateMode,
                        updateState = update
                    )
                }
            }
        }
    }
}
