package com.kenjigames.ividsmusic

import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.player.PlaybackQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying audio queue index updates, navigation limits, shuffle, and repeat modes.
 */
class PlaybackQueueTest {

    private val testSongs = listOf(
        Song("1", "Song A", "Artist X", coverUrl = ""),
        Song("2", "Song B", "Artist Y", coverUrl = ""),
        Song("3", "Song C", "Artist Z", coverUrl = "")
    )

    @Test
    fun testEmptyQueueInitialization() {
        val queue = PlaybackQueue()
        assertNull(queue.currentSong)
        assertEquals(-1, queue.currentIndex)
    }

    @Test
    fun testQueueNavigation() {
        val queue = PlaybackQueue(testSongs, 0)
        
        // Assert initial
        assertEquals("Song A", queue.currentSong?.title)
        assertEquals(0, queue.currentIndex)

        // Go Next
        val nextSong = queue.next()
        assertNotNull(nextSong)
        assertEquals("Song B", nextSong?.title)
        assertEquals(1, queue.currentIndex)

        // Go Previous
        val prevSong = queue.previous()
        assertNotNull(prevSong)
        assertEquals("Song A", prevSong?.title)
        assertEquals(0, queue.currentIndex)
    }

    @Test
    fun testCircularQueueNavigation() {
        val queue = PlaybackQueue(testSongs, 2)
        assertEquals("Song C", queue.currentSong?.title)

        // Wrap next to index 0
        val wrappedNext = queue.next()
        assertEquals("Song A", wrappedNext?.title)
        assertEquals(0, queue.currentIndex)

        // Wrap previous to index 2
        val wrappedPrev = queue.previous()
        assertEquals("Song C", wrappedPrev?.title)
        assertEquals(2, queue.currentIndex)
    }

    @Test
    fun testRepeatOneTrackMode() {
        val queue = PlaybackQueue(testSongs, 1)
        queue.toggleRepeatOne()
        assertTrue(queue.isRepeatOne)

        // Navigating next should stay on same song
        val next = queue.next()
        assertEquals("Song B", next?.title)
        assertEquals(1, queue.currentIndex)
    }

    @Test
    fun testToggleShuffleMode() {
        val queue = PlaybackQueue(testSongs, 0)
        assertFalse(queue.isShuffleEnabled)

        queue.toggleShuffle()
        assertTrue(queue.isShuffleEnabled)

        // Toggling off restores original order
        queue.toggleShuffle()
        assertFalse(queue.isShuffleEnabled)
    }
}
