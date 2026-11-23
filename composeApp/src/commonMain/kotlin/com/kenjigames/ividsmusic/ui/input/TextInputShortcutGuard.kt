package com.kenjigames.ividsmusic.ui.input

/**
 * Tracks whether a Compose text field currently owns an active platform IME/input session.
 * Prevents single-key desktop playback shortcuts (`Space`, `M`, arrows) from firing while typing.
 */
object TextInputShortcutGuard {
    @Volatile
    private var activeInputSessions: Int = 0

    /** Returns `true` when any text input field is currently active in the Compose hierarchy. */
    fun isTyping(): Boolean = activeInputSessions > 0

    /** Increments active input session depth when a text field begins editing. */
    fun onSessionStarted() {
        activeInputSessions = (activeInputSessions + 1).coerceAtLeast(1)
    }

    /** Decrements active input session depth cleanly once a text field loses focus or disposes. */
    fun onSessionEnded() {
        activeInputSessions = (activeInputSessions - 1).coerceAtLeast(0)
    }
}
