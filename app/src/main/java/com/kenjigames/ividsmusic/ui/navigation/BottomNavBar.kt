package com.kenjigames.ividsmusic.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kenjigames.ividsmusic.ui.theme.PrimaryAccent
import com.kenjigames.ividsmusic.ui.theme.TextMuted
import com.kenjigames.ividsmusic.ui.theme.TextPrimary
import com.kenjigames.ividsmusic.ui.theme.Typography

/**
 * Standard edge-to-edge mobile bottom navigation bar matching YouTube and Spotify layouts.
 *
 * @param navController Navigation controller
 * @param modifier Compose modifier
 */
@Composable
fun BottomNavBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Screen.Home,
        Screen.Search,
        Screen.Library,
        Screen.Profile,
        Screen.Settings
    )

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF101016),
        tonalElevation = 8.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = TextPrimary,
            tonalElevation = 0.dp
        ) {
            items.forEach { screen ->
                val isSelected = currentRoute == screen.route
                NavigationBarItem(
                    icon = {
                        screen.icon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = screen.title
                            )
                        }
                    },
                    label = {
                        Text(
                            text = screen.title,
                            style = Typography.labelMedium
                        )
                    },
                    selected = isSelected,
                    onClick = {
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryAccent,
                        selectedTextColor = PrimaryAccent,
                        indicatorColor = Color(0x228B5CF6),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
            }
        }
    }
}
