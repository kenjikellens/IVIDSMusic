package com.kenjigames.ividsmusic.di

import com.kenjigames.ividsmusic.platform.AppUpdateChecker
import com.kenjigames.ividsmusic.platform.PlatformBridge
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Windows Desktop Koin UI bindings providing Swing directory selection and GitHub release checks.
 */
actual fun platformUiModule(): Module = module {
    single { PlatformBridge() }
    single { AppUpdateChecker(platformBridge = get(), json = get()) }
}
