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
        this.#queueManager.setQueue([track], 0);
        this.#updatePlayerBarInfo(track);
        this.#enableActionButtons(true);
        try {
            let streamUrl = track.url || track.audioUrl || track.previewUrl || track.preview;
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

    #enableActionButtons(enabled) {
        ['save-track-btn', 'like-track-btn', 'dislike-track-btn', 'more-info-btn'].forEach(id => {
            const btn = document.getElementById(id);
            if (btn) btn.disabled = !enabled;
        });
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

    /** Saves active playing track */
    async saveTrack() {
        const track = this.currentTrack;
        if (!track) return;
        if (window.DownloadManager) {
            window.DownloadManager.enqueueTrack(track);
        }
    }

    /** Toggles like state */
    toggleLike() {
        const track = this.currentTrack;
        if (!track) return;
        const btn = document.getElementById('like-track-btn');
        if (btn) btn.classList.toggle('active');
    }

    /** Toggles dislike state */
    toggleDislike() {
        const track = this.currentTrack;
        if (!track) return;
        const btn = document.getElementById('dislike-track-btn');
        if (btn) btn.classList.toggle('active');
    }

    /** Clears queue */
    clearQueue() {
        this.#queueManager.clear();
        const nowPlayingList = document.getElementById('queue-now-playing');
        const upcomingList = document.getElementById('queue-upcoming');
        if (nowPlayingList) nowPlayingList.innerHTML = '';
        if (upcomingList) upcomingList.innerHTML = '';
    }

    /**
     * Binds player DOM elements (play button, progress slider, volume slider, time labels, queue drawer).
     */
    bindUI() {
        const playBtn = document.getElementById('play-pause-btn');
        const progressSlider = document.getElementById('progress-slider');
        const volumeSlider = document.getElementById('volume-slider');
        const queueBtn = document.getElementById('queue-toggle-btn');
        const closeQueueBtn = document.getElementById('close-queue-btn');
        const queueDrawer = document.getElementById('queue-drawer');

        if (playBtn) {
            playBtn.onclick = () => this.toggle();
        }
        if (queueBtn && queueDrawer) {
            queueBtn.onclick = () => queueDrawer.classList.toggle('is-active');
        }
        if (closeQueueBtn && queueDrawer) {
            closeQueueBtn.onclick = () => queueDrawer.classList.remove('is-active');
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

if (typeof window !== 'undefined') {
    window.MediaPlayer = MediaPlayer;
    window.YouTubePlayer = MediaPlayer;
}
