/**
 * CardComponentFactory handles generating UI card elements (Song, Artist, Album)
 * optimized with DocumentFragment to prevent browser reflow loops.
 */
export class CardComponentFactory {
    /**
     * Creates a HTML element for a song card.
     * @param {Object} song
     * @returns {HTMLElement}
     */
    static createSongCard(song) {
        const div = document.createElement('div');
        div.className = 'music-card song-card';
        div.setAttribute('tabindex', '0');
        div.dataset.id = song.id || '';
        div.dataset.type = 'song';

        div.innerHTML = `
            <div class="card-cover-wrapper">
                <img src="${song.cover || 'gui/gemini-logo.png'}" alt="${song.title || ''}" class="card-cover" loading="lazy" />
                <button class="card-play-btn" aria-label="Play">
                    <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
                </button>
            </div>
            <div class="card-info">
                <div class="card-title">${song.title || 'Unknown Title'}</div>
                <div class="card-subtitle">${song.artist || 'Unknown Artist'}</div>
            </div>
        `;
        return div;
    }

    /**
     * Creates a HTML element for an artist card.
     * @param {Object} artist
     * @returns {HTMLElement}
     */
    static createArtistCard(artist) {
        const div = document.createElement('div');
        div.className = 'music-card artist-card';
        div.setAttribute('tabindex', '0');
        div.dataset.id = artist.id || '';
        div.dataset.type = 'artist';

        div.innerHTML = `
            <div class="card-cover-wrapper round">
                <img src="${artist.cover || 'gui/gemini-logo.png'}" alt="${artist.name || ''}" class="card-cover" loading="lazy" />
            </div>
            <div class="card-info">
                <div class="card-title">${artist.name || 'Unknown Artist'}</div>
                <div class="card-subtitle">Artist</div>
            </div>
        `;
        return div;
    }

    /**
     * Creates a HTML element for an album card.
     * @param {Object} album
     * @returns {HTMLElement}
     */
    static createAlbumCard(album) {
        const div = document.createElement('div');
        div.className = 'music-card album-card';
        div.setAttribute('tabindex', '0');
        div.dataset.id = album.id || '';
        div.dataset.type = 'album';

        div.innerHTML = `
            <div class="card-cover-wrapper">
                <img src="${album.cover || 'gui/gemini-logo.png'}" alt="${album.title || ''}" class="card-cover" loading="lazy" />
            </div>
            <div class="card-info">
                <div class="card-title">${album.title || 'Unknown Album'}</div>
                <div class="card-subtitle">${album.artist || 'Unknown Artist'}</div>
            </div>
        `;
        return div;
    }

    /**
     * Renders a batch of cards into a target container in a single reflow using DocumentFragment.
     * @param {HTMLElement} container
     * @param {Array<Object>} items
     * @param {string} type - 'song', 'artist', 'album'
     */
    static renderCards(container, items = [], type = 'song') {
        if (!container) return;
        container.innerHTML = '';
        const fragment = document.createDocumentFragment();

        items.forEach(item => {
            let card;
            if (type === 'artist' || item.type === 'artist') {
                card = this.createArtistCard(item);
            } else if (type === 'album' || item.type === 'album') {
                card = this.createAlbumCard(item);
            } else {
                card = this.createSongCard(item);
            }
            fragment.appendChild(card);
        });

        container.appendChild(fragment);
    }
}
