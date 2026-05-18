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
                    <button class="card-add-to-playlist-btn" title="Add to Playlist" tabindex="-1">
                        +
                    </button>
                ` : ''}
            </div>
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

        const addBtn = card.querySelector('.card-add-to-playlist-btn');
        if (addBtn) {
            addBtn.onclick = (e) => {
                e.preventDefault();
                e.stopPropagation();
                this.openAddToPlaylistPopover(e, track);
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
    openAddToPlaylistPopover(event, track) {
        // Remove any existing popover first
        const existing = document.getElementById('add-to-playlist-popover');
        if (existing) existing.remove();

        const playlists = window.PlaylistManager ? window.PlaylistManager.getPlaylists() : [];

        const popover = document.createElement('div');
        popover.id = 'add-to-playlist-popover';
        popover.className = 'glassmorphism add-to-playlist-popup';
        
        // Calculate position relative to viewport
        const rect = event.currentTarget.getBoundingClientRect();
        popover.style.position = 'fixed';
        popover.style.top = `${rect.bottom + window.scrollY + 5}px`;
        popover.style.left = `${Math.min(rect.left + window.scrollX, window.innerWidth - 220)}px`;
        popover.style.zIndex = '9999';

        let innerHTML = `<div class="popup-title">Add to Playlist</div>`;
        
        if (playlists.length === 0) {
            innerHTML += `
                <div class="popup-empty-state">
                    <p>No playlists found.</p>
                    <a href="#" onclick="event.preventDefault(); window.Router.loadPage('library')">Create in Library</a>
                </div>
            `;
        } else {
            innerHTML += `<div class="popup-list">`;
            playlists.forEach(pl => {
                innerHTML += `
                    <button class="popup-item" data-playlist-id="${pl.id}">
                        <span class="popup-icon">📂</span>
                        <span class="popup-text">${pl.name}</span>
                    </button>
                `;
            });
            innerHTML += `</div>`;
        }

        popover.innerHTML = innerHTML;
        document.body.appendChild(popover);

        // Bind clicks for playlist options
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
