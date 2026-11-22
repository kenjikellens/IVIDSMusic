package com.kenjigames.ividsmusic.data.di

/**
 * JVM and Android actual implementation returning standard wall-clock epoch milliseconds.
 */
internal actual fun currentEpochMillis(): Long = System.currentTimeMillis()
