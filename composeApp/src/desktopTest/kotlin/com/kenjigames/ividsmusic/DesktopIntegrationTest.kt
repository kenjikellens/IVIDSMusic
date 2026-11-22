package com.kenjigames.ividsmusic

import com.kenjigames.ividsmusic.data.local.AppStorageDirectories
import com.kenjigames.ividsmusic.di.initKoin
import com.kenjigames.ividsmusic.domain.model.AudioQuality
import com.kenjigames.ividsmusic.ui.i18n.LocalizationManager
import com.kenjigames.ividsmusic.ui.i18n.SupportedLanguage
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.screens.album.AlbumViewModel
import com.kenjigames.ividsmusic.ui.screens.artist.ArtistViewModel
import com.kenjigames.ividsmusic.ui.screens.downloader.DownloaderViewModel
import com.kenjigames.ividsmusic.ui.screens.home.HomeViewModel
import com.kenjigames.ividsmusic.ui.screens.library.LibraryViewModel
import com.kenjigames.ividsmusic.ui.screens.playlist.PlaylistDetailViewModel
import com.kenjigames.ividsmusic.ui.screens.profile.ProfileViewModel
import com.kenjigames.ividsmusic.ui.screens.recommended.RecommendedViewModel
import com.kenjigames.ividsmusic.ui.screens.search.SearchViewModel
import com.kenjigames.ividsmusic.ui.screens.settings.SettingsViewModel
import com.kenjigames.ividsmusic.ui.screens.song.SongDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies desktop resources (all 16 language JSON bundles, icons, TTF fonts) and full Koin
 * dependency graph resolution across `:core:domain`, `:core:data`, `:core:player`, and `:composeApp`.
 */
class DesktopIntegrationTest {

    /**
     * Checks that all 16 locale JSON files exist, parse into valid JSON objects, and contain
     * non-blank translations for primary navigation keys.
     */
    @Test
    fun all16LanguageBundlesParseCleanly() {
        val langDir = File("src/commonMain/composeResources/files/lang")
        assertTrue(langDir.isDirectory, "Expected composeResources/files/lang directory at ${langDir.absolutePath}")

        val json = Json { ignoreUnknownKeys = true }
        SupportedLanguage.entries.forEach { lang ->
            val file = File(langDir, "${lang.code}.json")
            assertTrue(file.exists() && file.length() > 100L, "Missing or empty ${lang.code}.json")
            val parsed = json.parseToJsonElement(file.readText(Charsets.UTF_8)).jsonObject
            assertTrue(parsed.containsKey("home"), "Missing 'home' key in ${lang.code}.json")
            assertTrue(parsed["settings"]?.jsonPrimitive?.content?.isNotBlank() == true)
        }
    }

    /**
     * Verifies Desktop PNG/ICO icons and bundled Plus Jakarta Sans + Inter font assets exist and are valid.
     */
    @Test
    fun iconsAndBundledFontsExist() {
        val iconPng = File("src/desktopMain/resources/icon.png")
        val iconIco = File("src/desktopMain/resources/icon.ico")
        val brandPng = File("src/commonMain/composeResources/drawable/ic_ivids_brand.png")
        assertTrue(iconPng.exists() && iconPng.length() > 1024L)
        assertTrue(iconIco.exists() && iconIco.length() > 1024L)
        assertTrue(brandPng.exists() && brandPng.length() > 1024L)

        val fontDir = File("src/commonMain/composeResources/font")
        listOf(
            "plus_jakarta_sans_semibold.ttf",
            "plus_jakarta_sans_bold.ttf",
            "inter_regular.ttf",
            "inter_medium.ttf",
            "inter_semibold.ttf"
        ).forEach { fontFile ->
            val file = File(fontDir, fontFile)
            assertTrue(file.exists() && file.length() > 10_000L, "Missing font $fontFile")
        }
    }

    /**
     * Bootstraps the full multi-module Koin container against a temporary directory and resolves
     * every ViewModel and localization service without missing bindings.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun koinResolvesAllPresentationAndDomainDependencies() {
        val tempRoot = Files.createTempDirectory("ivids_compose_koin_test").toFile()
        val testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        try {
            val dirs = AppStorageDirectories(
                cacheDir = File(tempRoot, "cache").apply { mkdirs() }.absolutePath,
                filesDir = File(tempRoot, "files").apply { mkdirs() }.absolutePath,
                savedDir = File(tempRoot, "saved").apply { mkdirs() }.absolutePath,
                bundledToolsDir = null
            )
            val koin = initKoin(dirs).koin
            try {
                assertEquals(7, AudioQuality.entries.size)
                koin.get<LocalizationManager>()
                koin.get<PlayerViewModel>()
                koin.get<HomeViewModel>()
                koin.get<SearchViewModel>()
                koin.get<LibraryViewModel>()
                koin.get<DownloaderViewModel>()
                koin.get<ProfileViewModel>()
                koin.get<RecommendedViewModel>()
                koin.get<SettingsViewModel>()

                val artistVm = koin.get<ArtistViewModel> { parametersOf("27", "Daft Punk") }
                artistVm.dispose()
                val albumVm = koin.get<AlbumViewModel> { parametersOf("302127") }
                albumVm.dispose()
                val songVm = koin.get<SongDetailViewModel> { parametersOf("3135556") }
                songVm.dispose()
                val playlistVm = koin.get<PlaylistDetailViewModel> { parametersOf(1L) }
                playlistVm.dispose()
            } finally {
                stopKoin()
                runBlocking { }
            }
        } finally {
            Dispatchers.resetMain()
            tempRoot.deleteRecursively()
        }
    }
}
