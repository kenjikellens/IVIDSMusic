package com.kenjigames.ividsmusic.player.di

import com.kenjigames.ividsmusic.domain.port.AudioEngine
import com.kenjigames.ividsmusic.player.desktop.DesktopAudioEngine
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

/**
 * Verifies [playerModule] Koin resolution and singleton cleanup on JVM Desktop.
 * Ensures `AudioEngine` resolves cleanly to a single [DesktopAudioEngine] instance.
 */
class PlayerModuleJvmTest {

    /**
     * Stops any active Koin context after each test so global Koin state stays isolated.
     * Prevents cross-test pollution in concurrent or repeated test runs.
     */
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    /**
     * Starts Koin with [playerModule], resolves [AudioEngine] twice as a singleton, and closes cleanly.
     * Verifies `onClose` disposal runs without throwing on JVM Desktop.
     */
    @Test
    fun resolvesSingletonDesktopAudioEngineAndClosesCleanly() {
        val app = startKoin {
            modules(playerModule)
        }
        val first = app.koin.get<AudioEngine>()
        val second = app.koin.get<AudioEngine>()

        assertIs<DesktopAudioEngine>(first)
        assertSame(first, second)

        app.close()
    }
}
