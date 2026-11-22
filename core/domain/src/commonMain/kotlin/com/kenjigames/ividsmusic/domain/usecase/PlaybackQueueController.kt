package com.kenjigames.ividsmusic.domain.usecase

import com.kenjigames.ividsmusic.domain.model.RepeatMode
import com.kenjigames.ividsmusic.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Immutable snapshot of playback queue order, active pointer, shuffle flag, and repeat mode.
 * Exposed via [PlaybackQueueController.state] for player coordination and Queue sheet UI.
 */
data class QueueState(
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
) {
    /** Currently active [Track] pointed to by [currentIndex], or `null` when out of bounds. */
    val currentTrack: Track?
        get() = queue.getOrNull(currentIndex)
}

/**
 * Pure domain state controller managing playback queue ordering, deterministic shuffle/unshuffle, and repeat modes.
 * Mutates internal [QueueState] flow without performing any platform I/O or audio engine calls.
 */
class PlaybackQueueController(
    private val random: Random = Random.Default
) {
    private val _state = MutableStateFlow(QueueState())

    /** Observable queue snapshot consumed by PlayerViewModel and Queue UI components. */
    val state: StateFlow<QueueState> = _state.asStateFlow()

    private var originalQueue: List<Track> = emptyList()

    /**
     * Replaces the active queue with [tracks] starting at [startIndex], applying shuffle pinning if enabled.
     * Mutates [state] and [originalQueue], returning the resulting current [Track] or `null` if empty.
     */
    fun setQueue(tracks: List<Track>, startIndex: Int = 0): Track? {
        if (tracks.isEmpty()) {
            originalQueue = emptyList()
            _state.value = _state.value.copy(queue = emptyList(), currentIndex = -1)
            return null
        }

        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        originalQueue = tracks.toList()
        val current = _state.value

        if (!current.isShuffleEnabled) {
            _state.value = current.copy(queue = originalQueue, currentIndex = safeIndex)
            return originalQueue[safeIndex]
        }

        val anchor = tracks[safeIndex]
        val remaining = tracks.filterIndexed { index, _ -> index != safeIndex }.shuffled(random)
        val shuffled = listOf(anchor) + remaining
        _state.value = current.copy(queue = shuffled, currentIndex = 0)
        return anchor
    }

    /**
     * Appends [track] to the end of both the active queue and [originalQueue].
     * Advances `currentIndex` to `0` if the queue was previously empty.
     */
    fun addToQueue(track: Track) {
        originalQueue = originalQueue + track
        val current = _state.value
        val updatedQueue = current.queue + track
        val updatedIndex = if (current.currentIndex < 0) 0 else current.currentIndex
        _state.value = current.copy(queue = updatedQueue, currentIndex = updatedIndex)
    }

    /**
     * Inserts [track] immediately after the current track so it plays next.
     * Updates [state] and [originalQueue], initializing `currentIndex = 0` when the queue was empty.
     */
    fun playNext(track: Track) {
        val current = _state.value
        if (current.queue.isEmpty() || current.currentIndex !in current.queue.indices) {
            setQueue(listOf(track), startIndex = 0)
            return
        }

        val insertIndex = (current.currentIndex + 1).coerceAtMost(current.queue.size)
        val updatedQueue = current.queue.toMutableList().apply { add(insertIndex, track) }
        val activeId = current.currentTrack?.id
        val origIndex = originalQueue.indexOfFirst { it.id == activeId }
        originalQueue = if (origIndex >= 0) {
            originalQueue.toMutableList().apply { add(origIndex + 1, track) }
        } else {
            originalQueue + track
        }
        _state.value = current.copy(queue = updatedQueue)
    }

    /**
     * Removes the item at [index] from the queue and adjusts `currentIndex` safely.
     * No-ops cleanly if [index] is outside valid bounds, mutating [state] and [originalQueue] otherwise.
     */
    fun removeAt(index: Int) {
        val current = _state.value
        if (index !in current.queue.indices) return

        val removed = current.queue[index]
        val updatedQueue = current.queue.toMutableList().apply { removeAt(index) }
        originalQueue = originalQueue.toMutableList().apply {
            val origIdx = indexOfFirst { it.id == removed.id }
            if (origIdx >= 0) removeAt(origIdx)
        }

        val updatedIndex = when {
            updatedQueue.isEmpty() -> -1
            index < current.currentIndex -> current.currentIndex - 1
            index == current.currentIndex -> current.currentIndex.coerceAtMost(updatedQueue.lastIndex)
            else -> current.currentIndex
        }
        _state.value = current.copy(queue = updatedQueue, currentIndex = updatedIndex)
    }

    /**
     * Moves a track from [fromIndex] to [toIndex] within the active queue while keeping `currentIndex` aligned.
     * Mutates [state] (and [originalQueue] when shuffle is off) and guards out-of-range indices.
     */
    fun moveTrack(fromIndex: Int, toIndex: Int) {
        val current = _state.value
        if (fromIndex !in current.queue.indices || toIndex !in current.queue.indices || fromIndex == toIndex) {
            return
        }

        val updatedQueue = current.queue.toMutableList()
        val movedItem = updatedQueue.removeAt(fromIndex)
        updatedQueue.add(toIndex, movedItem)

        val updatedIndex = when (current.currentIndex) {
            fromIndex -> toIndex
            in (fromIndex + 1)..toIndex -> current.currentIndex - 1
            in toIndex until fromIndex -> current.currentIndex + 1
            else -> current.currentIndex
        }

        if (!current.isShuffleEnabled) {
            originalQueue = updatedQueue.toList()
        }
        _state.value = current.copy(queue = updatedQueue, currentIndex = updatedIndex)
    }

    /**
     * Moves `currentIndex` directly to [index] and returns the selected [Track], or `null` if invalid.
     * Mutates [state] only when [index] is within the active queue bounds.
     */
    fun jumpTo(index: Int): Track? {
        val current = _state.value
        if (index !in current.queue.indices) return null
        _state.value = current.copy(currentIndex = index)
        return current.queue[index]
    }

    /**
     * Advances to the next track according to [RepeatMode], returning `null` if the queue ends with repeat off.
     * When [respectRepeatOne] is true and [RepeatMode.ONE] is active, stays on the current track; manual skip wraps at tail.
     */
    fun next(respectRepeatOne: Boolean = true): Track? {
        val current = _state.value
        if (current.queue.isEmpty()) return null
        if (respectRepeatOne && current.repeatMode == RepeatMode.ONE) {
            return current.currentTrack
        }

        val nextIndex = current.currentIndex + 1
        if (nextIndex < current.queue.size) {
            _state.value = current.copy(currentIndex = nextIndex)
            return current.queue[nextIndex]
        }

        return if (current.repeatMode == RepeatMode.ALL ||
            (!respectRepeatOne && current.repeatMode == RepeatMode.ONE)
        ) {
            _state.value = current.copy(currentIndex = 0)
            current.queue[0]
        } else {
            null
        }
    }

    /**
     * Moves back to the previous track, wrapping to the tail only when [RepeatMode.ALL] is enabled.
     * Mutates [state] and returns the resulting [Track] or `null` when the queue is empty.
     */
    fun previous(): Track? {
        val current = _state.value
        if (current.queue.isEmpty()) return null

        if (current.currentIndex > 0) {
            val prevIndex = current.currentIndex - 1
            _state.value = current.copy(currentIndex = prevIndex)
            return current.queue[prevIndex]
        }

        val targetIndex = if (current.repeatMode == RepeatMode.ALL) current.queue.lastIndex else 0
        _state.value = current.copy(currentIndex = targetIndex)
        return current.queue[targetIndex]
    }

    /**
     * Toggles shuffle mode, pinning the active track at index `0` when enabling or restoring [originalQueue] when disabling.
     * Mutates [state] and returns the new `isShuffleEnabled` flag.
     */
    fun toggleShuffle(): Boolean {
        val current = _state.value
        val enabling = !current.isShuffleEnabled
        if (current.queue.isEmpty()) {
            _state.value = current.copy(isShuffleEnabled = enabling)
            return enabling
        }

        if (enabling) {
            val activeTrack = current.currentTrack
            val remaining = current.queue
                .filterIndexed { index, _ -> index != current.currentIndex }
                .shuffled(random)
            val shuffled = if (activeTrack != null) listOf(activeTrack) + remaining else remaining
            val nextIndex = if (shuffled.isEmpty()) -1 else 0
            _state.value = current.copy(
                queue = shuffled,
                currentIndex = nextIndex,
                isShuffleEnabled = true
            )
            return true
        }

        val activeId = current.currentTrack?.id
        val restoredIndex = originalQueue.indexOfFirst { it.id == activeId }
            .let { if (it >= 0) it else if (originalQueue.isEmpty()) -1 else 0 }
        _state.value = current.copy(
            queue = originalQueue,
            currentIndex = restoredIndex,
            isShuffleEnabled = false
        )
        return false
    }

    /**
     * Cycles repeat mode cleanly through `OFF -> ALL -> ONE -> OFF` and mutates [state].
     * Returns the newly active [RepeatMode].
     */
    fun cycleRepeatMode(): RepeatMode {
        val nextMode = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _state.value = _state.value.copy(repeatMode = nextMode)
        return nextMode
    }

    /**
     * Sets an explicit [RepeatMode] on [state] while leaving queue items and index unchanged.
     */
    fun setRepeatMode(mode: RepeatMode) {
        _state.value = _state.value.copy(repeatMode = mode)
    }

    /**
     * Empties both the active queue and [originalQueue] while preserving shuffle and repeat mode toggles.
     */
    fun clear() {
        originalQueue = emptyList()
        _state.value = _state.value.copy(queue = emptyList(), currentIndex = -1)
    }
}
