package com.kenjigames.ividsmusic.player.desktop

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Verifies [DesktopAudioCacheTranscoder] JavaFX container detection, YouTube cache key stability, and LRU pruning.
 * Operates purely against URIs and isolated temp folders without external network or `ffmpeg` process execution.
 */
class DesktopAudioCacheTranscoderTest {

    /**
     * Ensures local `.mp3` and `.wav` URIs bypass transcoding while `.m4a` and HTTP streams require MP3 preparation.
     */
    @Test
    fun isDirectlyPlayableByJavaFxDetectsMp3AndWavOnly() {
        assertTrue(DesktopAudioCacheTranscoder.isDirectlyPlayableByJavaFx("file:///C:/Music/track.mp3"))
        assertTrue(DesktopAudioCacheTranscoder.isDirectlyPlayableByJavaFx("file:///C:/Music/TRACK.WAV"))
        assertFalse(DesktopAudioCacheTranscoder.isDirectlyPlayableByJavaFx("file:///C:/Music/track.m4a"))
        assertFalse(DesktopAudioCacheTranscoder.isDirectlyPlayableByJavaFx("https://cdn.example.com/audio.mp3"))
    }

    /**
     * Verifies `googlevideo.com` URLs sharing `id` and `itag` yield identical cache keys across rotating signatures.
     */
    @Test
    fun buildCacheKeyDeduplicatesSignedYouTubeUrlsAndSeparatesDifferentTracks() {
        val urlA =
            "https://rr1---sn.googlevideo.com/videoplayback?expire=111&id=o-ABC123&itag=140&sig=SIG_ONE"
        val urlB =
            "https://rr9---sn.googlevideo.com/videoplayback?expire=222&id=o-ABC123&itag=140&sig=SIG_TWO"
        val otherTrack =
            "https://rr9---sn.googlevideo.com/videoplayback?expire=222&id=o-XYZ999&itag=140&sig=SIG_TWO"

        val keyA = DesktopAudioCacheTranscoder.buildCacheKey(urlA)
        val keyB = DesktopAudioCacheTranscoder.buildCacheKey(urlB)
        val keyOther = DesktopAudioCacheTranscoder.buildCacheKey(otherTrack)
        val hintedA = DesktopAudioCacheTranscoder.buildCacheKey(urlA, cacheKeyHint = "xy3AcmW0lrQ")
        val hintedB = DesktopAudioCacheTranscoder.buildCacheKey(otherTrack, cacheKeyHint = "xy3AcmW0lrQ")

        assertEquals(24, keyA.length)
        assertEquals(keyA, keyB)
        assertNotEquals(keyA, keyOther)
        assertEquals(hintedA, hintedB)
    }

    /**
     * Confirms `pruneOldCacheFiles` keeps only the newest `maxFiles` `.mp3` files while ignoring `.part.mp3` temp files.
     */
    @Test
    fun pruneOldCacheFilesRetainsNewestEntriesOnly() {
        val tempDir = Files.createTempDirectory("ivids-cache-prune-test").toFile()
        try {
            val f1 = File(tempDir, "old.mp3").apply {
                writeText("1")
                setLastModified(1_000L)
            }
            val f2 = File(tempDir, "mid.mp3").apply {
                writeText("2")
                setLastModified(2_000L)
            }
            val f3 = File(tempDir, "new.mp3").apply {
                writeText("3")
                setLastModified(3_000L)
            }
            val inFlightPart = File(tempDir, "active.part.mp3").apply {
                writeText("part")
                setLastModified(500L)
            }

            DesktopAudioCacheTranscoder.pruneOldCacheFiles(tempDir, maxFiles = 2)

            assertFalse(f1.exists())
            assertTrue(f2.exists())
            assertTrue(f3.exists())
            assertTrue(inFlightPart.exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
