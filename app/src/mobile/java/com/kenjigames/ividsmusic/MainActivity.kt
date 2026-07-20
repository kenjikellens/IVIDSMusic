package com.kenjigames.ividsmusic

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kenjigames.ividsmusic.player.PlaybackManager
import com.kenjigames.ividsmusic.player.PlaybackService
import com.kenjigames.ividsmusic.ui.component.player.PlayerBottomBar
import com.kenjigames.ividsmusic.ui.navigation.BottomNavBar
import com.kenjigames.ividsmusic.ui.navigation.NavGraph
import com.kenjigames.ividsmusic.ui.theme.DarkBackground
import com.kenjigames.ividsmusic.ui.theme.IVIDSMusicTheme

/**
 * Main Activity for the native Android Mobile app flavor.
 * Renders the Jetpack Compose single-activity interface with a persistent PlayerBottomBar,
 * BottomNavBar, and Media3 background audio service integration.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start background Media3 audio playback service
        val serviceIntent = Intent(this, PlaybackService::class.java)
        startService(serviceIntent)

        setContent {
            IVIDSMusicTheme {
                val navController = rememberNavController()
                val playerState by PlaybackManager.instance.playerState.collectAsState()

                Scaffold(
                    containerColor = DarkBackground,
                    bottomBar = {
                        BottomNavBar(navController = navController)
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Core Screen Navigation Graph
                        NavGraph(
                            navController = navController,
                            onSongClick = { song ->
                                PlaybackManager.instance.playSong(song)
                            }
                        )

                        // Persistent Player Bottom Bar
                        PlayerBottomBar(
                            playerState = playerState,
                            modifier = Modifier.align(Alignment.BottomCenter),
                            onClick = {
                                navController.navigate(com.kenjigames.ividsmusic.ui.navigation.Screen.SongDetail.route)
                            },
                            onTogglePlayPause = { PlaybackManager.instance.togglePlayPause() },
                            onNext = { PlaybackManager.instance.next() },
                            onToggleLike = {
                                playerState.currentSong?.let { song ->
                                    // Toggle like
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
