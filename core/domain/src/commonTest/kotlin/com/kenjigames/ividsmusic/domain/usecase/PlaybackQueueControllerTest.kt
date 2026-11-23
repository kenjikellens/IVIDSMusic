package com.kenjigames.ividsmusic.domain.usecase

import com.kenjigames.ividsmusic.domain.model.PlaybackSessionSnapshot
import com.kenjigames.ividsmusic.domain.model.RepeatMode
import com.kenjigames.ividsmusic.domain.model.Track
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests covering queue navigation, repeat modes, shuffle preservation, and reordering in [PlaybackQueueController].
 */
class PlaybackQueueControllerTest {

    private val tracks = listOf(
        Track(id = "1", title = "Track A", artist = "Artist 1"),
        Track(id = "2", title = "Track B", artist = "Artist 2"),
        Track(id = "3", title = "Track C", artist = "Artist 3"),
        Track(id = "4", title = "Track D", artist = "Artist 4")
    )

    /** Ensures an uninitialized controller starts empty with no active track. */
    @Test
    fun emptyQueueInitializesSafely() {
        val controller = PlaybackQueueController(Random(42))
        assertNull(controller.state.value.currentTrack)
        assertEquals(-1, controller.state.value.currentIndex)
        assertNull(controller.next())
        assertNull(controller.previous())
    }

    /** Verifies forward and backward navigation with RepeatMode.OFF stops cleanly at queue ends. */
    @Test
    fun sequentialNavigationAndRepeatOffBoundary() {
        val controller = PlaybackQueueController(Random(42))
        controller.setQueue(tracks, startIndex = 0)

        assertEquals("1", controller.state.value.currentTrack?.id)
        assertEquals("2", controller.next()?.id)
        assertEquals("3", controller.next()?.id)
        assertEquals("4", controller.next()?.id)
        assertNull(controller.next())
        assertEquals(3, controller.state.value.currentIndex)

        assertEquals("3", controller.previous()?.id)
        controller.jumpTo(0)
        assertEquals("1", controller.previous()?.id)
        assertEquals(0, controller.state.value.currentIndex)
    }

    /** Verifies RepeatMode.ALL wraps across boundaries while RepeatMode.ONE repeats the active track. */
    @Test
    fun repeatAllAndRepeatOneModes() {
        val controller = PlaybackQueueController(Random(42))
        controller.setQueue(tracks, startIndex = 3)

        assertEquals(RepeatMode.ALL, controller.cycleRepeatMode())
        assertEquals("1", controller.next()?.id)
        assertEquals("4", controller.previous()?.id)

        assertEquals(RepeatMode.ONE, controller.cycleRepeatMode())
        assertEquals("4", controller.next(respectRepeatOne = true)?.id)
        assertEquals("1", controller.next(respectRepeatOne = false)?.id)

        assertEquals(RepeatMode.OFF, controller.cycleRepeatMode())
    }

    /** Verifies enabling shuffle pins the current track at index 0 and disabling restores original track order. */
    @Test
    fun shufflePinsActiveTrackAndRestoresOriginalOrderOnDisable() {
        val controller = PlaybackQueueController(Random(7))
        controller.setQueue(tracks, startIndex = 2)
        assertEquals("3", controller.state.value.currentTrack?.id)

        assertTrue(controller.toggleShuffle())
        assertEquals(0, controller.state.value.currentIndex)
        assertEquals("3", controller.state.value.currentTrack?.id)
        assertEquals(setOf("1", "2", "3", "4"), controller.state.value.queue.map { it.id }.toSet())

        assertFalse(controller.toggleShuffle())
        assertEquals(listOf("1", "2", "3", "4"), controller.state.value.queue.map { it.id })
        assertEquals(2, controller.state.value.currentIndex)
        assertEquals("3", controller.state.value.currentTrack?.id)
    }

    /** Verifies playNext, addToQueue, moveTrack, and removeAt keep currentIndex tracking the playing song. */
    @Test
    fun queueMutationHelpersMaintainCurrentPointer() {
        val controller = PlaybackQueueController(Random(1))
        val extra = Track(id = "99", title = "Next Up", artist = "Extra")
        controller.setQueue(tracks.take(3), startIndex = 1) // Playing "2"

        controller.playNext(extra)
        assertEquals(listOf("1", "2", "99", "3"), controller.state.value.queue.map { it.id })
        assertEquals("2", controller.state.value.currentTrack?.id)

        controller.moveTrack(fromIndex = 3, toIndex = 0)
        assertEquals(listOf("3", "1", "2", "99"), controller.state.value.queue.map { it.id })
        assertEquals(2, controller.state.value.currentIndex)
        assertEquals("2", controller.state.value.currentTrack?.id)

        controller.removeAt(0)
        assertEquals(listOf("1", "2", "99"), controller.state.value.queue.map { it.id })
        assertEquals(1, controller.state.value.currentIndex)
    }

    /** Verifies peekNext respects RepeatMode without mutating index and restoreState restores snapshots safely. */
    @Test
    fun peekNextAndRestoreSessionPreserveOrderAndBounds() {
        val controller = PlaybackQueueController(Random(9))
        controller.restoreState(
            PlaybackSessionSnapshot(
                queue = tracks,
                currentIndex = 99,
                positionMs = 45_000L,
                isShuffleEnabled = false,
                repeatMode = RepeatMode.ALL
            )
        )
        assertEquals(3, controller.state.value.currentIndex)
        assertEquals("4", controller.state.value.currentTrack?.id)
        assertEquals("1", controller.peekNext()?.id)
        assertEquals(3, controller.state.value.currentIndex)

        controller.setRepeatMode(RepeatMode.OFF)
        assertNull(controller.peekNext())
        controller.jumpTo(1)
        assertEquals("3", controller.peekNext()?.id)
        assertEquals(1, controller.state.value.currentIndex)
    }
}
