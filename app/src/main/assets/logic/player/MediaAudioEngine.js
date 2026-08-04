import { EventEmitter } from '../core/EventEmitter.js';

/**
 * MediaAudioEngine encapsulates HTML5 Audio element playback, volume, seeking, and event hooks.
 */
export class MediaAudioEngine extends EventEmitter {
    #audio = new Audio();
    #isPlaying = false;

    constructor() {
        super();
        this.#bindAudioEvents();
    }

    #bindAudioEvents() {
        this.#audio.onplay = () => {
            this.#isPlaying = true;
            this.emit('play');
        };

        this.#audio.onpause = () => {
            this.#isPlaying = false;
            this.emit('pause');
        };

        this.#audio.onended = () => {
            this.#isPlaying = false;
            this.emit('ended');
        };

        this.#audio.onloadedmetadata = () => {
            this.emit('loadedmetadata', { duration: this.#audio.duration });
        };

        this.#audio.onerror = (err) => {
            this.emit('error', err);
        };
    }

    /** Returns native audio element */
    get audio() { return this.#audio; }

    /** Returns playing state */
    get isPlaying() { return this.#isPlaying; }

    /** Returns current playback position in seconds */
    get currentTime() { return this.#audio.currentTime; }
    set currentTime(seconds) { this.#audio.currentTime = seconds; }

    /** Returns total audio duration in seconds */
    get duration() { return this.#audio.duration || 0; }

    /** Returns volume level (0 to 1) */
    get volume() { return this.#audio.volume; }
    set volume(val) { this.#audio.volume = Math.max(0, Math.min(1, val)); }

    /**
     * Sets audio source and begins playback.
     * @param {string} url
     */
    async play(url) {
        if (url && this.#audio.src !== url) {
            this.#audio.src = url;
        }
        await this.#audio.play();
    }

    /** Pauses active playback */
    pause() {
        this.#audio.pause();
    }

    /** Toggles play/pause state */
    toggle() {
        if (this.#isPlaying) {
            this.pause();
        } else {
            this.#audio.play();
        }
    }
}
