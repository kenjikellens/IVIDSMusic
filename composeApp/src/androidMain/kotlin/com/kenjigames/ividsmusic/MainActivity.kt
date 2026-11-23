package com.kenjigames.ividsmusic

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kenjigames.ividsmusic.ui.navigation.AppNavigator
import org.koin.android.ext.android.inject
import org.koin.compose.KoinContext
import org.koin.core.context.GlobalContext

/**
 * Single-Activity Jetpack Compose host for both Android Mobile and Android TV (`mobile` and `tv`
 * flavors) replacing legacy WebView rendering with pure GPU-accelerated native Compose UI.
 */
class MainActivity : ComponentActivity() {

    private val navigator: AppNavigator by inject()

    /**
     * Configures edge-to-edge dark system bars, prioritizes closing NowPlayingSheet before
     * popping navigation back-stack entries, and mounts [IvidsApp].
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        setContent {
            val backStack by navigator.backStack.collectAsState()
            var nowPlayingExpanded by rememberSaveable { mutableStateOf(false) }

            BackHandler(enabled = nowPlayingExpanded || backStack.size > 1) {
                if (nowPlayingExpanded) {
                    nowPlayingExpanded = false
                } else {
                    navigator.navigateBack()
                }
            }

            KoinContext(context = GlobalContext.get()) {
                IvidsApp(
                    nowPlayingExpanded = nowPlayingExpanded,
                    onNowPlayingExpandedChange = { nowPlayingExpanded = it }
                )
            }
        }
    }
}
