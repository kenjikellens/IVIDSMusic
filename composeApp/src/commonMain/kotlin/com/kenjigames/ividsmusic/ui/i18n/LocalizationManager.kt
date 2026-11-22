package com.kenjigames.ividsmusic.ui.i18n

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.kenjigames.ividsmusic.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Enumerates all 16 supported application locales with their ISO code, native display name,
 * and right-to-left script flag for instant UI relocalization.
 */
enum class SupportedLanguage(
    val code: String,
    val displayName: String,
    val isRtl: Boolean = false
) {
    EN("en", "English"),
    NL("nl", "Nederlands"),
    FR("fr", "Français"),
    DE("de", "Deutsch"),
    ES("es", "Español"),
    PT("pt", "Português"),
    IT("it", "Italiano"),
    ZH("zh", "简体中文"),
    HI("hi", "हिन्दी"),
    AR("ar", "العربية", isRtl = true),
    RU("ru", "Русский"),
    RO("ro", "Română"),
    JA("ja", "日本語"),
    TR("tr", "Türkçe"),
    KO("ko", "한국어"),
    PL("pl", "Polski");

    companion object {
        /**
         * Resolves a persisted language [code] into a [SupportedLanguage], falling back to [EN]
         * when an unknown or blank code is supplied.
         */
        fun fromCode(code: String?): SupportedLanguage =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: EN
    }
}

/**
 * Immutable lookup table of localized strings for the active language, backed by English and
 * built-in Compose UI defaults so missing translation keys never render raw placeholders.
 */
@Immutable
data class AppStrings(
    val language: SupportedLanguage = SupportedLanguage.EN,
    private val entries: Map<String, String> = emptyMap(),
    private val englishFallback: Map<String, String> = emptyMap()
) {
    /**
     * Looks up a localized translation for [key], falling back through English JSON entries
     * and Compose-only defaults before returning the key itself.
     */
    operator fun get(key: String): String =
        entries[key]?.takeIf { it.isNotBlank() }
            ?: englishFallback[key]?.takeIf { it.isNotBlank() }
            ?: COMPOSE_DEFAULT_STRINGS[key]
            ?: key

    /**
     * Resolves a localized template for [key] and substitutes `{placeholder}` tokens supplied in [args].
     */
    fun format(key: String, vararg args: Pair<String, String>): String {
        var resolved = get(key)
        for ((token, value) in args) {
            resolved = resolved.replace("{$token}", value)
        }
        return resolved
    }

    private companion object {
        val COMPOSE_DEFAULT_STRINGS = mapOf(
            "dynamic_tint" to "Dynamic Album Tint",
            "dynamic_tint_desc" to "Subtly tint backgrounds and accents from the active album cover.",
            "data_management" to "Data & Storage",
            "clear_cache" to "Clear Audio Cache",
            "clear_cache_desc" to "Free cached online streaming audio files.",
            "clear_scores" to "Reset Taste Scores",
            "clear_scores_desc" to "Clear personalized local recommendation weights.",
            "reset_defaults" to "Reset Preferences",
            "reset_defaults_desc" to "Restore default UI scale, language, tint, and download bitrate.",
            "saved_offline" to "Saved Offline",
            "storage_used" to "Offline Storage Used",
            "rescan_saved" to "Rescan Folder",
            "play_next" to "Play Next",
            "add_to_queue" to "Add to Queue",
            "add_to_playlist" to "Add to Playlist",
            "saved_albums" to "Saved Albums",
            "year_filter" to "Release Year Range",
            "any_year" to "Any Year",
            "retry" to "Retry",
            "move_up" to "Move Up",
            "move_down" to "Move Down",
            "remove" to "Remove",
            "rename" to "Edit Details",
            "confirm" to "Confirm",
            "cancel" to "Cancel",
            "default_quality" to "Default Audio Quality",
            "default_quality_desc" to "Preferred bitrate for offline track downloads.",
            "up_to_date" to "You are on the latest version.",
            "cache_cleared" to "Audio cache cleared.",
            "scores_cleared" to "Recommendation scores reset.",
            "defaults_restored" to "Default preferences restored."
        )
    }
}

val LocalStrings = staticCompositionLocalOf { AppStrings() }

/**
 * Loads and hot-swaps the 16 bundled JSON locale dictionaries from Compose Resources (`files/lang/`)
 * and exposes an observable [StateFlow] of [AppStrings] for instant app-wide relocalization.
 */
class LocalizationManager(private val json: Json = Json { ignoreUnknownKeys = true }) {

    private val loadMutex = Mutex()
    private val cache = mutableMapOf<String, Map<String, String>>()
    private val _strings = MutableStateFlow(AppStrings())

    /** Observable active localized string table collected by the root Compose app. */
    val strings: StateFlow<AppStrings> = _strings.asStateFlow()

    /**
     * Loads the baseline English dictionary and switches immediately to [initialLanguageCode].
     */
    suspend fun initialize(initialLanguageCode: String) {
        loadMutex.withLock {
            if (!cache.containsKey(SupportedLanguage.EN.code)) {
                cache[SupportedLanguage.EN.code] = loadDictionary(SupportedLanguage.EN.code)
            }
            val target = SupportedLanguage.fromCode(initialLanguageCode)
            val targetMap = cache.getOrPut(target.code) { loadDictionary(target.code) }
            val fallback = cache[SupportedLanguage.EN.code].orEmpty()
            _strings.value = AppStrings(target, targetMap, fallback)
        }
    }

    /**
     * Hot-swaps the active translation dictionary to [languageCode] without restarting the app.
     */
    suspend fun setLanguage(languageCode: String) {
        loadMutex.withLock {
            val target = SupportedLanguage.fromCode(languageCode)
            val fallback = cache.getOrPut(SupportedLanguage.EN.code) {
                loadDictionary(SupportedLanguage.EN.code)
            }
            val targetMap = cache.getOrPut(target.code) { loadDictionary(target.code) }
            _strings.value = AppStrings(target, targetMap, fallback)
        }
    }

    /**
     * Reads and parses `files/lang/{code}.json` from Compose Multiplatform resources on IO dispatcher.
     */
    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadDictionary(code: String): Map<String, String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val rawBytes = Res.readBytes("files/lang/$code.json")
                val obj = json.parseToJsonElement(rawBytes.decodeToString()).jsonObject
                obj.mapValues { (_, element) -> element.jsonPrimitive.content }
            }.getOrElse { emptyMap() }
        }
}
