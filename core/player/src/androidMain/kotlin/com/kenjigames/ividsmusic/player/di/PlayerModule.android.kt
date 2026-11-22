package com.kenjigames.ividsmusic.player.di

import com.kenjigames.ividsmusic.domain.port.AudioEngine
import com.kenjigames.ividsmusic.player.android.AndroidAudioEngine
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * Android Koin module providing [AndroidAudioEngine] bound to [AudioEngine] with automatic lifecycle release.
 * Depends on Koin's `androidContext()` supplied at application startup.
 */
actual val playerModule: Module = module {
    single<AudioEngine> { AndroidAudioEngine(context = androidContext()) } onClose { it?.release() }
}
