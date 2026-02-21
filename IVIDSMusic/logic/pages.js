import { MusicAPI } from './api.js';
import { YouTubePlayer } from './player.js';
import { CardSystem } from './cards.js';

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
                <div class="skeleton-text title"></div>
                <div class="skeleton-text artist"></div>
            </div>
        `;

        // Pre-render skeletons with titles
        container.innerHTML = GENRES.map(genre => `
            <div class="row-container">
                <div class="row-header"><h2 class="row-title">${genre}</h2></div>
                <div class="skeleton-row">${SKELETON_CARD.repeat(12)}</div>
            </div>
        `).join('');

        if (window.Loader) window.Loader.init();

        try {
            const rows = await MusicAPI.getRecommendations();
            container.innerHTML = '';
            rows.forEach(category => container.appendChild(CardSystem.createRow(category.title, category.id, category.tracks)));

            if (window.Loader) window.Loader.init(); // Init loaders in real cards if any

            const heroBtn = document.getElementById('play-hero-btn');
            if (heroBtn && rows[0]?.tracks[0]) {
                heroBtn.onclick = () => YouTubePlayer.loadTrack(rows[0].tracks[0]);
            }
        } catch (e) {
            container.innerHTML = '<p class="error-msg">Failed to load content.</p>';
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

        if (!query) {
            if (browseHero) browseHero.style.display = 'flex';
            if (resultsHeader) resultsHeader.style.display = 'none';
            if (rowsCont) rowsCont.style.display = 'none';
            if (gridView) gridView.style.display = 'none';
            return;
        }

        if (browseHero) browseHero.style.display = 'none';
        if (resultsHeader) resultsHeader.style.display = 'flex';
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
                if (data.length === 0) get('load-more-container').style.display = 'none';
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
                <div class="skeleton-text title"></div>
                <div class="skeleton-text artist"></div>
            </div>`;

        if (params.type) {
            if (rowsCont) rowsCont.style.display = 'none';
            if (gridView) gridView.style.display = 'block';
            if (get('grid-title')) get('grid-title').textContent = params.type.charAt(0).toUpperCase() + params.type.slice(1) + "s";
            const grid = get('grid-results');
            grid.innerHTML = SKELETON_CARD.repeat(12);
            if (window.Loader) window.Loader.init();

            const data = await MusicAPI.search(query, PAGE_SIZE, params.type, params.year, 0, true);
            grid.innerHTML = '';
            data.forEach(item => grid.appendChild(CardSystem.createCard(item)));
            if (window.Loader) window.Loader.init();

            if (get('load-more-container')) get('load-more-container').style.display = data.length >= PAGE_SIZE ? 'block' : 'none';
        } else {
            if (rowsCont) rowsCont.style.display = 'block';
            if (gridView) gridView.style.display = 'none';
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
                if (get(contId)) get(contId).style.display = data.length ? 'block' : 'none';
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
    }
};

// Global UI helpers
window.performSearch = (q) => window.Router.loadPage('search', { query: q });
window.handleBrowseInput = (v) => { if (document.getElementById('header-search-input')) document.getElementById('header-search-input').value = v; };
window.setSearchFilter = (t) => {
    const p = window.Router.currentParams || {};
    window.Router.loadPage('search', { query: p.query, type: t });
};
