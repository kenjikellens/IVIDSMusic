import { BaseService } from '../core/BaseService.js';
import { MediaAudioEngine } from './MediaAudioEngine.js';
import { QueueManager } from './QueueManager.js';
import { MusicRepository } from '../api/MusicRepository.js';

/**
 * MediaPlayerService provides an integrated facade for audio playback, queueing, UI player bar binding,
 * and 60fps requestAnimationFrame progress updates.
 */
export class MediaPlayerService extends BaseService {
    #audioEngine = new MediaAudioEngine();
    #queueManager = new QueueManager();
    #rafId = null;
    #isDraggingSlider = false;

    constructor() {
        super();
        this.#bindEngineEvents();
    }

    #bindEngineEvents() {
        this.#audioEngine.on('play', () => {
            this.emit('play');
            this.#startProgressLoop();
            this.#updatePlayButtonState(true);
        });

        this.#audioEngine.on('pause', () => {
            this.emit('pause');
            this.#stopProgressLoop();
            this.#updatePlayButtonState(false);
        });

        this.#audioEngine.on('ended', () => {
            this.emit('ended');
            this.#stopProgressLoop();
            this.next();
        });

        this.#queueManager.on('trackChanged', ({ track }) => {
            this.emit('trackChanged', track);
            this.#updatePlayerBarInfo(track);
        });
    }

    #startProgressLoop() {
        this.#stopProgressLoop();
        const update = () => {
            if (this.#audioEngine.isPlaying && !this.#isDraggingSlider) {
                this.#updateProgressUI();
            }
            if (this.#audioEngine.isPlaying) {
                this.#rafId = requestAnimationFrame(update);
            }
        };
        this.#rafId = requestAnimationFrame(update);
    }

    #stopProgressLoop() {
        if (this.#rafId) {
            cancelAnimationFrame(this.#rafId);
            this.#rafId = null;
        }
    }

    #updateProgressUI() {
        const currentTimeEl = document.getElementById('current-time');
        const durationEl = document.getElementById('total-duration');
        const slider = document.getElementById('progress-slider');

        const current = this.#audioEngine.currentTime;
        const duration = this.#audioEngine.duration;

        if (currentTimeEl) currentTimeEl.textContent = this.formatTime(current);
        if (durationEl && duration) durationEl.textContent = this.formatTime(duration);

        if (slider && duration) {
            const pct = (current / duration) * 100;
            slider.value = pct;
            slider.style.setProperty('--slider-val', `${pct}%`);
        }
    }

    #updatePlayButtonState(isPlaying) {
        const playBtn = document.getElementById('play-pause-btn');
        const playIcon = document.getElementById('play-icon');
        const playerBar = document.getElementById('player-bar');

        if (playerBar) playerBar.classList.remove('is-inactive');
        if (playIcon) {
            playIcon.src = isPlaying ? 'svg/pause.svg' : 'svg/play.svg';
        } else if (playBtn) {
            playBtn.innerHTML = isPlaying
                ? '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>'
                : '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>';
        }
    }

    #updatePlayerBarInfo(track) {
        if (!track) return;
        const coverEl = document.getElementById('player-cover');
        const titleEl = document.getElementById('player-title');
        const artistEl = document.getElementById('player-artist');

        if (coverEl) coverEl.src = track.cover || 'gui/gemini-logo.png';
        if (titleEl) titleEl.textContent = track.title || track.name || '—';
        if (artistEl) artistEl.textContent = track.artist || '—';
    }

    /** Formats seconds into MM:SS display string */
    formatTime(seconds) {
        if (!seconds || isNaN(seconds)) return '0:00';
        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
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
        this.#updatePlayerBarInfo(track);
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
        if (progressSlider) {
            progressSlider.onmousedown = () => { this.#isDraggingSlider = true; };
            progressSlider.ontouchstart = () => { this.#isDraggingSlider = true; };
            const onRelease = () => {
                if (!this.#isDraggingSlider) return;
                this.#isDraggingSlider = false;
                if (this.#audioEngine.duration) {
                    const time = (progressSlider.value / 100) * this.#audioEngine.duration;
                    this.#audioEngine.currentTime = time;
                }
            };
            progressSlider.onmouseup = onRelease;
            progressSlider.ontouchend = onRelease;
        }
    }
}

/** MediaPlayer singleton instance */
export const MediaPlayer = new MediaPlayerService();
export const YouTubePlayer = MediaPlayer;
