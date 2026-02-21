import { MusicAPI } from './api.js';
import { YouTubePlayer } from './player.js';

/**
 * Shared logic for creating and managing music cards across the app.
 */
export const CardSystem = {
    /**
     * Creates a music card DOM element.
     */
    createCard(track) {
        const card = document.createElement('div');
        card.className = `music-card type-${track.type || 'song'}`;
        card.tabIndex = 0;

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
            <div class="card-info-box">
                <div class="card-title">${track.title || track.name}</div>
                <div class="card-artist">
                    ${track.type === 'artist' ? (track.genre || 'Artist') : `
                        <a href="#" class="artist-link" data-name="${track.artist}">
                            ${track.artist}
                        </a>
                    `}
                </div>
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

        card.onclick = (e) => {
            if (e.target.classList.contains('artist-link')) {
                e.preventDefault(); e.stopPropagation();
                window.Router.loadPage('artist', { name: e.target.dataset.name });
                return;
            }
            if (track.type === 'song') YouTubePlayer.loadTrack(track);
            else if (track.type === 'album') window.Router.loadPage('search', { query: track.title || track.name, type: 'song' });
            else if (track.type === 'artist') window.Router.loadPage('artist', { name: track.name });
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
                <h2 class="row-title">${title}</h2>
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
    }
};

// Global helper for row scrolling
window.scrollRow = (id, dir) => {
    const el = document.getElementById(id);
    if (el) el.scrollBy({ left: dir * el.clientWidth * 0.8, behavior: 'smooth' });
};
