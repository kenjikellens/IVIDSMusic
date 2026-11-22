package com.kenjigames.ividsmusic.di

import com.kenjigames.ividsmusic.data.di.dataModule
import com.kenjigames.ividsmusic.data.di.platformDataModule
import com.kenjigames.ividsmusic.data.local.AppStorageDirectories
import com.kenjigames.ividsmusic.player.di.playerModule
import com.kenjigames.ividsmusic.ui.components.ToastController
import com.kenjigames.ividsmusic.ui.i18n.LocalizationManager
import com.kenjigames.ividsmusic.ui.navigation.AppNavigator
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
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * Provides platform-bound UI services ([com.kenjigames.ividsmusic.platform.PlatformBridge] and
 * [com.kenjigames.ividsmusic.platform.AppUpdateChecker]) for Android or Windows Desktop.
 */
expect fun platformUiModule(): Module

/**
 * Registers shared presentation coordinators, localization manager, navigator, and all 11
 * screen ViewModels in Koin.
 */
fun uiModule(): Module = module {
    single { LocalizationManager(json = get()) }
    single { AppNavigator() }
    single { ToastController() }

    single {
        PlayerViewModel(
            audioEngine = get(),
            queueController = get(),
            resolveStream = get(),
            libraryRepository = get(),
            recommendationEngine = get(),
            preferencesRepository = get(),
            offlineDownloadManager = get(),
            toastController = get()
        )
    } onClose { it?.dispose() }

    single { HomeViewModel(discoveryRepository = get(), libraryRepository = get()) } onClose { it?.dispose() }
    single { SearchViewModel(discoveryRepository = get()) } onClose { it?.dispose() }
    single { LibraryViewModel(libraryRepository = get(), toastController = get()) } onClose { it?.dispose() }
    single {
        DownloaderViewModel(
            downloadManager = get(),
            libraryRepository = get(),
            preferencesRepository = get(),
            platformBridge = get(),
            toastController = get()
        )
    } onClose { it?.dispose() }
    single {
        ProfileViewModel(
            preferencesRepository = get(),
            libraryRepository = get(),
            scoreRepository = get(),
            toastController = get()
        )
    } onClose { it?.dispose() }
    single {
        RecommendedViewModel(
            recommendationEngine = get(),
            scoreRepository = get(),
            libraryRepository = get()
        )
    } onClose { it?.dispose() }
    single {
        SettingsViewModel(
            preferencesRepository = get(),
            scoreRepository = get(),
            offlineDownloadManager = get(),
            localizationManager = get(),
            appUpdateChecker = get(),
            platformBridge = get(),
            toastController = get()
        )
    } onClose { it?.dispose() }

    factory { (artistId: String, artistName: String) ->
        ArtistViewModel(
            artistId = artistId,
            artistNameHint = artistName,
            discoveryRepository = get(),
            recommendationEngine = get()
        )
    }
    factory { (albumId: String) ->
        AlbumViewModel(
            albumId = albumId,
            discoveryRepository = get(),
            libraryRepository = get()
        )
    }
    factory { (trackId: String) ->
        SongDetailViewModel(
            trackId = trackId,
            discoveryRepository = get(),
            libraryRepository = get()
        )
    }
    factory { (playlistId: Long) ->
        PlaylistDetailViewModel(
            playlistId = playlistId,
            libraryRepository = get(),
            toastController = get()
        )
    }
}

/**
 * Bootstraps the full multi-module Koin dependency graph (`:core:data`, `:core:player`, and
 * `:composeApp`) with platform-resolved [directories].
 */
fun initKoin(
    directories: AppStorageDirectories,
    platformDeclaration: KoinApplication.() -> Unit = {}
): KoinApplication = startKoin {
    platformDeclaration()
    modules(
        dataModule,
        platformDataModule(directories),
        playerModule,
        platformUiModule(),
        uiModule()
    )
}
