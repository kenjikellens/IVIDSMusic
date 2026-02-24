import { MusicAPI } from './api.js';
import { YouTubePlayer } from './player.js';
import { CardSystem } from './cards.js';
import { HistorySystem } from './history.js';

export const PageSystem = {
    async initHome() {
        const container = document.getElementById('home-rows-container');
        if (!container) return;

        const GENRES = ['Pop', 'Rock', 'Hip-Hop', 'Hardcore', '90\'s', 'Electronic'];
        const SKELETON_CARD = `
            <div class="skeleton-card">
                <div class="skeleton-img">
                    <div class="ivids-loader poster-loader"></div>
                </div>
                <div class="skeleton-info-box">
                    <div class="skeleton-text title"></div>
                    <div class="skeleton-text artist"></div>
                </div>
            </div>
        `;

        // 1. Render History immediately (local data)
        const history = HistorySystem.get();
        const historyHTML = history.length > 0 ? `
            <div id="home-history-section">
                <!-- Rendered below via JS for consistency -->
            </div>
        ` : '';

        // 2. Pre-render skeletons with titles
        container.innerHTML = historyHTML + GENRES.map(genre => `
            <div class="row-container">
                <div class="row-header"><h2 class="row-title">${genre}</h2></div>
                <div class="skeleton-row">${SKELETON_CARD.repeat(12)}</div>
            </div>
        `).join('');

        const histSec = document.getElementById('home-history-section');
        if (histSec && history.length > 0) {
            histSec.appendChild(CardSystem.createRow('Recently Listened', 'home-recent-list', history.slice(0, 12)));
        }

        if (window.Loader) window.Loader.init();

        try {
            const signal = window.Router.abortController?.signal;
            const rows = await MusicAPI.getRecommendations(signal);

            if (signal?.aborted) return;

            // Preserve History section, replace skeletons only
            const historySection = document.getElementById('home-history-section');
            container.innerHTML = '';
            if (historySection) container.appendChild(historySection);

            rows.forEach(category => container.appendChild(CardSystem.createRow(category.title, category.id, category.tracks)));

            if (window.Loader) window.Loader.init();

            const heroBtn = document.getElementById('play-hero-btn');
            if (heroBtn && rows[0]?.tracks[0]) {
                heroBtn.onclick = () => YouTubePlayer.loadTrack(rows[0].tracks[0]);
            }
        } catch (e) {
            console.error('[Home] Error:', e);
            const err = document.createElement('p');
            err.className = 'error-msg';
            err.textContent = 'Failed to load content.';
            container.appendChild(err);
        }
    },

    async initSearch(params) {
        const get = (id) => document.getElementById(id);
        const query = (params.query || '').trim();
        const resultsHeader = get('results-header'), browseHero = get('browse-hero'), rowsCont = get('results-rows-container'), gridView = get('single-category-grid');

        let currentOffset = 0;
        const PAGE_SIZE = 24;

        if (get('header-search-input')) get('header-search-input').value = query;
        if (get('browse-search-input')) get('browse-search-input').value = query;
        if (get('year-input')) get('year-input').value = params.year || '';

        if (!query) {
            if (browseHero) browseHero.classList.remove('is-hidden');
            if (resultsHeader) resultsHeader.classList.add('is-hidden');
            if (rowsCont) rowsCont.classList.add('is-hidden');
            if (gridView) gridView.classList.add('is-hidden');
            return;
        }

        if (browseHero) browseHero.classList.add('is-hidden');
        if (resultsHeader) resultsHeader.classList.remove('is-hidden');
        if (get('search-subtitle')) get('search-subtitle').textContent = `Results for "${query}"${params.year ? ` - ${params.year}` : ''}`;

        // Add Load More Logic
        window.loadMoreResults = async () => {
            const btn = get('load-more-btn');
            const grid = get('grid-results');
            if (!btn || !grid) return;
            currentOffset += PAGE_SIZE;
            btn.disabled = true; btn.textContent = 'Loading...';
            try {
                const data = await MusicAPI.search(query, PAGE_SIZE, params.type, params.year, currentOffset);
                if (data.length === 0) get('load-more-container').classList.add('is-hidden');
                else {
                    data.forEach(item => grid.appendChild(CardSystem.createCard(item)));
                    if (window.Loader) window.Loader.init();
                    btn.disabled = false; btn.textContent = 'Load More';
                }
            } catch (e) { btn.disabled = false; btn.textContent = 'Load More'; }
        };

        const SKELETON_CARD = `
            <div class="skeleton-card">
                <div class="skeleton-img">
                    <div class="ivids-loader poster-loader"></div>
                </div>
                <div class="skeleton-info-box">
                    <div class="skeleton-text title"></div>
                    <div class="skeleton-text artist"></div>
                </div>
            </div>`;

        if (params.type) {
            if (rowsCont) rowsCont.classList.add('is-hidden');
            if (gridView) gridView.classList.remove('is-hidden');
            if (get('grid-title')) get('grid-title').textContent = params.type.charAt(0).toUpperCase() + params.type.slice(1) + "s";
            const grid = get('grid-results');
            grid.innerHTML = SKELETON_CARD.repeat(12);
            if (window.Loader) window.Loader.init();

            const data = await MusicAPI.search(query, PAGE_SIZE, params.type, params.year, 0, true);
            grid.innerHTML = '';

            data.forEach(item => grid.appendChild(CardSystem.createCard(item)));
            if (window.Loader) window.Loader.init();

            if (get('load-more-container')) {
                if (data.length >= PAGE_SIZE) get('load-more-container').classList.remove('is-hidden');
                else get('load-more-container').classList.add('is-hidden');
            }
        } else {
            if (rowsCont) rowsCont.classList.remove('is-hidden');
            if (gridView) gridView.classList.add('is-hidden');
            ['artists', 'songs', 'albums'].forEach(id => {
                if (get(`${id}-results`)) get(`${id}-results`).innerHTML = SKELETON_CARD.repeat(6);
            });
            if (window.Loader) window.Loader.init();

            const [artists, songs, albums] = await Promise.all([
                MusicAPI.search(query, 12, 'artist', params.year, 0, true).catch(() => []),
                MusicAPI.search(query, 12, 'all', params.year, 0, true).catch(() => []),
                MusicAPI.search(query, 12, 'album', params.year, 0, true).catch(() => [])
            ]);

            const fill = (id, data, contId) => {
                const el = get(id); if (!el) return;
                el.innerHTML = '';
                data.forEach(item => el.appendChild(CardSystem.createCard(item)));
                if (get(contId)) {
                    if (data.length) get(contId).classList.remove('is-hidden');
                    else get(contId).classList.add('is-hidden');
                }
            };
            fill('artists-results', artists, 'artists-row-container');
            fill('songs-results', songs, 'songs-row-container');
            fill('albums-results', albums, 'albums-row-container');

            if (window.Loader) window.Loader.init();
        }
    },

    async initArtist(params) {
        if (!params || !params.name) return;
        const nameEl = document.getElementById('artist-name');
        if (nameEl) nameEl.textContent = params.name;
    },

    async initSettings() {
        const { SettingsManager } = await import('./settings-manager.js');
        const container = document.getElementById('scale-options-container');
        if (!container) return;

        const currentScale = SettingsManager.getScale();
        const buttons = container.querySelectorAll('.scale-btn');

        buttons.forEach(btn => {
            const scale = parseFloat(btn.dataset.scale);
            if (scale === currentScale) {
                btn.classList.add('active');
            }

            btn.onclick = () => {
                buttons.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                SettingsManager.setScale(scale);
            };
        });
    },

    async initProfile() {
        const recentList = document.getElementById('profile-recent-list');
        if (!recentList) return;

        // Load real recently played from centralized history
        const history = HistorySystem.get();

        if (history.length === 0) {
            recentList.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">🎵</div>
                    <p>Nothing played yet. Start listening!</p>
                </div>`;
            return;
        }

        history.slice(0, 6).forEach(track => {
            recentList.appendChild(CardSystem.createCard(track));
        });

        if (window.Loader) window.Loader.init();
    },

    async initLibrary() {
        const container = document.getElementById('library-list-container');
        const emptyState = document.getElementById('library-empty-state');
        if (!container) return;

        // Reset search input
        const searchInput = document.getElementById('library-track-search');
        if (searchInput) searchInput.value = '';

        // Show history instantly (local data)
        this.renderRecentTracks();

        container.innerHTML = `
            <div class="skeleton-list">
                ${`<div class="skeleton-card track-skeleton" style="width: 100%; height: 60px; margin-bottom: 10px;"></div>`.repeat(5)}
            </div>
        `;

        try {
            const signal = window.Router.abortController?.signal;
            const tracks = await MusicAPI.getSavedTracks(signal);

            if (signal?.aborted) return;

            this.savedTracks = tracks || []; // Store globally for filtering

            this.renderLibrary(this.savedTracks);

        } catch (error) {
            console.error('[Library] Failed to load', error);
            container.innerHTML = '<p class="error-msg">Failed to load library.</p>';
        }
    },

    renderLibrary(tracks) {
        const container = document.getElementById('library-list-container');
        const emptyState = document.getElementById('library-empty-state');
        if (!container) return;

        container.innerHTML = '';

        if (!tracks || tracks.length === 0) {
            if (emptyState) emptyState.classList.remove('is-hidden');
            container.classList.add('is-hidden');
            return;
        }

        if (emptyState) emptyState.classList.add('is-hidden');
        container.classList.remove('is-hidden');

        tracks.forEach(track => {
            const itemDiv = document.createElement('div');
            itemDiv.className = 'library-track-item';
            itemDiv.innerHTML = `
                <div class="track-info">
                    <div class="track-title">${track.title}</div>
                    <div class="track-artist">${track.artist}</div>
                </div>
                <button class="play-local-btn player-control-btn main small">
                    <img src="svg/play.svg" alt="Play">
                </button>
            `;

            itemDiv.onclick = (e) => {
                YouTubePlayer.playSavedTrack(track);
            };

            container.appendChild(itemDiv);
        });
    },

    renderRecentTracks() {
        const container = document.getElementById('library-recent-list');
        const rowWrapper = document.getElementById('recent-row-container');
        if (!container || !rowWrapper) return;

        const history = HistorySystem.get();

        if (history.length === 0) {
            rowWrapper.classList.add('is-hidden');
            return;
        }

        rowWrapper.classList.remove('is-hidden');
        container.innerHTML = '';

        const validHistory = history.filter(t => t.title && t.artist && t.cover);

        validHistory.forEach(track => {
            const card = document.createElement('div');
            card.className = 'music-card';
            card.innerHTML = `
                <div class="card-image-box">
                    <img src="${track.cover}" class="poster" alt="${track.title}">
                </div>
                <div class="card-info-box">
                    <div class="card-title">${track.title}</div>
                    <div class="card-artist">${track.artist}</div>
                </div>
            `;

            card.onclick = () => {
                if (track.url) {
                    YouTubePlayer.playSavedTrack(track);
                } else {
                    YouTubePlayer.loadTrack(track);
                }
            };

            container.appendChild(card);
        });
    },

    clearHistory() {
        if (confirm('Are you sure you want to clear your playback history?')) {
            HistorySystem.clear();
            this.renderRecentTracks();
        }
    },

    filterLibrary(query) {
        if (!this.savedTracks) return;
        const q = query.toLowerCase().trim();
        const filtered = this.savedTracks.filter(t =>
            t.title.toLowerCase().includes(q) ||
            t.artist.toLowerCase().includes(q)
        );
        this.renderLibrary(filtered);
    }
};

// Global UI helpers
window.performSearch = (q) => window.Router.loadPage('search', { query: q });
window.handleBrowseInput = (v) => { if (document.getElementById('header-search-input')) document.getElementById('header-search-input').value = v; };
window.setSearchFilter = (t) => {
    const p = window.Router.currentParams || {};
    window.Router.loadPage('search', { ...p, type: t });
};

window.applyYearFilter = () => {
    const year = document.getElementById('year-input')?.value;
    const p = window.Router.currentParams || {};
    window.Router.loadPage('search', { ...p, year });
};
