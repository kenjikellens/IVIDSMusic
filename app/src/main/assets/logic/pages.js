import { MusicAPI } from './api.js';
import { YouTubePlayer } from './player.js';
import { CardSystem } from './cards.js';
import { HistorySystem } from './history.js';
import { SettingsManager } from './settings-manager.js';

let isHeroDismissed = false;

export const PageSystem = {
    async initHome() {
        if (window.Loader) window.Loader.init();

        try {
            const signal = window.Router.abortController?.signal;
            const rows = await MusicAPI.getRecommendations(signal);

            if (signal?.aborted) return;

            // Target the hardcoded containers and populate them
            rows.forEach(category => {
                const rowContent = document.getElementById(`content-${category.id}`);
                if (rowContent) {
                    const existingCards = Array.from(rowContent.children);

                    // Replace skeleton content with real data on the same element
                    category.tracks.forEach((track, index) => {
                        if (index < existingCards.length) {
                            CardSystem.hydrateCard(existingCards[index], track);
                        } else {
                            rowContent.appendChild(CardSystem.createCard(track));
                        }
                    });

                    // Remove any leftover skeletons if API returned fewer tracks
                    if (existingCards.length > category.tracks.length) {
                        for (let i = category.tracks.length; i < existingCards.length; i++) {
                            existingCards[i].remove();
                        }
                    }
                }
            });

            if (window.Loader) window.Loader.init();

            const heroBtn = document.getElementById('play-hero-btn');
            if (heroBtn && rows[0]?.tracks[0]) {
                heroBtn.onclick = () => YouTubePlayer.loadTrack(rows[0].tracks[0]);
            }

            // Hero Dismissal Logic
            const hero = document.querySelector('.hero');
            const closeBtn = document.getElementById('close-hero-btn');

            // Cleanup legacy persistence if it exists
            if (localStorage.getItem('hero_dismissed')) {
                localStorage.removeItem('hero_dismissed');
            }

            if (hero) {
                if (isHeroDismissed) {
                    hero.classList.add('is-hidden');
                } else if (closeBtn) {
                    closeBtn.onclick = (e) => {
                        e.stopPropagation(); // Avoid triggering parent clicks
                        hero.classList.add('is-hidden');
                        isHeroDismissed = true;
                    };
                }
            }
        } catch (e) {
            console.error('[Home] Error:', e);
        }
    },

    async initSettings() {
        try {
            // Update the display text to match current persistence state
            SettingsManager.updateScaleDisplay(SettingsManager.getScale());
            if (window.LanguageManager) {
                window.LanguageManager.updateLanguageDisplay();
            }
        } catch (e) {
            console.error('[Settings] Error:', e);
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

        const signal = window.Router.abortController?.signal;
        if (window.Loader) window.Loader.init();

        try {
            // 1. Fetch main artist data
            const artist = await MusicAPI.getArtistByName(params.name, signal);
            if (signal?.aborted) return;

            if (artist) {
                const nameEl = document.getElementById('artist-name');
                const avatarEl = document.getElementById('artist-avatar');
                const avatarLoader = document.getElementById('artist-avatar-loader');
                const fansEl = document.getElementById('artist-fans');
                const albumsCountEl = document.getElementById('artist-albums-count');

                if (nameEl) nameEl.textContent = artist.name;
                if (fansEl) fansEl.textContent = `${(artist.nb_fan || 0).toLocaleString()} fans`;

                if (avatarEl && artist.picture_xl) {
                    avatarEl.onload = () => {
                        avatarEl.style.opacity = '1';
                        if (avatarLoader) avatarLoader.style.display = 'none';
                    };
                    avatarEl.src = artist.picture_xl;
                }

                // Get average color for hero blobs
                MusicAPI.getAverageColor(artist.picture_xl).then(color => {
                    const hero = document.querySelector('.artist-hero');
                    if (hero) hero.style.setProperty('--primary-color', color);
                });

                // 2. Fetch Top Tracks & Albums in parallel
                const [topTracks, albums] = await Promise.all([
                    MusicAPI.getArtistTopTracks(artist.id, 15, signal).catch(() => []),
                    MusicAPI.getArtistAlbums(artist.id, 50, artist.name, signal).catch(() => [])
                ]);

                // Update album count with actual studio albums
                if (albumsCountEl) albumsCountEl.textContent = `${albums.length} albums`;

                if (signal?.aborted) return;

                // Wire up Play button
                const playBtn = document.getElementById('play-artist-btn');
                if (playBtn && topTracks.length > 0) {
                    playBtn.disabled = false;
                    playBtn.onclick = () => {
                        YouTubePlayer.loadTrack(topTracks[0]);
                    };
                }

                // Render Top Tracks
                const topTracksCont = document.getElementById('artist-top-tracks-container');
                const topTracksRow = document.getElementById('artist-top-tracks');
                if (topTracksCont && topTracksRow && topTracks.length > 0) {
                    topTracksRow.innerHTML = '';
                    topTracks.forEach(track => {
                        topTracksRow.appendChild(CardSystem.createCard(track));
                    });
                    topTracksCont.classList.remove('is-hidden');
                }

                // Render Albums
                const albumsCont = document.getElementById('artist-albums-container');
                const albumsRow = document.getElementById('artist-albums');
                if (albumsCont && albumsRow && albums.length > 0) {
                    albumsRow.innerHTML = '';
                    albums.forEach(album => {
                        albumsRow.appendChild(CardSystem.createCard(album));
                    });
                    albumsCont.classList.remove('is-hidden');
                }
            }
        } catch (e) {
            console.error('[initArtist] Failed to load artist:', e);
        }
    },

    async initSettings() {
        const { SettingsManager } = await import('./settings-manager.js');
        const { LanguageManager } = await import('./language-manager.js');

        // 1. Scale UI Binding
        const scaleContainer = document.getElementById('scale-options-container');
        if (scaleContainer) {
            const currentScale = SettingsManager.getScale();
            const buttons = scaleContainer.querySelectorAll('.scale-btn');

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
        }

        // 2. Language UI Binding
        LanguageManager.bindLanguageUI();
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
            itemDiv.tabIndex = 0; // Added for TV Nav
            itemDiv.innerHTML = `
                <div class="track-info">
                    <div class="track-title">${track.title}</div>
                    <div class="track-artist">${track.artist}</div>
                </div>
                <button class="play-local-btn player-control-btn main small" tabindex="-1">
                    <img src="svg/play.svg" alt="Play">
                </button>
            `;

            const playTrack = () => YouTubePlayer.playSavedTrack(track);
            itemDiv.onclick = playTrack;
            itemDiv.onkeydown = (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    playTrack();
                }
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
            card.tabIndex = 0; // Added for TV Nav
            card.innerHTML = `
                <div class="card-image-box">
                    <img src="${track.cover}" class="poster" alt="${track.title}">
                </div>
                <div class="card-info-box">
                    <div class="card-title">${track.title}</div>
                    <div class="card-artist">${track.artist}</div>
                </div>
            `;

            const playRecent = () => {
                if (track.url) {
                    YouTubePlayer.playSavedTrack(track);
                } else {
                    YouTubePlayer.loadTrack(track);
                }
            };

            card.onclick = playRecent;
            card.onkeydown = (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    playRecent();
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
    },
    async initAlbum(params) {
        if (!params || !params.id) return;

        const signal = window.Router.abortController?.signal;
        if (window.Loader) window.Loader.init();

        try {
            const album = await MusicAPI.getAlbumDetails(params.id, signal);
            if (signal?.aborted) return;

            if (album) {
                const titleEl = document.getElementById('album-title');
                const artistEl = document.getElementById('album-artist');
                const coverEl = document.getElementById('album-cover');
                const coverLoader = document.getElementById('album-cover-loader');
                const dateEl = document.getElementById('album-release-date');
                const countEl = document.getElementById('album-tracks-count');

                if (titleEl) titleEl.textContent = album.title;
                if (artistEl) {
                    artistEl.textContent = album.artist;
                    artistEl.onclick = () => window.Router.loadPage('artist', { name: album.artist });
                }
                if (dateEl) dateEl.textContent = new Date(album.releaseDate).getFullYear();
                if (countEl) countEl.textContent = `${album.nb_tracks} tracks`;

                if (coverEl && album.cover) {
                    coverEl.onload = () => {
                        coverEl.style.opacity = '1';
                        if (coverLoader) coverLoader.style.display = 'none';
                    };
                    coverEl.src = album.cover;
                }

                // Dynamic background color
                MusicAPI.getAverageColor(album.cover).then(color => {
                    const hero = document.querySelector('.album-hero');
                    if (hero) hero.style.setProperty('--primary-color', color);
                });

                // Play Button
                const playBtn = document.getElementById('play-album-btn');
                if (playBtn && album.tracks.length > 0) {
                    playBtn.disabled = false;
                    playBtn.onclick = () => YouTubePlayer.loadTrack(album.tracks[0]);
                }

                // Render Tracks
                const tracklistCont = document.getElementById('album-tracklist');
                if (tracklistCont) {
                    tracklistCont.innerHTML = '';
                    album.tracks.forEach((track, index) => {
                        const trackRow = document.createElement('div');
                        trackRow.className = 'album-track-row';
                        trackRow.tabIndex = 0;
                        trackRow.innerHTML = `
                            <div class="track-number">${index + 1}</div>
                            <div class="track-details">
                                <div class="track-title">${track.title}</div>
                                <div class="track-artist">${track.artist}</div>
                            </div>
                            <div class="track-duration">--:--</div>
                        `;
                        trackRow.onclick = () => YouTubePlayer.loadTrack(track);
                        trackRow.onkeydown = (e) => {
                            if (e.key === 'Enter') trackRow.click();
                        };
                        tracklistCont.appendChild(trackRow);
                    });
                }
            }
        } catch (e) {
            console.error('[initAlbum] Error:', e);
        }
    }
};

// Global UI helpers
window.performSearch = (q) => window.Router.loadPage('search', { query: q });
window.setSearchFilter = (t) => {
    const p = window.Router.currentParams || {};
    window.Router.loadPage('search', { ...p, type: t });
};

window.applyYearFilter = () => {
    const year = document.getElementById('year-input')?.value;
    const p = window.Router.currentParams || {};
    window.Router.loadPage('search', { ...p, year });
};
