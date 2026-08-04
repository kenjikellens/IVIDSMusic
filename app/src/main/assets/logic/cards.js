import { MusicRepository } from './api/MusicRepository.js';
import { MediaPlayer } from './player/MediaPlayer.js';

/**
 * Shared logic for creating, hydrating, and managing music cards across the app,
 * optimized for 60fps rendering, off-main-thread image decoding, and zero layout thrashing.
 */
export class CardComponentFactory {
    /**
     * Creates a new music card DOM element and hydrates it with track data.
     * @param {Object} track
     * @returns {HTMLElement}
     */
    static createCard(track) {
        const card = document.createElement('div');
        return this.hydrateCard(card, track);
    }

    /**
     * Hydrates a card DOM container with track metadata, cover artwork, and event hooks.
     * @param {HTMLElement} card
     * @param {Object} track
     * @returns {HTMLElement}
     */
    static hydrateCard(card, track) {
        if (!card || !track) return card;

        card.className = `music-card container-hover-effect type-${track.type || 'song'}`;
        card.tabIndex = 0;
        card.dataset.trackJson = JSON.stringify(track);

        const title = track.title || track.name || 'Unknown';
        const artist = track.artist || 'Unknown Artist';
        const cover = track.cover || 'gui/gemini-logo.png';
        const isExplicit = track.explicit || track.isExplicit;

        /* Card HTML template — play button positioned bottom-right inside image box */
        card.innerHTML = `
            <div class="card-image-box">
                ${track.type === 'artist' ? '<div class="ivids-loader poster-loader"></div>' : ''}
                <img src="${cover}" alt="${title}" class="poster" loading="lazy" style="${track.type === 'artist' ? 'opacity: 0' : ''}">
                ${(track.type === 'song' || track.type === 'album' || !track.type) ? `
                    <button class="card-play-btn" title="Play" tabindex="-1">
                        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
                    </button>
                ` : ''}
            </div>
            <div class="card-info-box">
                <div class="card-title"><span class="marquee-text">${title}</span></div>
                ${track.type === 'artist' ? '' : `
                    <div class="card-artist">
                        ${isExplicit ? '<span class="explicit-badge" title="Explicit">E</span>' : ''}
                        <a href="#" class="artist-link marquee-text" data-name="${artist}">
                            ${artist}
                        </a>
                    </div>
                `}
            </div>
        `;

        /* Off-main-thread image decoding */
        const img = card.querySelector('.poster');
        if (img && img.decode) {
            img.decode().catch(() => {});
        }

        /* Extract average color for card accent */
        if (MusicRepository.getAverageColor) {
            MusicRepository.getAverageColor(cover).then(color => {
                if (color) card.style.setProperty('--card-color', color);
            }).catch(() => {});
        }

        /* Artist image lazy-load with decode animation */
        if (track.type === 'artist' && MusicRepository.getArtistImage) {
            MusicRepository.getArtistImage(artist).then(imgUrl => {
                const loader = card.querySelector('.poster-loader');
                if (imgUrl && img) {
                    img.src = imgUrl;
                    if (img.decode) {
                        img.decode().then(() => {
                            img.style.opacity = '1';
                            if (loader) loader.remove();
                        }).catch(() => {
                            img.style.opacity = '1';
                            if (loader) loader.remove();
                        });
                    } else {
                        img.onload = () => { img.style.opacity = '1'; if (loader) loader.remove(); };
                    }
                } else if (img) {
                    img.style.opacity = '1';
                    if (loader) loader.remove();
                }
            }).catch(() => {
                const loader = card.querySelector('.poster-loader');
                if (img) img.style.opacity = '1';
                if (loader) loader.remove();
            });
        }

        /* Play button click → start playback directly (stopPropagation prevents card navigation) */
        const playBtn = card.querySelector('.card-play-btn');
        if (playBtn) {
            playBtn.onclick = (e) => {
                e.preventDefault();
                e.stopPropagation();
                MediaPlayer.playTrack(track);
            };
        }

        /* Card body click → navigate to page (song page, album page, artist page) */
        card.onclick = (e) => {
            if (e.target.classList.contains('artist-link')) {
                e.preventDefault();
                e.stopPropagation();
                if (window.Router) window.Router.loadPage('artist', { name: e.target.dataset.name || artist });
                return;
            }

            if (track.type === 'artist') {
                if (window.Router) window.Router.loadPage('artist', { name: track.name || title });
            } else if (track.type === 'album') {
                if (window.Router) window.Router.loadPage('album', { id: track.id });
            } else {
                /* Song card click → open song detail page */
                if (window.Router) window.Router.loadPage('song', { id: track.id });
            }
        };

        return card;
    }

    /**
     * Renders a batch of cards into a target container in a single reflow using DocumentFragment.
     * @param {HTMLElement} container
     * @param {Array<Object>} items
     * @param {string} [type]
     */
    static renderCards(container, items = [], type = 'song') {
        if (!container) return;
        container.innerHTML = '';
        const fragment = document.createDocumentFragment();
        items.forEach(item => {
            if (!item.type) item.type = type;
            fragment.appendChild(this.createCard(item));
        });
        container.appendChild(fragment);
    }

    /**
     * Creates a full music row component with header, title, and populated horizontal scroll container.
     * @param {string} title - Section header title text
     * @param {string} containerId - DOM ID for the scroll container
     * @param {Array<Object>} items - Array of track/card objects
     * @param {string} [type] - Default card item type
     * @returns {HTMLElement} The complete music row element
     */
    static createRow(title, containerId, items = [], type = 'song') {
        const row = document.createElement('div');
        row.className = 'row-container';

        const header = document.createElement('div');
        header.className = 'row-header';

        const titleEl = document.createElement('h2');
        titleEl.className = 'row-title';
        titleEl.textContent = title;
        header.appendChild(titleEl);

        const postersContainer = document.createElement('div');
        postersContainer.className = 'scroll-row';
        if (containerId) postersContainer.id = containerId;

        this.renderCards(postersContainer, items, type);

        row.appendChild(header);
        row.appendChild(postersContainer);
        return row;
    }

    /**
     * Creates a 1-to-1 skeleton row component matching the exact geometry of a hydrated row.
     * @param {string} title - Section header title text
     * @param {string} containerId - DOM ID for the scroll container
     * @param {number} [count=6] - Number of skeleton cards to generate
     * @returns {HTMLElement} The skeleton row element
     */
    static createSkeletonRow(title, containerId, count = 6) {
        const row = document.createElement('div');
        row.className = 'row-container';

        const header = document.createElement('div');
        header.className = 'row-header';

        const titleEl = document.createElement('h2');
        titleEl.className = 'row-title';
        titleEl.textContent = title;
        header.appendChild(titleEl);

        const postersContainer = document.createElement('div');
        postersContainer.className = 'scroll-row skeleton-row';
        if (containerId) postersContainer.id = containerId;

        let skeletonsHTML = '';
        for (let i = 0; i < count; i++) {
            skeletonsHTML += `
                <div class="music-card skeleton-card container-hover-effect">
                    <div class="skeleton-img">
                        <img src="svg/loader.svg" alt="Loading" class="ivids-loader-img poster-loader">
                    </div>
                    <div class="skeleton-info-box">
                        <div class="skeleton-text title"></div>
                        <div class="skeleton-text artist"></div>
                    </div>
                </div>
            `;
        }
        postersContainer.innerHTML = skeletonsHTML;

        row.appendChild(header);
        row.appendChild(postersContainer);
        return row;
    }
}

export const CardsEngine = CardComponentFactory;
export const CardSystem = CardComponentFactory;
