package com.kenjigames.ividsmusic.ui.navigation

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Closed hierarchy of all navigation destinations across Mobile, Desktop, and Android TV shells.
 */
@Immutable
sealed interface AppRoute {
    /** Primary discovery feed showing genre charts, top artists, and top albums. */
    data object Home : AppRoute
    /** Personalized algorithmic recommendations view powered by local interest scores. */
    data object Recommended : AppRoute
    /** Search and browse view with category filters and release-year range slider. */
    data class Search(val initialQuery: String = "") : AppRoute
    /** Local library tabs for liked songs, saved albums, playlists, and recent history. */
    data object Library : AppRoute
    /** Local listener profile stats, edit modal, 20-track history, and top genre breakdown. */
    data object Profile : AppRoute
    /** Direct URL downloader, per-download bitrate selector, and offline saved files browser. */
    data object Downloader : AppRoute
    /** Appearance, 16-language picker, default download quality, update checker, and storage settings. */
    data object Settings : AppRoute
    /** Artist profile, popular tracks, discography, and related artists screen. */
    data class ArtistDetail(val artistId: String, val artistName: String = "") : AppRoute
    /** Album cover, metadata, batch actions, and ordered tracklist screen. */
    data class AlbumDetail(val albumId: String) : AppRoute
    /** Detailed track view with queue/playlist/download actions and similar tracks. */
    data class SongDetail(val trackId: String) : AppRoute
    /** Custom user playlist detail supporting inline rename, reordering, removal, and deletion. */
    data class PlaylistDetail(val playlistId: Long) : AppRoute
}

/**
 * Enumerates top-level root navigation destinations rendered in the bottom bar or sidebar rail.
 */
enum class RootDestination {
    HOME, RECOMMENDED, SEARCH, LIBRARY, PROFILE, DOWNLOADER, SETTINGS
}

/**
 * Manages the immutable navigation back-stack for IVIDS Music screens without platform-coupled
 * Fragment/NavHost overhead.
 */
class AppNavigator(initialRoute: AppRoute = AppRoute.Home) {

    private val _backStack = MutableStateFlow(listOf(initialRoute))

    /** Observable stack of active routes where the last element is the visible screen. */
    val backStack: StateFlow<List<AppRoute>> = _backStack.asStateFlow()

    /** Returns the currently active top route on the back-stack. */
    val currentRoute: AppRoute
        get() = _backStack.value.lastOrNull() ?: AppRoute.Home

    /**
     * Pushes [route] onto the back-stack, deduplicating consecutive identical routes when [singleTop]
     * is true so rapid clicks do not create duplicate history frames.
     */
    fun navigateTo(route: AppRoute, singleTop: Boolean = true) {
        _backStack.update { stack ->
            val current = stack.lastOrNull()
            if (singleTop && current == route) {
                stack
            } else {
                (stack + route).takeLast(32)
            }
        }
    }

    /**
     * Switches directly to a primary root tab, collapsing any nested detail stack underneath it.
     */
    fun switchRootTab(route: AppRoute) {
        _backStack.value = listOf(route)
    }

    /**
     * Pops the top route from the back-stack if more than one entry exists; returns true when handled.
     */
    fun navigateBack(): Boolean {
        var popped = false
        _backStack.update { stack ->
            if (stack.size > 1) {
                popped = true
                stack.dropLast(1)
            } else {
                stack
            }
        }
        return popped
    }
}
