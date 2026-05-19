import { MusicAPI } from './api.js';
import { YouTubePlayer } from './player.js';
import { CardSystem } from './cards.js';
import { HistorySystem } from './history.js';
import { SettingsManager } from './settings-manager.js';
import { DiscoveryEngine } from './recommendations.js';

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

    /**
     * Initializes the Settings page view.
     * Refreshes and updates the user-facing stepper display for interface scaling
     * and coordinates with the LanguageManager to display the active preferred language.
     */
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

    /**
     * Method: initSearch
     * Description: Initializes the search interface and triggers query execution based on user inputs.
     *              Handles the Browse Hero visual state transitions, populates query parameters,
     *              synchronizes localized horizontal category filter chips, and requests segmented
     *              or grid-based track, artist, and album results from the MusicAPI.
     * @param {Object} params - Contains search constraints: query string, type filters, and release year.
     */
    async initSearch(params) {
        const get = (id) => document.getElementById(id);
        const query = (params.query || '').trim();
        const resultsHeader = get('results-header'), browseHero = get('browse-hero'), rowsCont = get('results-rows-container'), gridView = get('single-category-grid');

        let currentOffset = 0;
        const PAGE_SIZE = 24;

        if (get('header-search-input')) get('header-search-input').value = query;
        if (get('browse-search-input')) get('browse-search-input').value = query;
        if (get('year-input')) get('year-input').value = params.year || '';

        // Wire up the unified search clear (✕) button
        const clearBtn = get('search-clear-btn');
        if (clearBtn) {
            clearBtn.style.display = query ? 'flex' : 'none';
            clearBtn.onclick = () => {
                const input = get('browse-search-input');
                if (input) {
                    input.value = '';
                    input.focus();
                }
                clearBtn.style.display = 'none';
                window.Router.loadPage('search');
            };
        }

        // Show/hide clear button dynamically as user types
        const searchInput = get('browse-search-input');
        if (searchInput) {
            searchInput.oninput = () => {
                if (clearBtn) {
                    clearBtn.style.display = searchInput.value.trim() ? 'flex' : 'none';
                }
            };
        }

        // Dynamically toggle localized premium filter chip active states
        const activeType = params.type || null;
        ['all', 'track', 'artist', 'album'].forEach(type => {
            const chip = get(`filter-chip-${type}`);
            if (chip) {
                if (type === 'all' && activeType === null) {
                    chip.classList.add('active');
                } else if (type === 'track' && activeType === 'all') {
                    chip.classList.add('active');
                } else if (type === activeType) {
                    chip.classList.add('active');
                } else {
                    chip.classList.remove('active');
                }
            }
        });

        if (!query) {
            if (browseHero) browseHero.classList.remove('is-hidden');
            if (resultsHeader) resultsHeader.classList.add('is-hidden');
            if (rowsCont) rowsCont.classList.add('is-hidden');
            if (gridView) gridView.classList.add('is-hidden');
            return;
        }

        if (browseHero) browseHero.classList.add('is-hidden');
        if (resultsHeader) resultsHeader.classList.remove('is-hidden');
        const resultsFor = (window.LanguageManager && window.LanguageManager.translations['results_for']) || 'Results for';
        if (get('search-subtitle')) get('search-subtitle').textContent = `${resultsFor} "${query}"${params.year ? ` - ${params.year}` : ''}`;

        // Add Load More Logic
        window.loadMoreResults = async () => {
            const btn = get('load-more-btn');
            const grid = get('grid-results');
            if (!btn || !grid) return;
            currentOffset += PAGE_SIZE;
            const loadMoreText = (window.LanguageManager && window.LanguageManager.translations['load_more']) || 'Load More';
            const loadingText = (window.LanguageManager && window.LanguageManager.translations['loading']) || 'Loading...';
            btn.disabled = true; btn.textContent = loadingText;
            try {
                const data = await MusicAPI.search(query, PAGE_SIZE, params.type, params.year, currentOffset);
                if (data.length === 0) get('load-more-container').classList.add('is-hidden');
                else {
                    data.forEach(item => grid.appendChild(CardSystem.createCard(item)));
                    if (window.Loader) window.Loader.init();
                    btn.disabled = false; btn.textContent = loadMoreText;
                }
            } catch (e) { btn.disabled = false; btn.textContent = loadMoreText; }
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

            let data = await MusicAPI.search(query, PAGE_SIZE, params.type, params.year, 0, false);

            // Smart Album Search Association (Grid View)
            // If we are looking specifically at Songs ('all') and the search query matches the top album exactly,
            // we load the album tracklist and prepended it to ensure its hits are discoverable.
            if (params.type === 'all') {
                try {
                    const albums = await MusicAPI.search(query, 1, 'album', params.year, 0, false).catch(() => []);
                    if (albums.length > 0 && albums[0].title.toLowerCase().trim() === query.toLowerCase().trim()) {
                        const albumDetails = await MusicAPI.getAlbumDetails(albums[0].id);
                        if (albumDetails && albumDetails.tracks) {
                            const existingIds = new Set(data.map(s => s.id));
                            const albumSongs = albumDetails.tracks.filter(s => !existingIds.has(s.id));
                            data = [...albumSongs, ...data].slice(0, PAGE_SIZE);
                        }
                    }
                } catch (e) {
                    console.error('[Search Grid] Smart Album Association failed:', e);
                }
            }

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

            const [artists, rawSongs, albums] = await Promise.all([
                MusicAPI.search(query, 12, 'artist', params.year, 0, false).catch(() => []),
                MusicAPI.search(query, 12, 'all', params.year, 0, false).catch(() => []),
                MusicAPI.search(query, 12, 'album', params.year, 0, false).catch(() => [])
            ]);

            let songs = [...rawSongs];

            // Smart Album Search Association (Categorized View)
            // If the query text matches the name of our top returned album exactly (e.g. searching "Currents"),
            // we retrieve that album's actual tracklist and inject its hits (which don't share search words)
            // right at the beginning of the Songs row to meet premium musical expectations.
            if (albums.length > 0) {
                const topAlbum = albums[0];
                if (topAlbum.title.toLowerCase().trim() === query.toLowerCase().trim()) {
                    try {
                        const albumDetails = await MusicAPI.getAlbumDetails(topAlbum.id);
                        if (albumDetails && albumDetails.tracks) {
                            const existingIds = new Set(songs.map(s => s.id));
                            const albumSongs = albumDetails.tracks.filter(s => !existingIds.has(s.id));
                            songs = [...albumSongs, ...songs].slice(0, 12);
                        }
                    } catch (e) {
                        console.error('[Search] Smart Album Association failed:', e);
                    }
                }
            }

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



    async initProfile() {
        const recentList = document.getElementById('profile-recent-list');
        const genresList = document.getElementById('profile-genres-list');

        // --- Load profile info from localStorage ---
        const profile = JSON.parse(localStorage.getItem('iv_profile') || '{}');
        const nameEl = document.getElementById('profile-name-display');
        const bioEl = document.getElementById('profile-bio-display');
        if (nameEl && profile.name) nameEl.textContent = profile.name;
        if (bioEl && profile.bio) bioEl.textContent = profile.bio;

        // --- Populate stat counters ---
        const scores = DiscoveryEngine.getScores();

        // Liked Songs = tracks with score >= 3
        const likedCount = Object.values(scores.tracks || {}).filter(s => s >= 3).length;
        const likedEl = document.getElementById('stat-liked-count');
        if (likedEl) likedEl.textContent = likedCount;

        // Minutes Played
        const minutesEl = document.getElementById('stat-minutes-count');
        if (minutesEl) minutesEl.textContent = HistorySystem.getTotalMinutes();

        // Playlists (queried from local PlaylistManager)
        const playlistsEl = document.getElementById('stat-playlists-count');
        if (playlistsEl) {
            playlistsEl.textContent = window.PlaylistManager ? window.PlaylistManager.getPlaylists().length : '0';
        }

        // --- Recently Played ---
        if (recentList) {
            const history = HistorySystem.get();
            if (history.length === 0) {
                recentList.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-icon">🎵</div>
                        <p>Nothing played yet. Start listening!</p>
                    </div>`;
            } else {
                recentList.innerHTML = '';
                history.slice(0, 8).forEach(track => {
                    recentList.appendChild(CardSystem.createCard(track));
                });
            }
        }

        // --- Top Genres ---
        if (genresList) {
            const interests = DiscoveryEngine.getInterests();
            const topGenres = interests.genres.slice(0, 6);

            if (topGenres.length === 0) {
                genresList.innerHTML = '<p class="settings-desc" data-i18n="no_history">No listening history yet.</p>';
            } else {
                genresList.innerHTML = '';
                const genreColors = [
                    ['#ff6b6b', '#ee5a24'], ['#4dd0e1', '#0288d1'], ['#a29bfe', '#6c5ce7'],
                    ['#ffeaa7', '#fdcb6e'], ['#55efc4', '#00b894'], ['#fd79a8', '#e84393']
                ];
                topGenres.forEach((genre, i) => {
                    const chip = document.createElement('div');
                    chip.className = 'profile-genre-chip';
                    const colors = genreColors[i % genreColors.length];
                    chip.style.background = `linear-gradient(135deg, ${colors[0]}22, ${colors[1]}22)`;
                    chip.style.borderColor = `${colors[0]}44`;
                    chip.innerHTML = `
                        <span class="genre-chip-name" style="color: ${colors[0]}">${genre.name}</span>
                        <span class="genre-chip-score">${genre.score} pts</span>
                    `;
                    genresList.appendChild(chip);
                });
            }
        }

        // --- Top Artists ---
        const artistsList = document.getElementById('profile-top-artists');
        if (artistsList) {
            const interests = DiscoveryEngine.getInterests();
            const topArtists = interests.artists.slice(0, 8);

            if (topArtists.length > 0) {
                const artistContainer = document.getElementById('profile-artists-section');
                if (artistContainer) artistContainer.classList.remove('is-hidden');
                artistsList.innerHTML = '';
                topArtists.forEach(artist => {
                    const card = document.createElement('div');
                    card.className = 'music-card type-artist';
                    card.tabIndex = 0;
                    card.innerHTML = `
                        <div class="card-image-box">
                            <img src="svg/user.svg" class="poster" alt="${artist.name}" style="filter: invert(0.3); padding: 25%;">
                        </div>
                        <div class="card-info-box">
                            <div class="card-title">${artist.name}</div>
                        </div>
                    `;
                    card.onclick = () => window.Router.loadPage('artist', { name: artist.name });
                    card.onkeydown = (e) => { if (e.key === 'Enter') card.click(); };
                    artistsList.appendChild(card);
                });
            }
        }

        // --- Edit Profile Modal ---
        const editBtn = document.querySelector('.profile-actions .btn-primary');
        const modal = document.getElementById('profile-edit-modal');
        if (editBtn && modal) {
            editBtn.onclick = () => {
                const stored = JSON.parse(localStorage.getItem('iv_profile') || '{}');
                const nameInput = document.getElementById('profile-edit-name');
                const bioInput = document.getElementById('profile-edit-bio');
                if (nameInput) nameInput.value = stored.name || '';
                if (bioInput) bioInput.value = stored.bio || '';
                modal.classList.add('active');
            };

            // Close modal
            const closeBtn = modal.querySelector('.modal-close-btn');
            if (closeBtn) closeBtn.onclick = () => modal.classList.remove('active');
            modal.onclick = (e) => { if (e.target === modal) modal.classList.remove('active'); };

            // Save
            const saveBtn = document.getElementById('profile-save-btn');
            if (saveBtn) {
                saveBtn.onclick = () => {
                    const name = document.getElementById('profile-edit-name')?.value.trim();
                    const bio = document.getElementById('profile-edit-bio')?.value.trim();
                    const data = { name: name || 'Listener', bio: bio || '' };
                    localStorage.setItem('iv_profile', JSON.stringify(data));

                    if (nameEl) nameEl.textContent = data.name;
                    if (bioEl) bioEl.textContent = data.bio || 'No bio yet.';
                    modal.classList.remove('active');
                };
            }
        }

        if (window.Loader) window.Loader.init();
        if (window.LanguageManager) window.LanguageManager.translateUI(document.getElementById('main-view'));
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

        // Render user custom playlists
        this.renderPlaylists();

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

    /**
     * Renders a given list of saved tracks in the library list container.
     * serializes track metadata directly on each DOM row, and hooks the click and key events
     * to dynamically populate the player queue from sibling tracks upon playback initiation.
     *
     * @param {Array<Object>} tracks - The list of saved tracks to display in the library.
     */
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
            itemDiv.dataset.trackJson = JSON.stringify(track);
            itemDiv.innerHTML = `
                <div class="track-info">
                    <div class="track-title">${track.title}</div>
                    <div class="track-artist">${track.artist}</div>
                </div>
                <button class="play-local-btn player-control-btn main small" tabindex="-1">
                    <img src="svg/play.svg" alt="Play">
                </button>
            `;

            const playTrack = () => {
                const parent = itemDiv.closest('#library-list-container');
                if (parent) {
                    const siblingDivs = Array.from(parent.querySelectorAll('.library-track-item'));
                    const siblingTracks = siblingDivs.map(d => {
                        try {
                            return JSON.parse(d.dataset.trackJson || '{}');
                        } catch (err) {
                            return null;
                        }
                    }).filter(t => t && t.title);

                    if (siblingTracks.length > 0) {
                        YouTubePlayer.queue = siblingTracks;
                        YouTubePlayer.currentIndex = siblingTracks.findIndex(t => t.title === track.title && t.artist === track.artist);
                        console.log(`[Library Queue] Populated queue with ${siblingTracks.length} saved tracks. Active index: ${YouTubePlayer.currentIndex}`);
                    }
                } else {
                    YouTubePlayer.queue = [track];
                    YouTubePlayer.currentIndex = 0;
                }
                YouTubePlayer.playSavedTrack(track);
            };

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

    /**
     * Renders a list of recently played tracks from history storage inside the Library view.
     * Serializes history data on DOM cards and setups sibling navigation queues.
     */
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
            card.dataset.trackJson = JSON.stringify(track);
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
                const parent = card.closest('#library-recent-list');
                if (parent) {
                    const siblingCards = Array.from(parent.querySelectorAll('.music-card'));
                    const siblingTracks = siblingCards.map(c => {
                        try {
                            return JSON.parse(c.dataset.trackJson || '{}');
                        } catch (err) {
                            return null;
                        }
                    }).filter(t => t && t.title);

                    if (siblingTracks.length > 0) {
                        YouTubePlayer.queue = siblingTracks;
                        YouTubePlayer.currentIndex = siblingTracks.findIndex(t => t.title === track.title && t.artist === track.artist);
                        console.log(`[Recent Queue] Populated queue with ${siblingTracks.length} history tracks. Active index: ${YouTubePlayer.currentIndex}`);
                    }
                } else {
                    YouTubePlayer.queue = [track];
                    YouTubePlayer.currentIndex = 0;
                }

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
        HistorySystem.clear();
        this.renderRecentTracks();
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
    /**
     * Initializes the album view by fetching metadata and track listing for a specific album ID.
     * Hydrates the album hero layout, sets dynamic brand accent colors, wires up the big "Play Album"
     * action button (populates complete sequential tracks in player), and renders the individual track rows
     * with serialized context-aware click triggers to easily navigate back/forward in the queue.
     *
     * @param {Object} params - The routing parameters containing the active 'id' of the album.
     */
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
                const headerTitleEl = document.getElementById('header-page-title');
                if (headerTitleEl) headerTitleEl.textContent = album.title;
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

                // Play Button - Populates the entire album tracks into active player queue
                const playBtn = document.getElementById('play-album-btn');
                if (playBtn && album.tracks.length > 0) {
                    playBtn.disabled = false;
                    playBtn.onclick = () => {
                        YouTubePlayer.queue = [...album.tracks];
                        YouTubePlayer.currentIndex = 0;
                        YouTubePlayer.loadTrack(album.tracks[0]);
                        console.log(`[Album Play] Started album playback with ${album.tracks.length} tracks.`);
                    };
                }

                // Render Tracks with contextual queue binding
                const tracklistCont = document.getElementById('album-tracklist');
                if (tracklistCont) {
                    tracklistCont.innerHTML = '';
                    album.tracks.forEach((track, index) => {
                        const trackRow = document.createElement('div');
                        trackRow.className = 'album-track-row';
                        trackRow.tabIndex = 0;
                        trackRow.dataset.trackJson = JSON.stringify(track);
                        trackRow.innerHTML = `
                            <div class="track-number">${index + 1}</div>
                            <div class="track-details">
                                <div class="track-title">${track.title}</div>
                                <div class="track-artist">${track.artist}</div>
                            </div>
                            <div class="track-duration">--:--</div>
                        `;

                        trackRow.onclick = () => {
                            const parent = trackRow.closest('#album-tracklist');
                            if (parent) {
                                const siblingRows = Array.from(parent.querySelectorAll('.album-track-row'));
                                const siblingTracks = siblingRows.map(r => {
                                    try {
                                        return JSON.parse(r.dataset.trackJson || '{}');
                                    } catch (err) {
                                        return null;
                                    }
                                }).filter(t => t && t.id);

                                if (siblingTracks.length > 0) {
                                    YouTubePlayer.queue = siblingTracks;
                                    YouTubePlayer.currentIndex = siblingTracks.findIndex(t => t.id === track.id);
                                    console.log(`[Album Queue] Populated queue with ${siblingTracks.length} tracks. Active index: ${YouTubePlayer.currentIndex}`);
                                }
                            } else {
                                YouTubePlayer.queue = [track];
                                YouTubePlayer.currentIndex = 0;
                            }
                            YouTubePlayer.loadTrack(track);
                        };

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
    },

    /**
     * Method: initSong
     * Description: Initializes the song details page. If a track is provided in params,
     *              loads details for that specific track. Otherwise, falls back to the
     *              currently active track in the player. Sets up play, queue, and playlist actions.
     * @param {Object} [params] - Navigation parameters containing the target track object.
     */
    async initSong(params = {}) {
        const track = params.track || window.YouTubePlayer?.currentTrack;
        const signal = window.Router.abortController?.signal;

        // If no track is playing, show an empty state
        if (!track) {
            const main = document.querySelector('.song-page');
            if (main) main.innerHTML = `
                <div class="song-empty-state">
                    <div class="empty-icon-wrapper"><img src="svg/library.svg" alt="" class="empty-icon"></div>
                    <h3 data-i18n="song_no_track">Nothing is playing</h3>
                    <p data-i18n="song_no_track_desc">Play a song first, then tap More Info to see its details.</p>
                    <button class="btn btn-primary" onclick="Router.loadPage('home')" data-i18n="explore_music">Explore Music</button>
                </div>`;
            if (window.LanguageManager) window.LanguageManager.translateUI(document.getElementById('main-view'));
            return;
        }

        if (window.Loader) window.Loader.init();

        // Populate immediately with what we already know from the player
        const setEl = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = val; };
        setEl('song-title', track.title);
        setEl('song-artist', track.artist);
        setEl('song-album', track.album || '—');

        const coverEl = document.getElementById('song-cover');
        if (coverEl) { coverEl.src = track.cover; coverEl.style.opacity = '1'; }

        // Apply background color from cover
        MusicAPI.getAverageColor(track.cover).then(color => {
            const hero = document.querySelector('.song-hero');
            if (hero) hero.style.setProperty('--song-color', color);
        });

        // Wire up Play button
        const playBtn = document.getElementById('song-play-btn');
        if (playBtn) {
            playBtn.onclick = () => {
                if (window.YouTubePlayer) {
                    window.YouTubePlayer.queue = [track];
                    window.YouTubePlayer.currentIndex = 0;
                    window.YouTubePlayer.loadTrack(track);
                }
            };
        }

        // Wire up Play Next button
        const playNextBtn = document.getElementById('song-play-next-btn');
        if (playNextBtn) {
            playNextBtn.onclick = () => {
                if (window.YouTubePlayer) {
                    const currentIdx = window.YouTubePlayer.currentIndex;
                    if (currentIdx === -1) {
                        window.YouTubePlayer.queue = [track];
                        window.YouTubePlayer.currentIndex = 0;
                        window.YouTubePlayer.loadTrack(track);
                    } else {
                        window.YouTubePlayer.queue.splice(currentIdx + 1, 0, track);
                    }
                    alert("Added to Play Next");
                }
            };
        }

        // Wire up Add to Queue button
        const addToQueueBtn = document.getElementById('song-add-to-queue-btn');
        if (addToQueueBtn) {
            addToQueueBtn.onclick = () => {
                if (window.YouTubePlayer) {
                    if (window.YouTubePlayer.queue.length === 0) {
                        window.YouTubePlayer.queue = [track];
                        window.YouTubePlayer.currentIndex = 0;
                        window.YouTubePlayer.loadTrack(track);
                    } else {
                        window.YouTubePlayer.queue.push(track);
                    }
                    alert("Added to Queue");
                }
            };
        }

        // Wire up Add to Playlist button
        const addToPlaylistBtn = document.getElementById('song-add-to-playlist-btn');
        if (addToPlaylistBtn) {
            addToPlaylistBtn.onclick = (e) => {
                e.preventDefault(); e.stopPropagation();
                if (window.CardSystem) {
                    window.CardSystem.openOptionsPopover(e, track);
                }
            };
        }

        // Wire up Save button
        const saveBtn = document.getElementById('song-save-btn');
        if (saveBtn) {
            saveBtn.onclick = () => {
                if (window.YouTubePlayer) {
                    if (window.YouTubePlayer.currentTrack && window.YouTubePlayer.currentTrack.id === track.id) {
                        window.YouTubePlayer.saveTrack();
                    } else {
                        if (window.AndroidAPI && window.AndroidAPI.downloadTrack) {
                            window.AndroidAPI.downloadTrack(track.id, track.title, track.artist, track.cover);
                        } else {
                            alert("Download only supported on mobile app.");
                        }
                    }
                }
            };
        }

        // Wire up artist click
        const artistLink = document.getElementById('song-artist-link');
        if (artistLink) artistLink.onclick = () => Router.loadPage('artist', { name: track.artist });

        // Format helper
        const fmtDur = s => { const m = Math.floor(s / 60); const sec = s % 60; return `${m}:${sec < 10 ? '0' : ''}${sec}`; };

        try {
            // Fetch rich metadata + related/artist tracks in parallel
            const [details, related] = await Promise.all([
                track.id ? MusicAPI.getTrackDetails(track.id, signal).catch(() => null) : Promise.resolve(null),
                track.id ? MusicAPI.getRelatedTracks(track.id, signal).catch(() => []) : Promise.resolve([])
            ]);

            if (signal?.aborted) return;

            if (details) {
                // Update with rich metadata for maximum accuracy
                if (details.title) setEl('song-title', details.title);
                if (details.artist) setEl('song-artist', details.artist);
                if (details.album) setEl('song-album', details.album);

                // Duration
                if (details.duration) setEl('song-duration', fmtDur(details.duration));

                // Release Year
                if (details.releaseDate) setEl('song-year', new Date(details.releaseDate).getFullYear());

                // Album link
                const albumLink = document.getElementById('song-album-link');
                if (albumLink && details.albumId) albumLink.onclick = () => Router.loadPage('album', { id: details.albumId });

                // Genre chips
                const chipsContainer = document.getElementById('song-genres');
                if (chipsContainer) {
                    chipsContainer.innerHTML = '';
                    const genres = details.genres?.length ? details.genres : (track.genre ? [track.genre] : []);
                    genres.forEach(g => {
                        const chip = document.createElement('span');
                        chip.className = 'genre-chip';
                        chip.textContent = g;
                        chipsContainer.appendChild(chip);
                    });
                    // BPM badge
                    if (details.bpm && details.bpm > 0) {
                        const bpmBadge = document.createElement('span');
                        bpmBadge.className = 'bpm-badge';
                        bpmBadge.textContent = `${Math.round(details.bpm)} BPM`;
                        chipsContainer.appendChild(bpmBadge);
                    }
                    // Explicit badge
                    if (details.explicit) {
                        const expBadge = document.createElement('span');
                        expBadge.className = 'explicit-badge';
                        expBadge.textContent = 'E';
                        expBadge.title = 'Explicit content';
                        chipsContainer.appendChild(expBadge);
                    }
                }

                // Fetch artist top tracks for "From the Artist" row
                if (details.artistId) {
                    const artistTracks = await MusicAPI.getArtistTopTracks(details.artistId, 12, signal).catch(() => []);
                    if (signal?.aborted) return;
                    const artistRow = document.getElementById('song-artist-tracks');
                    const artistRowContainer = document.getElementById('song-artist-row');
                    if (artistRow && artistTracks.length > 0) {
                        artistRow.innerHTML = '';
                        artistTracks.forEach(t => artistRow.appendChild(CardSystem.createCard(t)));
                        if (artistRowContainer) artistRowContainer.classList.remove('is-hidden');
                        if (window.Loader) window.Loader.init();
                    }
                }
            }

            // Related tracks row
            const relatedRow = document.getElementById('song-related-tracks');
            const relatedContainer = document.getElementById('song-related-row');
            if (relatedRow && related.length > 0) {
                relatedRow.innerHTML = '';
                related.forEach(t => relatedRow.appendChild(CardSystem.createCard(t)));
                if (relatedContainer) relatedContainer.classList.remove('is-hidden');
                if (window.Loader) window.Loader.init();
            }

        } catch (e) {
            console.error('[initSong] Error:', e);
        }
    },

    /**
     * Renders user created local playlists inside the Library view.
     * Uses standard PlaylistManager data model, hiding empty states and
     * registering card click navigation details.
     */
    renderPlaylists() {
        const list = document.getElementById('library-playlists-list');
        const emptyState = document.getElementById('playlists-empty-state');
        if (!list) return;

        const playlists = window.PlaylistManager ? window.PlaylistManager.getPlaylists() : [];

        if (playlists.length === 0) {
            if (emptyState) emptyState.classList.remove('is-hidden');
            list.classList.add('is-hidden');
            return;
        }

        if (emptyState) emptyState.classList.add('is-hidden');
        list.classList.remove('is-hidden');
        list.innerHTML = '';

        playlists.forEach(pl => {
            const card = document.createElement('div');
            card.className = 'music-card playlist-card';
            card.tabIndex = 0;
            card.dataset.playlistId = pl.id;
            card.innerHTML = `
                <div class="card-image-box" style="background: ${pl.cover}; display: flex; align-items: center; justify-content: center; border-radius: 8px; position: relative;">
                    <span style="font-size: 2.8rem;">📂</span>
                </div>
                <div class="card-info-box">
                    <div class="card-title">${pl.name}</div>
                    <div class="card-artist">${pl.tracks.length} ${pl.tracks.length === 1 ? 'song' : 'songs'}</div>
                </div>
                <button class="delete-playlist-btn" onclick="event.stopPropagation(); PageSystem.deletePlaylist('${pl.id}')" title="Delete Playlist" tabindex="-1" style="position: absolute; top: 8px; right: 8px; background: rgba(0,0,0,0.5); border: none; border-radius: 50%; color: #fff; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; font-size: 1.2rem; cursor: pointer; opacity: 0; transition: opacity 0.2s;">
                    &times;
                </button>
            `;

            card.onclick = () => {
                window.Router.loadPage('playlist', { id: pl.id });
            };

            card.onkeydown = (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    window.Router.loadPage('playlist', { id: pl.id });
                }
            };

            list.appendChild(card);
        });
    },

    /**
     * Deletes a playlist and refreshes the current UI view instantly.
     * @param {string} id - The playlist unique ID
     */
    deletePlaylist(id) {
        if (confirm("Are you sure you want to delete this playlist?")) {
            if (window.PlaylistManager) {
                window.PlaylistManager.deletePlaylist(id);
                this.renderPlaylists();
            }
        }
    },

    /**
     * Opens the Create Playlist dialog overlay.
     */
    openCreatePlaylistModal() {
        const overlay = document.getElementById('playlist-modal-overlay');
        if (overlay) {
            overlay.classList.remove('is-hidden');
            const input = document.getElementById('playlist-new-name');
            if (input) {
                input.value = '';
                setTimeout(() => input.focus(), 50);
            }
            const desc = document.getElementById('playlist-new-desc');
            if (desc) desc.value = '';
        }
    },

    /**
     * Closes the Create Playlist dialog overlay.
     */
    closeCreatePlaylistModal(event, force = false) {
        if (force || event.target.id === 'playlist-modal-overlay') {
            const overlay = document.getElementById('playlist-modal-overlay');
            if (overlay) overlay.classList.add('is-hidden');
        }
    },

    /**
     * Submits the create playlist input fields to the manager and refreshes views.
     */
    submitCreatePlaylist() {
        const nameInput = document.getElementById('playlist-new-name');
        const descInput = document.getElementById('playlist-new-desc');
        const name = nameInput ? nameInput.value.trim() : '';
        const desc = descInput ? descInput.value.trim() : '';

        if (!name) {
            alert('Please enter a playlist name.');
            return;
        }

        if (window.PlaylistManager) {
            window.PlaylistManager.createPlaylist(name, desc);
            this.renderPlaylists();
            const overlay = document.getElementById('playlist-modal-overlay');
            if (overlay) overlay.classList.add('is-hidden');
        }
    },

    /**
     * Initializes the playlist detail view.
     * Loads the target custom playlist from storage, updates the HTML elements
     * (name, description, cover gradient, song count), and lists all tracks.
     * Sets up play-all queue triggers and track remove helpers.
     * 
     * @param {Object} params - Routing parameters containing { id: string }
     */
    async initPlaylist(params) {
        const id = params.id;
        if (!id) return;

        const playlist = window.PlaylistManager ? window.PlaylistManager.getPlaylist(id) : null;
        if (!playlist) {
            console.error('[PlaylistDetails] Playlist not found', id);
            return;
        }

        // 1. Render Playlist Metadata
        const nameEl = document.getElementById('playlist-details-name');
        const descEl = document.getElementById('playlist-details-desc');
        const countEl = document.getElementById('playlist-track-count');
        const coverEl = document.getElementById('playlist-cover-wrapper');
        const heroBgEl = document.getElementById('playlist-hero-bg');

        if (nameEl) nameEl.textContent = playlist.name;
        const headerTitleEl = document.getElementById('header-page-title');
        if (headerTitleEl) headerTitleEl.textContent = playlist.name;
        if (descEl) descEl.textContent = playlist.description || '';
        if (countEl) countEl.textContent = `${playlist.tracks.length} ${playlist.tracks.length === 1 ? 'song' : 'songs'}`;
        if (coverEl) {
            coverEl.style.background = playlist.cover;
        }
        if (heroBgEl) {
            heroBgEl.style.setProperty('--accent-glow', playlist.cover.match(/#\w+/)?.[0] || '#fff');
        }

        // 2. Setup Actions
        const playBtn = document.getElementById('play-playlist-btn');
        const deleteBtn = document.getElementById('delete-playlist-detail-btn');

        if (playBtn) {
            if (playlist.tracks.length > 0) {
                playBtn.disabled = false;
                playBtn.onclick = () => {
                    if (window.YouTubePlayer) {
                        window.YouTubePlayer.queue = [...playlist.tracks];
                        window.YouTubePlayer.currentIndex = 0;
                        window.YouTubePlayer.playSavedTrack(playlist.tracks[0]);
                    }
                };
            } else {
                playBtn.disabled = true;
            }
        }

        if (deleteBtn) {
            deleteBtn.onclick = () => {
                if (confirm("Are you sure you want to delete this playlist?")) {
                    if (window.PlaylistManager) {
                        window.PlaylistManager.deletePlaylist(id);
                        window.Router.loadPage('library');
                    }
                }
            };
        }

        // 3. Render Tracks List
        const listContainer = document.getElementById('playlist-tracks-list');
        const emptyState = document.getElementById('playlist-tracks-empty');
        if (!listContainer) return;

        listContainer.innerHTML = '';

        if (!playlist.tracks || playlist.tracks.length === 0) {
            if (emptyState) emptyState.classList.remove('is-hidden');
            listContainer.classList.add('is-hidden');
            return;
        }

        if (emptyState) emptyState.classList.add('is-hidden');
        listContainer.classList.remove('is-hidden');

        playlist.tracks.forEach((track, index) => {
            const itemDiv = document.createElement('div');
            itemDiv.className = 'library-track-item playlist-track-item';
            itemDiv.tabIndex = 0;
            itemDiv.dataset.trackJson = JSON.stringify(track);
            itemDiv.innerHTML = `
                <div class="track-info" style="flex: 1;">
                    <div class="track-title" style="font-weight: 600; color: #fff; font-size: 1.05rem;">${track.title}</div>
                    <div class="track-artist" style="color: rgba(255,255,255,0.5); font-size: 0.9rem; margin-top: 3px;">${track.artist}</div>
                </div>
                <div style="display: flex; gap: 12px; align-items: center;">
                    <button class="remove-playlist-track-btn" title="Remove from Playlist" style="background: transparent; border: none; color: rgba(255,50,50,0.6); font-size: 1.4rem; cursor: pointer; padding: 5px 10px;" tabindex="-1">
                        &times;
                    </button>
                    <button class="play-local-btn player-control-btn main small" tabindex="-1" style="width: 32px; height: 32px; border-radius: 50%; background: var(--ui-accent, #fff); display: flex; align-items: center; justify-content: center; border: none; cursor: pointer;">
                        <img src="svg/play.svg" alt="Play" style="width: 14px; height: 14px; filter: brightness(0);">
                    </button>
                </div>
            `;

            // Setup Play Item
            const playTrack = () => {
                if (window.YouTubePlayer) {
                    window.YouTubePlayer.queue = [...playlist.tracks];
                    window.YouTubePlayer.currentIndex = index;
                    window.YouTubePlayer.playSavedTrack(track);
                }
            };

            // Setup Remove Item
            const removeBtn = itemDiv.querySelector('.remove-playlist-track-btn');
            if (removeBtn) {
                removeBtn.onclick = (e) => {
                    e.stopPropagation();
                    if (window.PlaylistManager) {
                        window.PlaylistManager.removeTrack(id, track.id);
                        this.initPlaylist(params); // Refresh view
                    }
                };
            }

            itemDiv.onclick = playTrack;
            itemDiv.onkeydown = (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    playTrack();
                }
            };

            listContainer.appendChild(itemDiv);
        });

        if (window.Loader) window.Loader.init();
        if (window.LanguageManager) window.LanguageManager.translateUI(document.getElementById('main-view'));
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

// Export PageSystem for inline HTML handlers (like clearHistory and filterLibrary)
if (typeof window !== 'undefined') {
    window.PageSystem = PageSystem;
}
