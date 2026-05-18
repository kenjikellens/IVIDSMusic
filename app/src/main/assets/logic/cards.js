import { MusicAPI } from './api.js';
import { YouTubePlayer } from './player.js';
import { DiscoveryEngine } from './recommendations.js';

/**
 * Shared logic for creating and managing music cards across the app.
 */
export const CardSystem = {
    /**
     * Creates a new unhydrated music card DOM element and populates it with track data.
     *
     * @param {Object} track - The data object representing a track, song, album, or artist.
     * @returns {HTMLDivElement} A complete interactive music card element.
     */
    createCard(track) {
        const card = document.createElement('div');
        return this.hydrateCard(card, track);
    },

    /**
     * Hydrates a given DOM card container with track data. Configures proper styling classes,
     * builds and injects descriptive inner HTML markup (handling cover, title, artist link overlays),
     * extracts vibrant dominant colors for cards, registers keyboard accessibility keys,
     * and sets up a robust contextual click queue system for smooth player control transitions.
     *
     * @param {HTMLElement} card - The DOM container element to hydrate.
     * @param {Object} track - The data object containing track/artist details.
     * @returns {HTMLElement} The populated, interactive card element.
     */
    /**
     * Method: hydrateCard
     * Description: Populates a card DOM container with track metadata, cover artwork, and event hooks.
     *              Places an absolute-positioned three-dots options button at the top-right of the card,
     *              allowing secondary play/queue/playlist controls to be loaded contextually.
     * @param {HTMLElement} card - The DOM container element to populate.
     * @param {Object} track - The data object containing track/artist details.
     * @returns {HTMLElement} The populated, interactive card element.
     */
    hydrateCard(card, track) {
        card.className = `music-card type-${track.type || 'song'}`;
        card.tabIndex = 0;
        card.dataset.trackJson = JSON.stringify(track);

        card.innerHTML = `
            <div class="card-image-box">
                ${track.type === 'artist' ? '<div class="ivids-loader poster-loader"></div>' : ''}
                <img src="${track.cover}" alt="${track.title || track.name}" class="poster" loading="lazy" style="${track.type === 'artist' ? 'opacity: 0' : ''}">
                ${track.type === 'song' ? `
                    <div class="card-play-overlay">
                        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
                    </div>
                ` : ''}
            </div>
            ${track.type === 'song' ? `
                <button class="card-more-btn" title="Options" tabindex="-1">
                    ⋮
                </button>
            ` : ''}
            <div class="card-info-box">
                <div class="card-title">${track.title || track.name}</div>
                ${track.type === 'artist' ? '' : `
                    <div class="card-artist">
                        <a href="#" class="artist-link" data-name="${track.artist}">
                            ${track.artist}
                        </a>
                    </div>
                `}
            </div>
        `;

        MusicAPI.getAverageColor(track.cover).then(color => {
            card.style.setProperty('--card-color', color);
        });

        if (track.type === 'artist') {
            MusicAPI.getArtistImage(track.name).then(imgUrl => {
                const img = card.querySelector('.poster');
                const loader = card.querySelector('.poster-loader');
                if (imgUrl && img) {
                    img.src = imgUrl;
                    img.onload = () => { img.style.opacity = '1'; if (loader) loader.remove(); };
                } else if (img) {
                    img.style.opacity = '1';
                    if (loader) loader.remove();
                }
            }).catch(() => {
                const img = card.querySelector('.poster');
                const loader = card.querySelector('.poster-loader');
                if (img) img.style.opacity = '1';
                if (loader) loader.remove();
            });
        }

        const moreBtn = card.querySelector('.card-more-btn');
        if (moreBtn) {
            moreBtn.onclick = (e) => {
                e.preventDefault();
                e.stopPropagation();
                this.openOptionsPopover(e, track);
            };
        }

        card.onclick = (e) => {
            // Check if the click target is an inline link leading to an artist's page
            if (e.target.classList.contains('artist-link')) {
                e.preventDefault(); e.stopPropagation();
                // Record telemetry scoring click for the artist with both ID and human-readable name
                DiscoveryEngine.recordArtistClick(track.artistId, e.target.dataset.name || track.artist);
                window.Router.loadPage('artist', { name: e.target.dataset.name });
                return;
            }

            // Determine routing and scoring action based on the card's active type
            if (track.type === 'artist') {
                // Scoring: Increment top interests score for artist by supplying ID and name
                DiscoveryEngine.recordArtistClick(track.id, track.name);
                window.Router.loadPage('artist', { name: track.name });
            } else if (track.type === 'album') {
                window.Router.loadPage('album', { id: track.id });
            } else {
                // Sibling Queue Builder: Look up matching containers to group similar songs automatically
                // Added '.row-posters' to include Home page category rows in sequential auto-play queue resolution
                const parent = card.closest('.results-row, .row-posters, .profile-recent-list, .grid-results, #recommended-content');
                if (parent) {
                    const siblingCards = Array.from(parent.querySelectorAll('.music-card.type-song'));
                    const siblingTracks = siblingCards.map(c => {
                        try {
                            return JSON.parse(c.dataset.trackJson || '{}');
                        } catch (err) {
                            return null;
                        }
                    }).filter(t => t && t.id);

                    if (siblingTracks.length > 0) {
                        YouTubePlayer.queue = siblingTracks;
                        YouTubePlayer.currentIndex = siblingTracks.findIndex(t => t.id === track.id);
                        console.log(`[Queue Builder] Populated queue with ${siblingTracks.length} tracks. Active index: ${YouTubePlayer.currentIndex}`);
                    }
                } else {
                    // Fallback to single item queue if container isn't recognized
                    YouTubePlayer.queue = [track];
                    YouTubePlayer.currentIndex = 0;
                }

                // Load and play the active track
                YouTubePlayer.loadTrack(track);
            }
        };

        card.onkeydown = (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                card.click();
            }
        };

        return card;
    },

    /**
     * Creates a scrollable row of cards.
     */
    createRow(title, id, tracks) {
        const rowWrapper = document.createElement('div');
        rowWrapper.className = 'row-scroll-wrapper';
        rowWrapper.innerHTML = `
            <div class="row-header">
                <h2 class="row-title" data-i18n="${id}_header">${title}</h2>
            </div>
            <button class="scroll-arrow left" onclick="scrollRow('${id}', -1)">
                <div class="scroll-arrow-icon">❮</div>
            </button>
            <div class="results-row" id="${id}"></div>
            <button class="scroll-arrow right" onclick="scrollRow('${id}', 1)">
                <div class="scroll-arrow-icon">❯</div>
            </button>
        `;

        const row = rowWrapper.querySelector('.results-row');
        tracks.forEach(track => row.appendChild(this.createCard(track)));
        return rowWrapper;
    },

    /**
     * Opens a floating accessible popover overlay to let the user add the selected track
     * to any of their custom local playlists.
     * 
     * @param {Event} event - The triggering click event containing coordinate locations.
     * @param {Object} track - The track data object to add.
     */
    /**
     * Method: openOptionsPopover
     * Description: Spawns a premium glassmorphic contextual popover for track interaction,
     *              supporting playing immediately, queueing next, appending to the active queue,
     *              or transitioning into a nested menu for localized playlist addition.
     * @param {Event} event - Triggering mouse/touch event containing client coordinates.
     * @param {Object} track - The active song/track data object.
     */
    openOptionsPopover(event, track) {
        // Remove any existing popover first
        const existing = document.getElementById('add-to-playlist-popover');
        if (existing) existing.remove();

        const popover = document.createElement('div');
        popover.id = 'add-to-playlist-popover';
        popover.className = 'glassmorphism add-to-playlist-popup';
        
        // Calculate position relative to document layout
        const rect = event.currentTarget.getBoundingClientRect();
        popover.style.position = 'absolute';
        popover.style.top = `${rect.bottom + window.scrollY + 5}px`;
        popover.style.left = `${Math.min(rect.left + window.scrollX, window.innerWidth - 220)}px`;
        popover.style.zIndex = '9999';

        document.body.appendChild(popover);

        // Renders the main options list (Play Now, Play Next, Add to Queue, Add to Playlist...)
        const showMainMenu = () => {
            popover.innerHTML = `
                <div class="popup-title">${track.title}</div>
                <div class="popup-list">
                    <button class="popup-item" id="opt-play-now">
                        <span class="popup-icon">▶</span>
                        <span class="popup-text">Play Now</span>
                    </button>
                    <button class="popup-item" id="opt-play-next">
                        <span class="popup-icon">⏭</span>
                        <span class="popup-text">Play Next</span>
                    </button>
                    <button class="popup-item" id="opt-add-to-queue">
                        <span class="popup-icon">➕</span>
                        <span class="popup-text">Add to Queue</span>
                    </button>
                    <button class="popup-item" id="opt-add-to-playlist">
                        <span class="popup-icon">📂</span>
                        <span class="popup-text">Add to Playlist...</span>
                    </button>
                </div>
            `;

            // Play Now Action
            const optPlayNow = popover.querySelector('#opt-play-now');
            if (optPlayNow) {
                optPlayNow.onclick = (e) => {
                    e.preventDefault(); e.stopPropagation();
                    if (window.YouTubePlayer) {
                        window.YouTubePlayer.queue = [track];
                        window.YouTubePlayer.currentIndex = 0;
                        window.YouTubePlayer.loadTrack(track);
                    }
                    popover.remove();
                };
            }

            // Play Next Action
            const optPlayNext = popover.querySelector('#opt-play-next');
            if (optPlayNext) {
                optPlayNext.onclick = (e) => {
                    e.preventDefault(); e.stopPropagation();
                    if (window.YouTubePlayer) {
                        const currentIdx = window.YouTubePlayer.currentIndex;
                        if (currentIdx === -1) {
                            window.YouTubePlayer.queue = [track];
                            window.YouTubePlayer.currentIndex = 0;
                            window.YouTubePlayer.loadTrack(track);
                        } else {
                            window.YouTubePlayer.queue.splice(currentIdx + 1, 0, track);
                        }
                    }
                    popover.remove();
                };
            }

            // Add to Queue Action
            const optAddToQueue = popover.querySelector('#opt-add-to-queue');
            if (optAddToQueue) {
                optAddToQueue.onclick = (e) => {
                    e.preventDefault(); e.stopPropagation();
                    if (window.YouTubePlayer) {
                        if (window.YouTubePlayer.queue.length === 0) {
                            window.YouTubePlayer.queue = [track];
                            window.YouTubePlayer.currentIndex = 0;
                            window.YouTubePlayer.loadTrack(track);
                        } else {
                            window.YouTubePlayer.queue.push(track);
                        }
                    }
                    popover.remove();
                };
            }

            // Navigate to Playlist Submenu
            const optAddToPlaylist = popover.querySelector('#opt-add-to-playlist');
            if (optAddToPlaylist) {
                optAddToPlaylist.onclick = (e) => {
                    e.preventDefault(); e.stopPropagation();
                    showPlaylistMenu();
                };
            }
        };

        // Renders the sub-level playlist selector interface within the same popover
        const showPlaylistMenu = () => {
            const playlists = window.PlaylistManager ? window.PlaylistManager.getPlaylists() : [];
            let html = `
                <div class="popup-header-row" style="display:flex; align-items:center; border-bottom:1px solid rgba(255,255,255,0.05); padding: calc(4px * var(--ui-scale)) calc(6px * var(--ui-scale)); margin-bottom: calc(4px * var(--ui-scale));">
                    <button id="popup-back-btn" class="popup-back-btn" style="background:transparent; border:none; color:#fff; cursor:pointer; font-size: calc(1rem * var(--ui-scale)); margin-right: calc(6px * var(--ui-scale)); padding: 0 calc(4px * var(--ui-scale)); display:flex; align-items:center; justify-content:center;">❮</button>
                    <div class="popup-title" style="border:none; padding:0; flex-grow:1;">Add to Playlist</div>
                </div>
            `;

            if (playlists.length === 0) {
                html += `
                    <div class="popup-empty-state">
                        <p>No playlists found.</p>
                        <a href="#" onclick="event.preventDefault(); window.Router.loadPage('library')">Create in Library</a>
                    </div>
                `;
            } else {
                html += `<div class="popup-list">`;
                playlists.forEach(pl => {
                    html += `
                        <button class="popup-item" data-playlist-id="${pl.id}">
                            <span class="popup-icon">📂</span>
                            <span class="popup-text">${pl.name}</span>
                        </button>
                    `;
                });
                html += `</div>`;
            }

            popover.innerHTML = html;

            // Back Button click returns to Main Menu options
            const backBtn = popover.querySelector('#popup-back-btn');
            if (backBtn) {
                backBtn.onclick = (e) => {
                    e.preventDefault(); e.stopPropagation();
                    showMainMenu();
                };
            }

            // Bind click handlers for target playlist selections
            popover.querySelectorAll('.popup-item').forEach(btn => {
                btn.onclick = (clickEvent) => {
                    clickEvent.preventDefault();
                    clickEvent.stopPropagation();
                    const plId = btn.dataset.playlistId;
                    if (window.PlaylistManager) {
                        const result = window.PlaylistManager.addTrack(plId, track);
                        alert(result.message);
                    }
                    popover.remove();
                };
            });
        };

        // Initialize display to main options menu
        showMainMenu();

        // Close on clicking outside
        const closeHandler = (e) => {
            if (!popover.contains(e.target)) {
                popover.remove();
                document.removeEventListener('click', closeHandler);
            }
        };
        
        // Timeout to avoid instant closing from the current click event propagating
        setTimeout(() => {
            document.addEventListener('click', closeHandler);
        }, 50);
    }
};

// Global helper for row scrolling
window.scrollRow = (id, dir) => {
    const el = document.getElementById(id);
    if (el) el.scrollBy({ left: dir * el.clientWidth * 0.8, behavior: 'smooth' });
};
