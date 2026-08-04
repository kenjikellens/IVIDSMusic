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

        card.className = `card card-${track.type || 'song'}`;
        card.tabIndex = 0;
        card.dataset.trackJson = JSON.stringify(track);

        const title = track.title || track.name || 'Unknown';
        const artist = track.artist || 'Unknown Artist';
        const cover = track.cover || 'gui/gemini-logo.png';

        card.innerHTML = `
            <div class="card-image-box">
                ${track.type === 'artist' ? '<div class="ivids-loader poster-loader"></div>' : ''}
                <img src="${cover}" alt="${title}" class="poster" loading="lazy" style="${track.type === 'artist' ? 'opacity: 0' : ''}">
                ${track.type === 'song' ? `
                    <div class="card-play-overlay">
                        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
                    </div>
                ` : ''}
            </div>
            ${track.type === 'song' ? `
                <button class="btn btn-ghost card-more-btn" title="Options" tabindex="-1">
                    ⋮
                </button>
            ` : ''}
            <div class="card-info-box">
                <div class="card-title"><span class="marquee-text">${title}</span></div>
                ${track.type === 'artist' ? '' : `
                    <div class="card-artist">
                        <a href="#" class="artist-link marquee-text" data-name="${artist}">
                            ${artist}
                        </a>
                    </div>
                `}
            </div>
        `;

        const img = card.querySelector('.poster');
        if (img && img.decode) {
            img.decode().catch(() => {});
        }

        if (MusicRepository.getAverageColor) {
            MusicRepository.getAverageColor(cover).then(color => {
                if (color) card.style.setProperty('--card-color', color);
            }).catch(() => {});
        }

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

        const moreBtn = card.querySelector('.card-more-btn');
        if (moreBtn) {
            moreBtn.onclick = (e) => {
                e.preventDefault();
                e.stopPropagation();
                this.openOptionsPopover(e, track);
            };
        }

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
                MediaPlayer.playTrack(track);
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
     * Opens contextual options popover menu for a track.
     */
    static openOptionsPopover(event, track) {
        const existing = document.getElementById('add-to-playlist-popover');
        if (existing) existing.remove();

        const popover = document.createElement('div');
        popover.id = 'add-to-playlist-popover';
        popover.className = 'glassmorphism add-to-playlist-popup';

        const rect = event.currentTarget.getBoundingClientRect();
        popover.style.position = 'absolute';
        popover.style.top = `${rect.bottom + window.scrollY + 5}px`;
        popover.style.left = `${Math.min(rect.left + window.scrollX, window.innerWidth - 220)}px`;
        popover.style.zIndex = '9999';

        document.body.appendChild(popover);

        popover.innerHTML = `
            <div class="popup-title">${track.title || track.name}</div>
            <div class="popup-list">
                <button class="btn btn-ghost popup-item" id="opt-play-now">
                    <span class="popup-icon">▶</span>
                    <span class="popup-text">Play Now</span>
                </button>
                <button class="btn btn-ghost popup-item" id="opt-add-to-queue">
                    <span class="popup-icon">➕</span>
                    <span class="popup-text">Add to Queue</span>
                </button>
            </div>
        `;

        const optPlayNow = popover.querySelector('#opt-play-now');
        if (optPlayNow) {
            optPlayNow.onclick = (e) => {
                e.preventDefault(); e.stopPropagation();
                MediaPlayer.playTrack(track);
                popover.remove();
            };
        }

        const optAddToQueue = popover.querySelector('#opt-add-to-queue');
        if (optAddToQueue) {
            optAddToQueue.onclick = (e) => {
                e.preventDefault(); e.stopPropagation();
                MediaPlayer.queueManager.addTrack(track);
                popover.remove();
            };
        }

        const closeHandler = (e) => {
            if (!popover.contains(e.target)) {
                popover.remove();
                document.removeEventListener('click', closeHandler);
            }
        };

        setTimeout(() => {
            document.addEventListener('click', closeHandler);
        }, 50);
    }
}

export const CardsEngine = CardComponentFactory;
export const CardSystem = CardComponentFactory;
