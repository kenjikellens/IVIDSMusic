package com.kenjigames.ividsmusic.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

/**
 * Data class representing user settings and UI preferences.
 *
 * @property uiScale Interface scale factor (75% = 0.75f, 100% = 1.0f, 150% = 1.5f)
 * @property languageCode ISO language code for internationalization (e.g. "en", "nl")
 * @property autoUpdateEnabled True if app should check GitHub releases automatically
 */
data class UserPreferences(
    val uiScale: Float = 1.0f,
    val languageCode: String = "en",
    val autoUpdateEnabled: Boolean = true
)

/**
 * Repository wrapping Jetpack DataStore for asynchronous preference persistence.
 */
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val UI_SCALE = floatPreferencesKey("ui_scale")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val AUTO_UPDATE = booleanPreferencesKey("auto_update")
    }

    /** Flow exposing current user preferences reactively */
    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            uiScale = prefs[Keys.UI_SCALE] ?: 1.0f,
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: "en",
            autoUpdateEnabled = prefs[Keys.AUTO_UPDATE] ?: true
        )
    }

    /** Updates the UI scale factor preference */
    suspend fun setUiScale(scale: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.UI_SCALE] = scale
        }
    }

    /** Updates the active language code preference */
    suspend fun setLanguageCode(code: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = code
        }
    }

    /** Toggles auto update checking preference */
    suspend fun setAutoUpdateEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_UPDATE] = enabled
        }
    }
}
