package com.kenjigames.ividsmusic.di

import com.kenjigames.ividsmusic.platform.AppUpdateChecker
import com.kenjigames.ividsmusic.platform.PlatformBridge
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android Koin UI bindings providing [PlatformBridge] and [AppUpdateChecker] backed by Android
 * application Context and FileProvider OTA updates.
 */
actual fun platformUiModule(): Module = module {
    single { PlatformBridge(context = androidContext()) }
    single { AppUpdateChecker(context = androidContext(), platformBridge = get(), json = get()) }
}
