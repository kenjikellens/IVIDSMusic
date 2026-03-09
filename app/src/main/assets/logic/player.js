import { MusicAPI } from './api.js';
import { Config } from './config.js';
import { HistorySystem } from './history.js';
import { DiscoveryEngine } from './recommendations.js';

export const YouTubePlayer = {
    audio: new Audio(),
    isPlaying: false,
    currentTrack: null,
    isInitialized: false,
    queue: [],
    currentIndex: -1,
    animationFrameId: null,
    isDraggingSlider: false,
    _listenScored: false,
    _completionScored: false,
    _lastPlayTimestamp: 0,

    init() {
        if (this.isInitialized) return;

        const playerBar = document.getElementById('player-bar');
        if (!playerBar) return;

        // Get references to existing DOM elements
        const playBtn = document.getElementById('play-pause-btn');
        const progressSlider = document.getElementById('progress-slider');
        const volumeSlider = document.getElementById('volume-slider');
        const currentTimeEl = document.getElementById('current-time');
        const durationEl = document.getElementById('total-duration');

        playBtn.onclick = () => this.toggle();

        // Audio events
        this.audio.onplay = () => {
            this.isPlaying = true;
            this._lastPlayTimestamp = Date.now();
            playBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>';
            playerBar.classList.remove('is-inactive');
            // Start smooth update loop
            this.updateProgressLoop();
        };

        this.audio.onpause = () => {
            this.isPlaying = false;
            // Track listening time
            if (this._lastPlayTimestamp > 0) {
                const elapsed = (Date.now() - this._lastPlayTimestamp) / 1000;
                HistorySystem.addListeningTime(elapsed);
                this._lastPlayTimestamp = 0;
            }
            playBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>';
            if (this.animationFrameId) cancelAnimationFrame(this.animationFrameId);
        };

        this.audio.onended = () => {
            // Track listening time
            if (this._lastPlayTimestamp > 0) {
                const elapsed = (Date.now() - this._lastPlayTimestamp) / 1000;
                HistorySystem.addListeningTime(elapsed);
                this._lastPlayTimestamp = 0;
            }
            if (this.animationFrameId) cancelAnimationFrame(this.animationFrameId);
            this.handleCompletion();
            this.next();
        };

        this.audio.onloadedmetadata = () => {
            durationEl.textContent = this.formatTime(this.audio.duration);
        };

        // Slider Interaction
        progressSlider.onmousedown = () => { this.isDraggingSlider = true; };
        progressSlider.ontouchstart = () => { this.isDraggingSlider = true; };

        progressSlider.oninput = () => {
            if (!this.audio.duration) return;
            // Instantly update the time text while dragging
            const time = (progressSlider.value / 100) * this.audio.duration;
            currentTimeEl.textContent = this.formatTime(time);
        };

        const onSliderRelease = () => {
            if (!this.isDraggingSlider) return;
            this.isDraggingSlider = false;
            if (this.audio.duration) {
                const time = (progressSlider.value / 100) * this.audio.duration;
                this.audio.currentTime = time;
            }
        };

        progressSlider.onmouseup = onSliderRelease;
        progressSlider.ontouchend = onSliderRelease;

        volumeSlider.oninput = () => {
            this.audio.volume = volumeSlider.value / 100;
        };

        // Load last track from persistence
        const lastTrack = localStorage.getItem('ivids_last_track');
        if (lastTrack) {
            try {
                const track = JSON.parse(lastTrack);
                this.setUI(track);
                // We keep it hidden (is-inactive) until user plays something
            } catch (e) {
                console.error('Error loading last track', e);
            }
        }

        if (window.Loader) window.Loader.init();
        this.isInitialized = true;
    },

    setUI(track) {
        const titleEl = document.getElementById('player-title');
        const artistEl = document.getElementById('player-artist');
        const coverEl = document.getElementById('player-cover');
        const playerBar = document.getElementById('player-bar');
        const moreInfoBtn = document.getElementById('more-info-btn');
        const likeBtn = document.getElementById('like-track-btn');
        const dislikeBtn = document.getElementById('dislike-track-btn');

        if (titleEl) titleEl.textContent = track.title;
        if (artistEl) artistEl.textContent = track.artist;
        if (coverEl) coverEl.src = track.cover;
        if (playerBar) {
            playerBar.style.setProperty('--current-cover', `url(${track.cover})`);
        }
        // Enable buttons
        if (moreInfoBtn) {
            moreInfoBtn.disabled = false;
            moreInfoBtn.style.opacity = '1';
        }
        if (likeBtn) likeBtn.disabled = false;
        if (dislikeBtn) dislikeBtn.disabled = false;

        // Visual check for liked state
        const checkLikeState = () => {
            const scores = DiscoveryEngine.getScores();
            const score = scores.tracks[track.id] || 0;
            if (likeBtn) likeBtn.style.color = score >= 5 ? 'var(--primary-color)' : '';
        };
        checkLikeState();
    },

    updateProgressLoop() {
        if (this.isPlaying && this.audio.duration && !this.isDraggingSlider) {
            const progressSlider = document.getElementById('progress-slider');
            const currentTimeEl = document.getElementById('current-time');

            if (progressSlider && currentTimeEl) {
                const percent = (this.audio.currentTime / this.audio.duration) * 100;
                progressSlider.value = percent;
                currentTimeEl.textContent = this.formatTime(this.audio.currentTime);

                // Completion trigger (after 90%)
                if (this.currentTrack && !this._completionScored && percent > 90) {
                    this.handleCompletion();
                }
            }
        }

        if (this.isPlaying) {
            this.animationFrameId = requestAnimationFrame(this.updateProgressLoop.bind(this));
        }
    },

    formatTime(seconds) {
        if (!seconds || isNaN(seconds)) return '0:00';
        const m = Math.floor(seconds / 60);
        const s = Math.floor(seconds % 60);
        return `${m}:${s < 10 ? '0' : ''}${s}`;
    },

    handleCompletion() {
        if (!this._completionScored && this.currentTrack) {
            this._completionScored = true;
            DiscoveryEngine.recordCompletion(this.currentTrack.id);
        }
    },

    next() {
        if (this.currentTrack && !this._listenScored && this.audio.currentTime < 20) {
            DiscoveryEngine.recordSkip(this.currentTrack.id);
        }
        console.log('Next track clicked');
    },
    previous() { console.log('Previous track clicked'); },

    toggleLike() {
        if (!this.currentTrack) return;
        DiscoveryEngine.recordLike(this.currentTrack);
        const likeBtn = document.getElementById('like-track-btn');
        if (likeBtn) likeBtn.style.color = 'var(--primary-color)';
    },

    toggleDislike() {
        if (!this.currentTrack) return;
        DiscoveryEngine.recordDislike(this.currentTrack);
        const likeBtn = document.getElementById('like-track-btn');
        if (likeBtn) likeBtn.style.color = ''; // Reset like visual
        this.next(); // Go to next track gracefully
    },


    async loadTrack(track) {
        this.init();
        this.currentTrack = track;

        // Refresh Song Detail page if currently active
        if (window.Router?.currentPage === 'song') {
            window.Router.loadPage('song', { id: track.id });
        }

        // Persist last track
        localStorage.setItem('ivids_last_track', JSON.stringify(track));

        // Save to history using automated system
        HistorySystem.add(track);

        // Score the listen immediately (+1 point)
        DiscoveryEngine.recordListen(track);

        // Reset scoring flags
        this._completionScored = false;

        this.setUI(track);

        const statusContainer = document.getElementById('player-status-container');
        const statusEl = document.getElementById('download-status');
        const loaderEl = document.getElementById('player-loader');
        const playerBar = document.getElementById('player-bar');
        const saveBtn = document.getElementById('save-track-btn'); // New save button

        if (playerBar) playerBar.classList.remove('is-inactive');
        if (statusContainer) statusContainer.style.display = 'flex';
        if (loaderEl) loaderEl.style.display = 'inline-flex';
        const moreInfoBtn = document.getElementById('more-info-btn');
        if (saveBtn) {
            saveBtn.disabled = true; // Disable until ready
            saveBtn.style.opacity = '0.5';
        }
        if (moreInfoBtn) {
            moreInfoBtn.disabled = false;
            moreInfoBtn.style.opacity = '1';
        }
        if (statusEl) {
            statusEl.textContent = 'Searching YouTube...';
            statusEl.style.color = 'var(--primary-color)';
        }

        try {
            const query = `${track.artist} - ${track.title}`;
            const videoId = await MusicAPI.getYouTubeVideoId(query);

            if (!videoId) throw new Error('Could not find song on YouTube');

            if (statusEl) statusEl.textContent = 'Downloading MP3...';

            // Pass artist and title to handle the new naming format
            const params = new URLSearchParams({
                videoId: videoId,
                artist: track.artist,
                title: track.title
            });

            const response = await MusicAPI._fetch(`${Config.SERVER_URL}/play?${params.toString()}`);
            const data = await response.json();

            if (data.status === 'ready') {
                if (statusContainer) statusContainer.style.display = 'none';

                // Track is ready, enable save functionality
                if (saveBtn) {
                    saveBtn.disabled = false;
                    saveBtn.style.opacity = '1';
                    // Store videoId for saving later
                    saveBtn.dataset.videoId = videoId;
                }

                this.audio.src = data.url;
                this.audio.play();
            } else {
                throw new Error('Server reported error');
            }

        } catch (error) {
            console.error('[Player Error]', error);
            if (statusEl) {
                statusEl.textContent = 'Error: ' + error.message;
                statusEl.style.color = 'red';
            }
            if (loaderEl) loaderEl.style.display = 'none';
        }
    },

    /**
     * Instantly play a track from local saved storage.
     * @param {Object} track {filename, artist, title, url}
     */
    playSavedTrack(track) {
        if (!this.audio) this.init();

        this.currentTrack = track;

        // Refresh Song Detail page if currently active
        if (window.Router?.currentPage === 'song') {
            window.Router.loadPage('song', { id: track.id });
        }

        // Persist last played (so it remembers across reloads)
        localStorage.setItem('ivids_last_track', JSON.stringify(track));

        // Save to history using automated system
        HistorySystem.add(track);

        this.setUI(track);

        const statusContainer = document.getElementById('player-status-container');
        const playerBar = document.getElementById('player-bar');
        const saveBtn = document.getElementById('save-track-btn'); // New save button

        if (playerBar) playerBar.classList.remove('is-inactive');
        if (statusContainer) statusContainer.style.display = 'none';

        if (saveBtn) {
            saveBtn.disabled = true;
            // It's already saved, so let's show the success icon (or hide the button)
            saveBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="currentColor" style="color:var(--primary-color); width:20px; height:20px;"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>';
            saveBtn.style.opacity = '1';
        }

        // Set local audio stream and drop into playback
        try {
            this.audio.src = track.url;
            this.audio.play();
        } catch (error) {
            console.error('[Player Error]', error);
        }
    },

    toggle() {
        if (!this.audio.src) return;
        if (this.isPlaying) this.audio.pause();
        else this.audio.play();
    },

    async saveTrack() {
        if (!this.currentTrack) return;

        const saveBtn = document.getElementById('save-track-btn');
        if (!saveBtn || !saveBtn.dataset.videoId || saveBtn.disabled) return;

        const videoId = saveBtn.dataset.videoId;

        try {
            // Visual feedback: saving
            saveBtn.disabled = true;
            saveBtn.style.opacity = '0.5';

            const params = new URLSearchParams({
                videoId: videoId,
                artist: this.currentTrack.artist,
                title: this.currentTrack.title
            });

            const response = await MusicAPI._fetch(`${Config.SERVER_URL}/save?${params.toString()}`);
            const data = await response.json();

            if (data.status === 'saved') {
                console.log('[Save]', data.message);
                // Visual feedback: success
                saveBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="currentColor" style="color:var(--primary-color)"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>';
            } else {
                throw new Error(data.message || 'Failed to save');
            }
        } catch (err) {
            console.error('[Save Error]', err);
            saveBtn.disabled = false;
            saveBtn.style.opacity = '1';
            alert('Could not save track: ' + err.message);
        }
    },

    async playAllSaved() {
        const { MusicAPI } = await import('./api.js');
        const tracks = await MusicAPI.getSavedTracks();
        if (tracks && tracks.length > 0) {
            this.queue = [...tracks];
            this.currentIndex = 0;
            this.playSavedTrack(this.queue[this.currentIndex]);
        }
    },

    async shuffleSaved() {
        const { MusicAPI } = await import('./api.js');
        const tracks = await MusicAPI.getSavedTracks();
        if (tracks && tracks.length > 0) {
            const shuffled = [...tracks].sort(() => Math.random() - 0.5);
            this.queue = shuffled;
            this.currentIndex = 0;
            this.playSavedTrack(this.queue[this.currentIndex]);
        }
    },

    handleError(msg) {
        console.error("Player Error:", msg);
        if (typeof showToast === 'function') {
            showToast(msg);
        }
    }
};

window.YouTubePlayer = YouTubePlayer;
if (document.readyState === 'loading') {
    window.addEventListener('DOMContentLoaded', () => YouTubePlayer.init());
} else {
    YouTubePlayer.init();
}
