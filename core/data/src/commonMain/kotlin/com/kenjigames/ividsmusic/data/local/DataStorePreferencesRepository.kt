package com.kenjigames.ividsmusic.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kenjigames.ividsmusic.domain.model.AppResult
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.domain.model.DomainError
import com.kenjigames.ividsmusic.domain.model.LayoutMode
import com.kenjigames.ividsmusic.domain.model.UpdateMode
import com.kenjigames.ividsmusic.domain.model.UserPreferences
import com.kenjigames.ividsmusic.domain.model.UserProfile
import com.kenjigames.ividsmusic.domain.port.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

/**
 * Multiplatform DataStore Preferences implementation of [PreferencesRepository] storing settings and local profile.
 * Also persists optional custom download folder overrides without sending any external telemetry.
 */
class DataStorePreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    private val safeData: Flow<Preferences> = dataStore.data.catch { throwable ->
        if (throwable is CancellationException) throw throwable
        emit(emptyPreferences())
    }

    /** Reactive [UserPreferences] flow reflecting quality, UI scale, language, tint, update, and layout settings. */
    override val preferences: Flow<UserPreferences> = safeData.map { prefs ->
        val rawQuality = prefs[KEY_DOWNLOAD_QUALITY] ?: AudioQuality.Q196.kbps
        val rawScale = prefs[KEY_UI_SCALE] ?: DEFAULT_UI_SCALE
        val rawUpdateMode = prefs[KEY_UPDATE_MODE]?.let { name ->
            UpdateMode.entries.firstOrNull { it.name == name }
        } ?: UpdateMode.AUTO
        val rawAutoUpdate = prefs[KEY_AUTO_UPDATE_ENABLED] ?: (rawUpdateMode == UpdateMode.AUTO)
        val rawLayoutMode = prefs[KEY_LAYOUT_MODE]?.let { name ->
            LayoutMode.entries.firstOrNull { it.name == name }
        } ?: LayoutMode.AUTO

        UserPreferences(
            downloadQuality = AudioQuality.fromKbps(rawQuality),
            uiScale = clampScale(rawScale),
            languageCode = prefs[KEY_LANGUAGE_CODE]?.takeIf { it.isNotBlank() } ?: DEFAULT_LANGUAGE_CODE,
            dynamicTintEnabled = prefs[KEY_DYNAMIC_TINT_ENABLED] ?: true,
            autoUpdateEnabled = rawAutoUpdate,
            updateMode = rawUpdateMode,
            layoutMode = rawLayoutMode
        )
    }

    /** Reactive [UserProfile] flow holding local display name, bio, and total listening time in seconds. */
    override val profile: Flow<UserProfile> = safeData.map { prefs ->
        UserProfile(
            displayName = prefs[KEY_PROFILE_NAME]?.takeIf { it.isNotBlank() } ?: DEFAULT_DISPLAY_NAME,
            bio = prefs[KEY_PROFILE_BIO] ?: "",
            totalListeningSeconds = (prefs[KEY_LISTENING_SECONDS] ?: 0L).coerceAtLeast(0L)
        )
    }

    /** Persists the preferred offline [AudioQuality] bitrate into DataStore. */
    override suspend fun setDownloadQuality(quality: AudioQuality): AppResult<Unit> = editPrefs { prefs ->
        prefs[KEY_DOWNLOAD_QUALITY] = quality.kbps
    }

    /** Persists UI scale factor clamped within `0.75f..1.50f` rounded to 5% increments. */
    override suspend fun setUiScale(scale: Float): AppResult<Unit> = editPrefs { prefs ->
        prefs[KEY_UI_SCALE] = clampScale(scale)
    }

    /** Persists active localization language [code] after trimming whitespace. */
    override suspend fun setLanguageCode(code: String): AppResult<Unit> {
        val normalized = code.trim().lowercase()
        if (normalized.isEmpty()) {
            return AppResult.Failure(DomainError.Validation("Language code cannot be blank"))
        }
        return editPrefs { prefs -> prefs[KEY_LANGUAGE_CODE] = normalized }
    }

    /** Enables or disables dynamic album-art ambient tinting in DataStore. */
    override suspend fun setDynamicTintEnabled(enabled: Boolean): AppResult<Unit> = editPrefs { prefs ->
        prefs[KEY_DYNAMIC_TINT_ENABLED] = enabled
    }

    /** Updates auto-update flag and aligns [UpdateMode] between `AUTO` and `MANUAL`. */
    override suspend fun setAutoUpdateEnabled(enabled: Boolean): AppResult<Unit> = editPrefs { prefs ->
        prefs[KEY_AUTO_UPDATE_ENABLED] = enabled
        val existing = prefs[KEY_UPDATE_MODE]?.let { name -> UpdateMode.entries.firstOrNull { it.name == name } }
        val nextMode = when {
            enabled -> UpdateMode.AUTO
            existing == UpdateMode.OFF || existing == UpdateMode.DEVELOPER -> existing
            else -> UpdateMode.MANUAL
        }
        prefs[KEY_UPDATE_MODE] = nextMode.name
    }

    /** Persists [UpdateMode] and synchronizes `autoUpdateEnabled` accordingly. */
    override suspend fun setUpdateMode(mode: UpdateMode): AppResult<Unit> = editPrefs { prefs ->
        prefs[KEY_UPDATE_MODE] = mode.name
        prefs[KEY_AUTO_UPDATE_ENABLED] = (mode == UpdateMode.AUTO)
    }

    /** Persists the shell [LayoutMode] preference in DataStore. */
    override suspend fun setLayoutMode(mode: LayoutMode): AppResult<Unit> = editPrefs { prefs ->
        prefs[KEY_LAYOUT_MODE] = mode.name
    }

    /** Updates local profile [displayName] (max 30 chars) and [bio] (max 120 chars). */
    override suspend fun updateProfile(displayName: String, bio: String): AppResult<Unit> {
        val cleanName = displayName.trim().take(MAX_NAME_LENGTH).ifEmpty { DEFAULT_DISPLAY_NAME }
        val cleanBio = bio.trim().take(MAX_BIO_LENGTH)
        return editPrefs { prefs ->
            prefs[KEY_PROFILE_NAME] = cleanName
            prefs[KEY_PROFILE_BIO] = cleanBio
        }
    }

    /** Increments cumulative listening duration by [seconds] when strictly positive. */
    override suspend fun addListeningSeconds(seconds: Long): AppResult<Unit> {
        if (seconds <= 0L) return AppResult.Success(Unit)
        return editPrefs { prefs ->
            val current = (prefs[KEY_LISTENING_SECONDS] ?: 0L).coerceAtLeast(0L)
            prefs[KEY_LISTENING_SECONDS] = current + seconds
        }
    }

    /** Reads any user-configured custom offline save directory path persisted in DataStore, or `null`. */
    suspend fun getCustomSaveDirectory(): String? =
        safeData.first()[KEY_CUSTOM_SAVE_DIR]?.takeIf { it.isNotBlank() }

    /** Persists a custom offline save directory override [path] in DataStore. */
    suspend fun setCustomSaveDirectory(path: String): AppResult<Unit> = editPrefs { prefs ->
        prefs[KEY_CUSTOM_SAVE_DIR] = path
    }

    private suspend inline fun editPrefs(
        crossinline block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit
    ): AppResult<Unit> = try {
        dataStore.edit { prefs -> block(prefs) }
        AppResult.Success(Unit)
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (error: Exception) {
        AppResult.Failure(DomainError.Storage(error.message ?: "Failed to update DataStore preferences"))
    }

    private fun clampScale(raw: Float): Float {
        val clamped = raw.coerceIn(MIN_UI_SCALE, MAX_UI_SCALE)
        return (clamped * 20f).roundToInt() / 20f
    }

    companion object {
        const val MIN_UI_SCALE = 0.75f
        const val MAX_UI_SCALE = 1.50f
        const val DEFAULT_UI_SCALE = 1.0f
        const val DEFAULT_LANGUAGE_CODE = "en"
        const val DEFAULT_DISPLAY_NAME = "Listener"
        private const val MAX_NAME_LENGTH = 30
        private const val MAX_BIO_LENGTH = 120

        private val KEY_DOWNLOAD_QUALITY = intPreferencesKey("download_quality_kbps")
        private val KEY_UI_SCALE = floatPreferencesKey("ui_scale_factor")
        private val KEY_LANGUAGE_CODE = stringPreferencesKey("language_code")
        private val KEY_DYNAMIC_TINT_ENABLED = booleanPreferencesKey("dynamic_tint_enabled")
        private val KEY_AUTO_UPDATE_ENABLED = booleanPreferencesKey("auto_update_enabled")
        private val KEY_UPDATE_MODE = stringPreferencesKey("update_mode")
        private val KEY_LAYOUT_MODE = stringPreferencesKey("layout_mode")
        private val KEY_PROFILE_NAME = stringPreferencesKey("profile_display_name")
        private val KEY_PROFILE_BIO = stringPreferencesKey("profile_bio")
        private val KEY_LISTENING_SECONDS = longPreferencesKey("total_listening_seconds")
        private val KEY_CUSTOM_SAVE_DIR = stringPreferencesKey("custom_save_directory")
    }
}
