import { MusicAPI } from './api.js';
import { Config } from './config.js';

export const YouTubePlayer = {
    audio: new Audio(),
    isPlaying: false,
    currentTrack: null,
    isInitialized: false,
    queue: [],
    currentIndex: -1,

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
            playBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>';
            playerBar.classList.remove('is-inactive');
        };
        this.audio.onpause = () => {
            this.isPlaying = false;
            playBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>';
        };

        this.audio.ontimeupdate = () => {
            if (!this.audio.duration) return;
            const percent = (this.audio.currentTime / this.audio.duration) * 100;
            progressSlider.value = percent;
            currentTimeEl.textContent = this.formatTime(this.audio.currentTime);
        };

        this.audio.onloadedmetadata = () => {
            durationEl.textContent = this.formatTime(this.audio.duration);
        };

        progressSlider.oninput = () => {
            const time = (progressSlider.value / 100) * this.audio.duration;
            this.audio.currentTime = time;
        };

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

        if (titleEl) titleEl.textContent = track.title;
        if (artistEl) artistEl.textContent = track.artist;
        if (coverEl) coverEl.src = track.cover;
        if (playerBar) {
            playerBar.style.setProperty('--current-cover', `url(${track.cover})`);
        }
    },

    formatTime(seconds) {
        if (!seconds || isNaN(seconds)) return '0:00';
        const m = Math.floor(seconds / 60);
        const s = Math.floor(seconds % 60);
        return `${m}:${s < 10 ? '0' : ''}${s}`;
    },

    next() { console.log('Next track clicked'); },
    previous() { console.log('Previous track clicked'); },

    async loadTrack(track) {
        this.init();
        this.currentTrack = track;

        // Persist last track
        localStorage.setItem('ivids_last_track', JSON.stringify(track));

        // Save to recently played history (max 20, no duplicates)
        const history = JSON.parse(localStorage.getItem('iv_recent_tracks') || '[]');
        const filtered = history.filter(t => !(t.title === track.title && t.artist === track.artist));
        filtered.unshift(track);
        localStorage.setItem('iv_recent_tracks', JSON.stringify(filtered.slice(0, 20)));

        this.setUI(track);

        const statusContainer = document.getElementById('player-status-container');
        const statusEl = document.getElementById('download-status');
        const loaderEl = document.getElementById('player-loader');
        const playerBar = document.getElementById('player-bar');

        if (playerBar) playerBar.classList.remove('is-inactive');
        if (statusContainer) statusContainer.style.display = 'flex';
        if (loaderEl) loaderEl.style.display = 'inline-flex';
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

            const response = await fetch(`${Config.SERVER_URL}/play?${params.toString()}`);
            const data = await response.json();

            if (data.status === 'ready') {
                if (statusContainer) statusContainer.style.display = 'none';
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

    toggle() {
        if (!this.audio.src) return;
        if (this.isPlaying) this.audio.pause();
        else this.audio.play();
    }
};

window.YouTubePlayer = YouTubePlayer;
if (document.readyState === 'loading') {
    window.addEventListener('DOMContentLoaded', () => YouTubePlayer.init());
} else {
    YouTubePlayer.init();
}
