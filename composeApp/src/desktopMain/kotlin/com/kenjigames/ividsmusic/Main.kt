package com.kenjigames.ividsmusic

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.kenjigames.ividsmusic.data.local.AppStorageDirectories
import com.kenjigames.ividsmusic.data.stream.DesktopNativeToolsManager
import com.kenjigames.ividsmusic.data.stream.YtDlpDesktopResolver
import com.kenjigames.ividsmusic.di.initKoin
import com.kenjigames.ividsmusic.domain.port.AudioEngine
import com.kenjigames.ividsmusic.player.desktop.DesktopAudioPrefetcher
import com.kenjigames.ividsmusic.resources.Res
import com.kenjigames.ividsmusic.resources.ic_ivids_brand
import com.kenjigames.ividsmusic.ui.input.TextInputShortcutGuard
import com.kenjigames.ividsmusic.ui.navigation.AppNavigator
import com.kenjigames.ividsmusic.ui.navigation.AppRoute
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.KoinContext
import org.koin.core.context.stopKoin
import java.awt.GraphicsEnvironment
import java.io.File
import kotlin.system.exitProcess

@Serializable
private data class WindowBounds(
    val widthDp: Float = 1280f,
    val heightDp: Float = 820f,
    val xDp: Float? = null,
    val yDp: Float? = null
)

/** Loads persisted window bounds from [file], returning defaults if absent or off-screen. */
private fun loadWindowBounds(file: File): WindowBounds {
    val raw = runCatching { file.takeIf { it.isFile }?.readText() }.getOrNull() ?: return WindowBounds()
    val parsed = runCatching { Json { ignoreUnknownKeys = true }.decodeFromString<WindowBounds>(raw) }.getOrNull()
        ?: return WindowBounds()
    val safeW = parsed.widthDp.coerceIn(720f, 3840f)
    val safeH = parsed.heightDp.coerceIn(520f, 2160f)
    val x = parsed.xDp
    val y = parsed.yDp
    val visibleOnAnyMonitor = if (x != null && y != null) {
        runCatching {
            GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.any { device ->
                val b = device.defaultConfiguration.bounds
                x >= b.x - 100 && x <= b.x + b.width - 120 && y >= b.y - 40 && y <= b.y + b.height - 120
            }
        }.getOrDefault(false)
    } else {
        false
    }
    return if (visibleOnAnyMonitor) {
        WindowBounds(safeW, safeH, x, y)
    } else {
        WindowBounds(safeW, safeH, null, null)
    }
}

/** Saves current desktop window size and position to [file] safely. */
private fun saveWindowBounds(file: File, widthDp: Dp, heightDp: Dp, position: WindowPosition) {
    val x = (position as? WindowPosition.Absolute)?.x?.value
    val y = (position as? WindowPosition.Absolute)?.y?.value
    val payload = WindowBounds(
        widthDp = widthDp.value.coerceAtLeast(720f),
        heightDp = heightDp.value.coerceAtLeast(520f),
        xDp = x,
        yDp = y
    )
    runCatching { file.writeText(Json.encodeToString(payload)) }
}

/**
 * Configures JVM and Skiko rendering system properties before AWT/Swing initializes so Windows
 * fractional HiDPI displays (e.g., 125%/150%) render crisp typography and VSync-paced frames.
 */
private fun configureDesktopRenderingHints() {
    System.setProperty("sun.java2d.dpiaware", "true")
    System.setProperty("sun.java2d.uiScale.enabled", "true")
    System.setProperty("awt.useSystemAAFontSettings", "lcd")
    System.setProperty("swing.aatext", "true")
    System.setProperty("skiko.vsync.enabled", "true")
}

/**
 * Windows Desktop JVM entrypoint bootstrapping Koin DI, bundled native tool lookup (`yt-dlp.exe`),
 * Compose Desktop Window, and global media keyboard shortcuts (`Space`, `Ctrl+K`, `Left/Right`, `M`).
 */
fun main() {
    configureDesktopRenderingHints()
    val directories = resolveDesktopDirectories()
    val koinApp = initKoin(directories)
    val koin = koinApp.koin
    val playerViewModel = koin.get<PlayerViewModel>()
    val navigator = koin.get<AppNavigator>()
    val audioEngine = koin.get<AudioEngine>()
    val toolsManager = koin.get<DesktopNativeToolsManager>()
    val ytDlpResolver = koin.get<YtDlpDesktopResolver>()
    val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    backgroundScope.launch {
        toolsManager.ensureToolsReady(
            ytDlpCurrentPath = ytDlpResolver.findYtDlpExecutable(),
            ffmpegCurrentPath = DesktopAudioPrefetcher.findAvailableFfmpegPath()
        )
    }

    val boundsFile = File(directories.filesDir, "window_bounds.json")
    val initialBounds = loadWindowBounds(boundsFile)

    application {
        var searchFocusTrigger by remember { mutableIntStateOf(0) }
        var nowPlayingExpanded by remember { mutableStateOf(false) }
        val windowState = rememberWindowState(
            width = initialBounds.widthDp.dp,
            height = initialBounds.heightDp.dp,
            position = if (initialBounds.xDp != null && initialBounds.yDp != null) {
                WindowPosition.Absolute(initialBounds.xDp.dp, initialBounds.yDp.dp)
            } else {
                WindowPosition.Aligned(Alignment.Center)
            }
        )

        Window(
            onCloseRequest = {
                saveWindowBounds(boundsFile, windowState.size.width, windowState.size.height, windowState.position)
                runCatching { runBlocking { playerViewModel.saveSessionNow() } }
                runCatching { backgroundScope.cancel() }
                runCatching { audioEngine.release() }
                runCatching { stopKoin() }
                exitApplication()
                exitProcess(0)
            },
            title = "IVIDS Music",
            state = windowState,
            icon = painterResource(Res.drawable.ic_ivids_brand),
            onPreviewKeyEvent = { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when {
                        event.isCtrlPressed && event.key == Key.K -> {
                            searchFocusTrigger += 1
                            if (navigator.currentRoute !is AppRoute.Search) {
                                navigator.navigateTo(AppRoute.Search())
                            }
                            true
                        }
                        event.key == Key.Escape && nowPlayingExpanded -> {
                            nowPlayingExpanded = false
                            true
                        }
                        event.key == Key.MediaPlayPause -> {
                            playerViewModel.togglePlayPause()
                            true
                        }
                        event.key == Key.MediaPlay -> {
                            if (!playerViewModel.uiState.value.isPlaying) playerViewModel.togglePlayPause()
                            true
                        }
                        event.key == Key.MediaPause || event.key == Key.MediaStop -> {
                            if (playerViewModel.uiState.value.isPlaying) playerViewModel.togglePlayPause()
                            true
                        }
                        event.key == Key.MediaNext -> {
                            playerViewModel.next()
                            true
                        }
                        event.key == Key.MediaPrevious -> {
                            playerViewModel.previous()
                            true
                        }
                        event.key == Key.VolumeMute -> {
                            playerViewModel.toggleMute()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
            onKeyEvent = { event ->
                if (event.type != KeyEventType.KeyDown ||
                    TextInputShortcutGuard.isTyping() ||
                    event.isCtrlPressed ||
                    event.isAltPressed ||
                    event.isMetaPressed
                ) {
                    return@Window false
                }
                when (event.key) {
                    Key.Spacebar -> {
                        playerViewModel.togglePlayPause()
                        true
                    }
                    Key.M -> {
                        playerViewModel.toggleMute()
                        true
                    }
                    Key.S -> {
                        playerViewModel.toggleShuffle()
                        true
                    }
                    Key.R -> {
                        playerViewModel.cycleRepeatMode()
                        true
                    }
                    Key.L -> {
                        playerViewModel.toggleLikeCurrent()
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
            LaunchedEffect(window) {
                val obsidianAwt = java.awt.Color(9, 9, 9)
                window.background = obsidianAwt
                window.contentPane.background = obsidianAwt
            }
            KoinContext(context = koin) {
                IvidsApp(
                    searchFocusTrigger = searchFocusTrigger,
                    nowPlayingExpanded = nowPlayingExpanded,
                    onNowPlayingExpandedChange = { nowPlayingExpanded = it }
                )
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
