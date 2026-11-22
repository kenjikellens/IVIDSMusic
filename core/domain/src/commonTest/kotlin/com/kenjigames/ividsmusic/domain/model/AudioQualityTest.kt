package com.kenjigames.ividsmusic.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies exact bitrate definitions and nearest-tier mapping rules on [AudioQuality].
 */
class AudioQualityTest {

    /** Confirms all seven required audio quality bitrates exist in exact ascending order. */
    @Test
    fun exactBitratesMatchSpecification() {
        assertEquals(
            listOf(4, 48, 96, 128, 196, 256, 320),
            AudioQuality.entries.map { it.kbps }
        )
        assertEquals(4, AudioQuality.Q4.kbps)
        assertEquals(48, AudioQuality.Q48.kbps)
        assertEquals(96, AudioQuality.Q96.kbps)
        assertEquals(128, AudioQuality.Q128.kbps)
        assertEquals(196, AudioQuality.Q196.kbps)
        assertEquals(256, AudioQuality.Q256.kbps)
        assertEquals(320, AudioQuality.Q320.kbps)
        assertEquals("196 kbps", AudioQuality.Q196.label)
    }

    /** Verifies arbitrary bitrates resolve to the closest [AudioQuality] enum constant. */
    @Test
    fun fromKbpsMapsNearestQualityTier() {
        assertEquals(AudioQuality.Q4, AudioQuality.fromKbps(4))
        assertEquals(AudioQuality.Q48, AudioQuality.fromKbps(40))
        assertEquals(AudioQuality.Q128, AudioQuality.fromKbps(129))
        assertEquals(AudioQuality.Q196, AudioQuality.fromKbps(192))
        assertEquals(AudioQuality.Q256, AudioQuality.fromKbps(255))
        assertEquals(AudioQuality.Q320, AudioQuality.fromKbps(500))
    }
}
