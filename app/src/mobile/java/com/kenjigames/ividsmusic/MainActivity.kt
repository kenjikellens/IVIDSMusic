package com.kenjigames.ividsmusic

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.kenjigames.ividsmusic.player.PlaybackManager
import com.kenjigames.ividsmusic.player.PlaybackService
import com.kenjigames.ividsmusic.ui.component.player.PlayerBottomBar
import com.kenjigames.ividsmusic.ui.navigation.BottomNavBar
import com.kenjigames.ividsmusic.ui.navigation.NavGraph
import com.kenjigames.ividsmusic.ui.theme.DarkBackground
import com.kenjigames.ividsmusic.ui.theme.IVIDSMusicTheme
import com.kenjigames.ividsmusic.ui.theme.PrimaryAccent

/**
 * Main Activity for the native Android Mobile app flavor.
 * Renders the Jetpack Compose single-activity interface with a unified bottom container
 * holding the floating PlayerBottomBar, BottomNavBar, and ambient dark background gradient.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start background Media3 audio playback service safely
        try {
            val serviceIntent = Intent(this, PlaybackService::class.java)
            startService(serviceIntent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to start PlaybackService: ${e.message}")
        }

        setContent {
            IVIDSMusicTheme {
                val navController = rememberNavController()
                val playerState by PlaybackManager.instance.playerState.collectAsState()

                Scaffold(
                    containerColor = DarkBackground,
                    bottomBar = {
                        Column {
                            // Persistent Player Bottom Bar positioned directly above BottomNavBar
                            PlayerBottomBar(
                                playerState = playerState,
                                onClick = {
                                    navController.navigate(com.kenjigames.ividsmusic.ui.navigation.Screen.SongDetail.route)
                                },
                                onTogglePlayPause = { PlaybackManager.instance.togglePlayPause() },
                                onNext = { PlaybackManager.instance.next() },
                                onToggleLike = {
                                    playerState.currentSong?.let { song ->
                                        // Toggle like state
                                    }
                                }
                            )

                            // Floating Bottom Navigation Bar
                            BottomNavBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        PrimaryAccent.copy(alpha = 0.08f),
                                        Color.Transparent,
                                        DarkBackground
                                    )
                                )
                            )
                            .padding(innerPadding)
                    ) {
                        // Core Screen Navigation Graph
                        NavGraph(
                            navController = navController,
                            onSongClick = { song ->
                                PlaybackManager.instance.playSong(song)
                            }
                        )
                    }
                }
            }
        }
    }
}
