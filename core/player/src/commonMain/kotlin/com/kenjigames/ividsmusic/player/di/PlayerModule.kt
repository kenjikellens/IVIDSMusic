package com.kenjigames.ividsmusic.player.di

import org.koin.core.module.Module

/**
 * Platform-specific Koin module binding [com.kenjigames.ividsmusic.domain.port.AudioEngine] for Android or JVM Desktop.
 * Resolved via `actual` implementations in `androidMain` and `jvmMain`.
 */
expect val playerModule: Module
