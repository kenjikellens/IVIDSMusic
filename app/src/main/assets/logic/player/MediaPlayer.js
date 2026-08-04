import { BaseService } from '../core/BaseService.js';
import { MediaAudioEngine } from './MediaAudioEngine.js';
import { QueueManager } from './QueueManager.js';
import { MusicRepository } from '../api/MusicRepository.js';
import { StorageFactory } from '../storage/StorageFactory.js';

/**
 * MediaPlayerService provides an integrated facade for audio playback, queueing, and UI player bar binding.
 */
export class MediaPlayerService extends BaseService {
    #audioEngine = new MediaAudioEngine();
    #queueManager = new QueueManager();

    constructor() {
        super();
        this.#bindEngineEvents();
    }

    #bindEngineEvents() {
        this.#audioEngine.on('play', () => this.emit('play'));
        this.#audioEngine.on('pause', () => this.emit('pause'));
        this.#audioEngine.on('ended', () => {
            this.emit('ended');
            this.next();
        });
        this.#queueManager.on('trackChanged', ({ track }) => this.emit('trackChanged', track));
    }

    /** Returns audio engine */
    get audioEngine() { return this.#audioEngine; }

    /** Returns queue manager */
    get queueManager() { return this.#queueManager; }

    /** Returns current playing track */
    get currentTrack() { return this.#queueManager.currentTrack; }

    /** Returns playing state */
    get isPlaying() { return this.#audioEngine.isPlaying; }

    /**
     * Initializes player UI bindings.
     */
    async init() {
        if (this.isInitialized) return;
        this.bindUI();
        this._setInitialized(true);
    }

    /**
     * Plays a specific track item, resolving its stream URL if necessary.
     * @param {Object} track
     */
    async playTrack(track) {
        if (!track) return;
        try {
            let streamUrl = track.url || track.audioUrl;
            if (!streamUrl && track.videoId) {
                streamUrl = await MusicRepository.getAudioStreamUrl(track.videoId);
            }
            if (!streamUrl && track.title && track.artist) {
                const searchRes = await MusicRepository.search(`${track.artist} ${track.title}`, 1);
                if (searchRes.length > 0 && searchRes[0].id) {
                    streamUrl = await MusicRepository.getAudioStreamUrl(searchRes[0].id);
                }
            }

            if (streamUrl) {
                track.audioUrl = streamUrl;
                await this.#audioEngine.play(streamUrl);
                this.emit('trackStarted', track);
            }
        } catch (err) {
            console.error('[MediaPlayer] Error playing track:', err);
        }
    }

    /** Toggles play / pause state */
    toggle() {
        this.#audioEngine.toggle();
    }

    /** Plays next track in queue */
    async next() {
        const nextTrack = this.#queueManager.next();
        if (nextTrack) await this.playTrack(nextTrack);
    }

    /** Plays previous track in queue */
    async previous() {
        const prevTrack = this.#queueManager.previous();
        if (prevTrack) await this.playTrack(prevTrack);
    }

    /**
     * Binds player DOM elements (play button, progress slider, volume slider, time labels).
     */
    bindUI() {
        const playBtn = document.getElementById('play-pause-btn');
        const progressSlider = document.getElementById('progress-slider');
        const volumeSlider = document.getElementById('volume-slider');

        if (playBtn) {
            playBtn.onclick = () => this.toggle();
        }
        if (volumeSlider) {
            volumeSlider.oninput = (e) => {
                const val = parseFloat(e.target.value) / 100;
                this.#audioEngine.volume = val;
            };
        }
    }
}

/** MediaPlayer singleton instance */
export const MediaPlayer = new MediaPlayerService();
