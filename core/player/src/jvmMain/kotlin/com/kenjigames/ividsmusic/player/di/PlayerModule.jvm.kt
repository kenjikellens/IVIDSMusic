package com.kenjigames.ividsmusic.player.di

import com.kenjigames.ividsmusic.domain.port.AudioEngine
import com.kenjigames.ividsmusic.player.desktop.DesktopAudioEngine
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * Windows JVM Desktop Koin module binding [DesktopAudioEngine] to [AudioEngine] with `release()` on container close.
 * Requires no external platform parameters at injection time.
 */
actual val playerModule: Module = module {
    single<AudioEngine> { DesktopAudioEngine() } onClose { it?.release() }
}
