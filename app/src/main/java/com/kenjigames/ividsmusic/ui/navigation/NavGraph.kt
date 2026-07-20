package com.kenjigames.ividsmusic.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.ui.screen.home.HomeScreen
import com.kenjigames.ividsmusic.ui.screen.library.LibraryScreen
import com.kenjigames.ividsmusic.ui.screen.profile.ProfileScreen
import com.kenjigames.ividsmusic.ui.screen.search.SearchScreen
import com.kenjigames.ividsmusic.ui.screen.settings.SettingsScreen

/**
 * Navigation graph host managing screen route transitions across the application.
 *
 * @param navController Navigation controller
 * @param modifier Compose modifier
 * @param onSongClick Global song playback trigger callback
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onSongClick: (Song) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onSongClick = onSongClick,
                onSeeAllClick = { navController.navigate(Screen.Search.route) }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onSongClick = onSongClick
            )
        }

        composable(Screen.Library.route) {
            LibraryScreen(
                onSongClick = onSongClick,
                onExploreClick = { navController.navigate(Screen.Search.route) }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onSongClick = onSongClick
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
