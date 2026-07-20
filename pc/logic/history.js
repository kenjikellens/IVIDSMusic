/**
 * IVIDS Music - History System
 * Dedicated module for managing playback history (iv_recent_tracks).
 */
export const HistorySystem = {
  STORAGE_KEY: "iv_recent_tracks",
  MAX_ITEMS: 20,

  /**
   * Get all history items
   */
  get() {
    try {
      const data = localStorage.getItem(this.STORAGE_KEY);
      return JSON.parse(data || "[]");
    } catch (e) {
      console.error("[History] Error reading history", e);
      return [];
    }
  },

  /**
   * Add a track to history
   * @param {Object} track - The track to add
   */
  add(track) {
    if (!track || !track.title || !track.artist) {
      console.warn("[History] Attempted to add invalid track", track);
      return;
    }

    // DEBUG: Track the caller to find ghost entries
    console.group("--- [History] Add Track ---");
    console.log("Track:", track.title, "-", track.artist);
    console.trace("Trace:");
    console.groupEnd();

    const history = this.get();

    // Remove existing copy if present (move to top)
    const filtered = history.filter(
      (t) => !(t.title === track.title && t.artist === track.artist),
    );

    // Add to front
    filtered.unshift(track);

    // Limit size
    const bounded = filtered.slice(0, this.MAX_ITEMS);

    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(bounded));
    } catch (e) {
      console.error("[History] Error saving history", e);
    }
  },

  /**
   * Add listening time (in seconds) to the total counter.
   * @param {number} seconds
   */
  addListeningTime(seconds) {
    if (!seconds || seconds <= 0) return;
    try {
      const current = parseFloat(
        localStorage.getItem("iv_listening_seconds") || "0",
      );
      localStorage.setItem("iv_listening_seconds", String(current + seconds));
    } catch (e) {
      console.error("[History] Error saving listening time", e);
    }
  },

  /**
   * Get total listening time in minutes (floored).
   * @returns {number}
   */
  getTotalMinutes() {
    try {
      const seconds = parseFloat(
        localStorage.getItem("iv_listening_seconds") || "0",
      );
      return Math.floor(seconds / 60);
    } catch (e) {
      return 0;
    }
  },

  /**
   * Clear all history
   */
  clear() {
    console.log("[History] Clearing all history");
    localStorage.removeItem(this.STORAGE_KEY);
  },
};

// Export to window for easy access from non-module scripts if needed
if (typeof window !== "undefined") {
  window.HistorySystem = HistorySystem;
}
