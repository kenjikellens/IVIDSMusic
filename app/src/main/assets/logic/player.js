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

    /**
     * Initializes player listeners, range controls, volume cache, and binds mobile portrait tap actions to load the song page.
     * Restores persistent state and establishes core HTML5 Audio callback hooks.
     */
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
            progressSlider.style.setProperty('--slider-val', progressSlider.value + '%');
        };

        const onSliderRelease = () => {
            if (!this.isDraggingSlider) return;
            this.isDraggingSlider = false;
            if (this.audio.duration) {
                const time = (progressSlider.value / 100) * this.audio.duration;
                this.audio.currentTime = time;
                progressSlider.style.setProperty('--slider-val', progressSlider.value + '%');
            }
        };

        progressSlider.onmouseup = onSliderRelease;
        progressSlider.ontouchend = onSliderRelease;

        // Initialize progress slider custom CSS property
        progressSlider.style.setProperty('--slider-val', progressSlider.value + '%');

        // Save volume levels to localStorage on adjustment
        volumeSlider.oninput = () => {
            this.audio.volume = volumeSlider.value / 100;
            localStorage.setItem('ivids_volume', volumeSlider.value);
            volumeSlider.style.setProperty('--slider-val', volumeSlider.value + '%');
        };

        // Load and restore user's cached volume setting on startup, defaulting to 100% (1.0)
        const savedVolume = localStorage.getItem('ivids_volume');
        if (savedVolume !== null) {
            volumeSlider.value = savedVolume;
            this.audio.volume = parseFloat(savedVolume) / 100;
        } else {
            this.audio.volume = 1.0;
        }
        volumeSlider.style.setProperty('--slider-val', volumeSlider.value + '%');

        // Queue Drawer bindings
        const queueToggleBtn = document.getElementById('queue-toggle-btn');
        const closeQueueBtn = document.getElementById('close-queue-btn');
        const queueDrawer = document.getElementById('queue-drawer');

        if (queueToggleBtn && queueDrawer) {
            queueToggleBtn.onclick = () => {
                queueDrawer.classList.toggle('is-active');
                if (queueDrawer.classList.contains('is-active')) {
                    this.renderQueue();
                }
            };
        }

        if (closeQueueBtn && queueDrawer) {
            closeQueueBtn.onclick = () => {
                queueDrawer.classList.remove('is-active');
            };
        }

        // Load last track from persistence
        const lastTrack = localStorage.getItem('ivids_last_track');
        if (lastTrack) {
            try {
                const track = JSON.parse(lastTrack);
                this.currentTrack = track;
                this.setUI(track);
                // We keep it hidden (is-inactive) until user plays something
            } catch (e) {
                console.error('Error loading last track', e);
            }
        }

        // Binds full-screen transition on player bar click in mobile portrait mode
        playerBar.addEventListener('click', (e) => {
            const isInteractive = e.target.closest('button, input, a, .player-control-btn, .ivids-slider');
            if (!isInteractive) {
                const isPortrait = window.matchMedia("(orientation: portrait)").matches;
                if (isPortrait) {
                    window.Router.loadPage('song');
                }
            }
        });

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

    /**
     * Periodically updates the position, time label, and custom background fill of the playback progress slider.
     * Automatically monitors the track percentage to trigger recommendation completion telemetry.
     */
    updateProgressLoop() {
        if (this.isPlaying && this.audio.duration && !this.isDraggingSlider) {
            const progressSlider = document.getElementById('progress-slider');
            const currentTimeEl = document.getElementById('current-time');

            if (progressSlider && currentTimeEl) {
                const percent = (this.audio.currentTime / this.audio.duration) * 100;
                progressSlider.value = percent;
                currentTimeEl.textContent = this.formatTime(this.audio.currentTime);
                progressSlider.style.setProperty('--slider-val', percent + '%');

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

    /**
     * Navigates to and plays the next track in the current active queue if available.
     * Triggers skip telemetry in DiscoveryEngine if the track is skipped before 20 seconds.
     * Automatically handles both local saved tracks and online streaming.
     */
    next() {
        if (this.currentTrack && !this._listenScored && this.audio.currentTime < 20) {
            DiscoveryEngine.recordSkip(this.currentTrack.id);
        }
        
        if (this.queue && this.queue.length > 0 && this.currentIndex !== -1) {
            // Traverse queue circularly
            this.currentIndex = (this.currentIndex + 1) % this.queue.length;
            const nextTrack = this.queue[this.currentIndex];
            if (nextTrack) {
                console.log(`[Queue] Navigating forward to: ${nextTrack.title} - ${nextTrack.artist} (index ${this.currentIndex})`);
                if (nextTrack.url) {
                    this.playSavedTrack(nextTrack);
                } else {
                    this.loadTrack(nextTrack);
                }
            }
        } else {
            console.log('[Queue] Next clicked but no valid active queue found.');
        }
    },

    /**
     * Navigates to and plays the previous track in the current active queue if available.
     * Restarts playback of the current track if it has played for more than 3 seconds.
     * Automatically handles both local saved tracks and online streaming.
     */
    previous() {
        if (this.audio && this.audio.currentTime > 3) {
            console.log('[Queue] Previous clicked, restarting current track');
            this.audio.currentTime = 0;
            return;
        }

        if (this.queue && this.queue.length > 0 && this.currentIndex !== -1) {
            // Traverse queue circularly backward
            this.currentIndex = (this.currentIndex - 1 + this.queue.length) % this.queue.length;
            const prevTrack = this.queue[this.currentIndex];
            if (prevTrack) {
                console.log(`[Queue] Navigating backward to: ${prevTrack.title} - ${prevTrack.artist} (index ${this.currentIndex})`);
                if (prevTrack.url) {
                    this.playSavedTrack(prevTrack);
                } else {
                    this.loadTrack(prevTrack);
                }
            }
        } else {
            console.log('[Queue] Previous clicked but no valid active queue found.');
        }
    },

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


    /**
     * Method: loadTrack
     * Description: Initiates streaming of a new music track. Searches YouTube for the track's video ID,
     *              requests the audio download and stream URL from the local server, and starts audio playback.
     * @param {Object} track - The metadata of the track to load and play.
     */
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
        this.updateAmbientColors(track.cover);
        this.renderQueue();

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
            const searchingText = (window.LanguageManager && window.LanguageManager.translations['searching_youtube']) || 'Searching YouTube...';
            statusEl.textContent = searchingText;
            statusEl.style.color = 'var(--primary-color)';
        }

        try {
            const query = `${track.artist} - ${track.title}`;
            const videoId = await MusicAPI.getYouTubeVideoId(query);

            if (!videoId) throw new Error('Could not find song on YouTube');

            if (statusEl) {
                const downloadingText = (window.LanguageManager && window.LanguageManager.translations['downloading_mp3']) || 'Downloading MP3...';
                statusEl.textContent = downloadingText;
            }

            const data = await MusicAPI.playTrack(videoId, track.artist, track.title);


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
                const errorText = (window.LanguageManager && window.LanguageManager.translations['error']) || 'Error';
                statusEl.textContent = errorText + ': ' + error.message;
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
        this.updateAmbientColors(track.cover);
        this.renderQueue();

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

            const data = await MusicAPI.saveTrack(videoId, this.currentTrack.artist, this.currentTrack.title, this.audio.src);


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

    /**
     * Extracts the average color from the cover art and updates the ambient backdrop CSS variables.
     * Computes a complementary hue for the second blob to create a beautiful gradient.
     * @param {string} coverUrl - The URL of the track's cover artwork.
     */
    async updateAmbientColors(coverUrl) {
        if (!coverUrl) return;
        try {
            const baseColor = await MusicAPI.getAverageColor(coverUrl);
            if (baseColor && baseColor.startsWith('rgb')) {
                const rgb = baseColor.match(/\d+/g);
                if (rgb && rgb.length >= 3) {
                    const r = parseInt(rgb[0]);
                    const g = parseInt(rgb[1]);
                    const b = parseInt(rgb[2]);

                    const compR = 255 - r;
                    const compG = 255 - g;
                    const compB = 255 - b;

                    document.documentElement.style.setProperty('--ambient-color-1', `rgb(${r}, ${g}, ${b})`);
                    document.documentElement.style.setProperty('--ambient-color-2', `rgb(${compR}, ${compG}, ${compB})`);
                    document.documentElement.style.setProperty('--primary-rgb', `${r}, ${g}, ${b}`);
                }
            } else {
                document.documentElement.style.setProperty('--ambient-color-1', 'var(--primary-color)');
                document.documentElement.style.setProperty('--ambient-color-2', 'var(--accent-color)');
            }
        } catch (e) {
            console.error('Error updating ambient colors:', e);
        }
    },

    /**
     * Renders the current tracks in the playback queue inside the Queue Drawer.
     * Separates the currently playing track from the upcoming tracks and sets the clear button state.
     */
    renderQueue() {
        const queueNowPlaying = document.getElementById('queue-now-playing');
        const queueUpcoming = document.getElementById('queue-upcoming');
        const clearQueueBtn = document.getElementById('clear-queue-btn');
        const queueDrawer = document.getElementById('queue-drawer');

        if (!queueNowPlaying || !queueUpcoming) return;

        queueNowPlaying.innerHTML = '';
        queueUpcoming.innerHTML = '';

        if (clearQueueBtn) {
            clearQueueBtn.disabled = this.queue.length === 0;
        }

        if (this.currentTrack) {
            const activeCard = document.createElement('div');
            activeCard.className = 'queue-track-card active';
            activeCard.innerHTML = `
                <img src="${this.currentTrack.cover}" alt="Cover" class="queue-track-cover">
                <div class="queue-track-info">
                    <div class="queue-track-title">${this.currentTrack.title}</div>
                    <div class="queue-track-artist">${this.currentTrack.artist}</div>
                </div>
            `;
            queueNowPlaying.appendChild(activeCard);
        } else {
            const emptyText = (window.LanguageManager && window.LanguageManager.translations['queue_empty']) || 'Queue is empty';
            queueNowPlaying.innerHTML = `<div data-i18n="queue_empty" style="font-size: calc(0.85rem * var(--ui-scale)); color: var(--text-muted); text-align: center; padding: calc(10px * var(--ui-scale)) 0;">${emptyText}</div>`;
        }

        const upcomingTracks = this.queue.slice(this.currentIndex + 1);

        if (upcomingTracks.length > 0) {
            upcomingTracks.forEach((track, offset) => {
                const actualIndex = this.currentIndex + 1 + offset;
                const card = document.createElement('div');
                card.className = 'queue-track-card';
                card.onclick = () => this.playTrackFromQueue(actualIndex);
                card.innerHTML = `
                    <img src="${track.cover}" alt="Cover" class="queue-track-cover">
                    <div class="queue-track-info">
                        <div class="queue-track-title">${track.title}</div>
                        <div class="queue-track-artist">${track.artist}</div>
                    </div>
                    <button class="queue-track-remove" title="Remove from Queue">&times;</button>
                `;
                const removeBtn = card.querySelector('.queue-track-remove');
                if (removeBtn) {
                    removeBtn.onclick = (e) => this.removeFromQueue(actualIndex, e);
                }
                queueUpcoming.appendChild(card);
            });
        } else {
            const emptyText = (window.LanguageManager && window.LanguageManager.translations['queue_empty']) || 'Queue is empty';
            queueUpcoming.innerHTML = `<div data-i18n="queue_empty" style="font-size: calc(0.85rem * var(--ui-scale)); color: var(--text-muted); text-align: center; padding: calc(10px * var(--ui-scale)) 0;">${emptyText}</div>`;
        }

        if (window.LanguageManager && queueDrawer) {
            window.LanguageManager.translateUI(queueDrawer);
        }
    },

    /**
     * Plays a track from the playback queue by its index.
     * Resolves and streams the track using its local URL or streaming fallback.
     * @param {number} index - The index of the track in the queue.
     */
    playTrackFromQueue(index) {
        if (index < 0 || index >= this.queue.length) return;
        this.currentIndex = index;
        const track = this.queue[index];
        if (track) {
            if (track.url) {
                this.playSavedTrack(track);
            } else {
                this.loadTrack(track);
            }
        }
    },

    /**
     * Removes a track from the queue at a specific index.
     * Prevents event propagation to avoid triggering click events on parent elements.
     * @param {number} index - The index of the track to remove from the queue.
     * @param {Event} event - The browser click event object.
     */
    removeFromQueue(index, event) {
        if (event) {
            event.stopPropagation();
        }
        if (index < 0 || index >= this.queue.length) return;
        
        this.queue.splice(index, 1);
        
        if (this.currentIndex === index) {
            if (this.queue.length === 0) {
                this.clearQueue();
            } else {
                this.currentIndex = this.currentIndex % this.queue.length;
                this.playTrackFromQueue(this.currentIndex);
            }
        } else {
            if (this.currentIndex > index) {
                this.currentIndex--;
            }
            this.renderQueue();
        }
    },

    /**
     * Clears the active playback queue and resets the player bar and state.
     * Stops the audio playback immediately and updates the Queue Drawer UI.
     */
    clearQueue() {
        this.queue = [];
        this.currentIndex = -1;
        this.currentTrack = null;
        this.audio.pause();
        this.audio.src = '';
        this.isPlaying = false;
        
        const playBtn = document.getElementById('play-pause-btn');
        if (playBtn) {
            playBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>';
        }
        
        const playerBar = document.getElementById('player-bar');
        if (playerBar) {
            playerBar.classList.add('is-inactive');
        }
        
        this.renderQueue();
    },

    /**
     * Adds a track to the end of the playback queue.
     * Automatically sets the track as current if no track is currently playing.
     * @param {Object} track - The metadata of the track to add to the queue.
     */
    addToQueue(track) {
        this.init();
        this.queue.push(track);
        if (this.currentIndex === -1 || !this.currentTrack) {
            this.currentIndex = this.queue.length - 1;
            if (track.url) {
                this.playSavedTrack(track);
            } else {
                this.loadTrack(track);
            }
        } else {
            this.renderQueue();
            if (typeof showToast === 'function') {
                showToast(`Added to Queue: ${track.title}`);
            }
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
