package com.kenjigames.ividsmusic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.kenjigames.ividsmusic.data.local.AppStorageDirectories
import com.kenjigames.ividsmusic.di.initKoin
import com.kenjigames.ividsmusic.domain.port.AudioEngine
import com.kenjigames.ividsmusic.resources.Res
import com.kenjigames.ividsmusic.resources.ic_ivids_brand
import com.kenjigames.ividsmusic.ui.navigation.AppNavigator
import com.kenjigames.ividsmusic.ui.navigation.AppRoute
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.KoinContext
import org.koin.core.context.stopKoin
import java.io.File

/**
 * Windows Desktop JVM entrypoint bootstrapping Koin DI, bundled native tool lookup (`yt-dlp.exe`),
 * Compose Desktop Window, and global media keyboard shortcuts (`Space`, `Ctrl+K`, `Left/Right`, `M`).
 */
fun main() {
    val directories = resolveDesktopDirectories()
    val koinApp = initKoin(directories)
    val koin = koinApp.koin
    val playerViewModel = koin.get<PlayerViewModel>()
    val navigator = koin.get<AppNavigator>()
    val audioEngine = koin.get<AudioEngine>()

    application {
        var searchFocusTrigger by remember { mutableIntStateOf(0) }
        val windowState = rememberWindowState(width = 1280.dp, height = 820.dp)

        Window(
            onCloseRequest = {
                runCatching { audioEngine.release() }
                runCatching { stopKoin() }
                exitApplication()
            },
            title = "IVIDS Music",
            state = windowState,
            icon = painterResource(Res.drawable.ic_ivids_brand),
            onPreviewKeyEvent = { event ->
                if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.K) {
                    searchFocusTrigger += 1
                    if (navigator.currentRoute !is AppRoute.Search) {
                        navigator.navigateTo(AppRoute.Search())
                    }
                    true
                } else {
                    false
                }
            },
            onKeyEvent = { event ->
                if (event.type != KeyEventType.KeyDown) return@Window false
                when (event.key) {
                    Key.Spacebar -> {
                        playerViewModel.togglePlayPause()
                        true
                    }
                    Key.M -> {
                        playerViewModel.toggleMute()
                        true
                    }
                    Key.DirectionLeft -> {
                        if (event.isShiftPressed) {
                            playerViewModel.previous()
                        } else {
                            playerViewModel.seekRelative(-5_000L)
                        }
                        true
                    }
                    Key.DirectionRight -> {
                        if (event.isShiftPressed) {
                            playerViewModel.next()
                        } else {
                            playerViewModel.seekRelative(5_000L)
                        }
                        true
                    }
                    else -> false
                }
            }
        ) {
            KoinContext(context = koin) {
                IvidsApp(searchFocusTrigger = searchFocusTrigger)
            }
        }
    }
}

/**
 * Resolves Windows/JVM user-local cache, database, saved offline tracks, and bundled tools directories.
 */
private fun resolveDesktopDirectories(): AppStorageDirectories {
    val appDataRoot = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }
        ?: System.getProperty("user.home")
    val baseDir = File(appDataRoot, "IVIDSMusic").apply { mkdirs() }
    val cacheDir = File(baseDir, "cache").apply { mkdirs() }
    val filesDir = File(baseDir, "data").apply { mkdirs() }
    val savedDir = File(baseDir, "saved").apply { mkdirs() }

    val composeResourcesDir = System.getProperty("compose.application.resources.dir")
        ?.takeIf { it.isNotBlank() }
    val devResourcesDir = File("composeApp/resources/windows")
        .takeIf { it.exists() }
        ?.absolutePath
    val rootDir = File(".").takeIf { File(it, "yt-dlp.exe").exists() }?.absolutePath

    return AppStorageDirectories(
        cacheDir = cacheDir.absolutePath,
        filesDir = filesDir.absolutePath,
        savedDir = savedDir.absolutePath,
        bundledToolsDir = composeResourcesDir ?: devResourcesDir ?: rootDir
    )
}
