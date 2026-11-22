package com.kenjigames.ividsmusic.ui.components

import android.os.Build

/**
 * Android supports RenderEffect-backed [androidx.compose.ui.Modifier.blur] starting on Android 12 (API 31).
 */
actual fun isNativeRenderBlurSupported(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
