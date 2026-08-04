import { EventEmitter } from '../core/EventEmitter.js';

/**
 * QueueManager handles playback queue indexing, track ordering, shuffle, and repeat modes.
 */
export class QueueManager extends EventEmitter {
    #queue = [];
    #currentIndex = -1;
    #isShuffle = false;
    #isRepeat = false;

    /** Returns copy of active queue */
    get queue() { return [...this.#queue]; }

    /** Returns current track index */
    get currentIndex() { return this.#currentIndex; }

    /** Returns current playing track or null */
    get currentTrack() {
        return (this.#currentIndex >= 0 && this.#currentIndex < this.#queue.length) ? this.#queue[this.#currentIndex] : null;
    }

    /** Returns true if shuffle is enabled */
    get isShuffle() { return this.#isShuffle; }

    /** Returns true if repeat is enabled */
    get isRepeat() { return this.#isRepeat; }

    /**
     * Sets queue list and starts at specified index.
     * @param {Array<Object>} tracks
     * @param {number} startIndex
     */
    setQueue(tracks = [], startIndex = 0) {
        this.#queue = [...tracks];
        this.#currentIndex = startIndex;
        this.emit('queueChanged', { queue: this.#queue, index: this.#currentIndex });
    }

    /**
     * Adds a track to the queue.
     * @param {Object} track
     */
    addTrack(track) {
        this.#queue.push(track);
        this.emit('queueChanged', { queue: this.#queue, index: this.#currentIndex });
    }

    /**
     * Advances to the next track in queue according to repeat/shuffle rules.
     * @returns {Object|null} Next track or null.
     */
    next() {
        if (this.#queue.length === 0) return null;

        if (this.#isShuffle) {
            this.#currentIndex = Math.floor(Math.random() * this.#queue.length);
        } else {
            this.#currentIndex++;
            if (this.#currentIndex >= this.#queue.length) {
                if (this.#isRepeat) {
                    this.#currentIndex = 0;
                } else {
                    this.#currentIndex = this.#queue.length - 1;
                    return null;
                }
            }
        }
        const track = this.currentTrack;
        this.emit('trackChanged', { track, index: this.#currentIndex });
        return track;
    }

    /**
     * Returns to previous track in queue.
     * @returns {Object|null} Previous track or null.
     */
    previous() {
        if (this.#queue.length === 0) return null;
        this.#currentIndex = Math.max(0, this.#currentIndex - 1);
        const track = this.currentTrack;
        this.emit('trackChanged', { track, index: this.#currentIndex });
        return track;
    }

    /** Toggles shuffle mode */
    toggleShuffle() {
        this.#isShuffle = !this.#isShuffle;
        this.emit('modeChanged', { shuffle: this.#isShuffle, repeat: this.#isRepeat });
        return this.#isShuffle;
    }

    /** Toggles repeat mode */
    toggleRepeat() {
        this.#isRepeat = !this.#isRepeat;
        this.emit('modeChanged', { shuffle: this.#isShuffle, repeat: this.#isRepeat });
        return this.#isRepeat;
    }
}
