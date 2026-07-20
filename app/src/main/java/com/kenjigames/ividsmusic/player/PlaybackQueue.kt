package com.kenjigames.ividsmusic.player

import com.kenjigames.ividsmusic.domain.model.Song

/**
 * Data structure managing the active audio playback queue, track indexing, shuffle, and repeat modes.
 */
class PlaybackQueue(
    initialTracks: List<Song> = emptyList(),
    initialIndex: Int = 0
) {
    private val originalTracks: MutableList<Song> = initialTracks.toMutableList()
    private val activeQueue: MutableList<Song> = initialTracks.toMutableList()
    
    var currentIndex: Int = if (activeQueue.isNotEmpty()) initialIndex.coerceIn(0, activeQueue.size - 1) else -1
        private set

    var isShuffleEnabled: Boolean = false
        private set

    var isRepeatOne: Boolean = false
        private set

    /** Gets the currently active song in the queue */
    val currentSong: Song?
        get() = if (currentIndex in activeQueue.indices) activeQueue[currentIndex] else null

    /** Sets a new queue and plays from index */
    fun setQueue(tracks: List<Song>, startIndex: Int = 0) {
        originalTracks.clear()
        originalTracks.addAll(tracks)
        activeQueue.clear()
        
        if (isShuffleEnabled) {
            activeQueue.addAll(originalTracks.shuffled())
        } else {
            activeQueue.addAll(originalTracks)
        }
        
        currentIndex = if (activeQueue.isNotEmpty()) startIndex.coerceIn(0, activeQueue.size - 1) else -1
    }

    /** Navigates to the next song in the queue */
    fun next(): Song? {
        if (activeQueue.isEmpty()) return null
        if (isRepeatOne) return currentSong
        currentIndex = (currentIndex + 1) % activeQueue.size
        return currentSong
    }

    /** Navigates to the previous song in the queue */
    fun previous(): Song? {
        if (activeQueue.isEmpty()) return null
        if (isRepeatOne) return currentSong
        currentIndex = (currentIndex - 1 + activeQueue.size) % activeQueue.size
        return currentSong
    }

    /** Toggles queue shuffle mode */
    fun toggleShuffle(): Boolean {
        isShuffleEnabled = !isShuffleEnabled
        val current = currentSong
        activeQueue.clear()
        if (isShuffleEnabled) {
            activeQueue.addAll(originalTracks.shuffled())
        } else {
            activeQueue.addAll(originalTracks)
        }
        currentIndex = if (current != null) activeQueue.indexOf(current).coerceAtLeast(0) else 0
        return isShuffleEnabled
    }

    /** Toggles repeat one track mode */
    fun toggleRepeatOne(): Boolean {
        isRepeatOne = !isRepeatOne
        return isRepeatOne
    }
}
