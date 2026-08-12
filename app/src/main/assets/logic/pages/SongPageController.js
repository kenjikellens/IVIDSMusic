import { BasePageController } from './BasePageController.js';
import { MusicRepository } from '../api/MusicRepository.js';
import { CardComponentFactory } from '../cards.js';
import { MediaPlayer } from '../player/MediaPlayer.js';
import { LanguageManager } from '../language-manager.js';

/**
 * SongPageController manages track detail view, artist albums, and popular/trending track recommendations.
 */
export class SongPageController extends BasePageController {
    /**
     * Renders detailed song view.
     * @param {Object} params
     */
    async render(params = {}) {
        this.resetAbortController();

        let track = params.track;
        if (!track && params.id) {
            const results = await MusicRepository.search(params.id, 1, 'song', null, 0, false, this.signal);
            if (results.length > 0) track = results[0];
        }

        if (!track) {
            track = MediaPlayer.currentTrack;
        }

        if (!track) {
            const main = document.querySelector('.song-page');
            if (main) {
                main.innerHTML = `
                    <div class="song-empty-state">
                        <div class="empty-icon-wrapper"><img src="svg/library.svg" alt="" class="empty-icon"></div>
                        <h3 data-i18n="song_no_track">Nothing is playing</h3>
                        <p data-i18n="song_no_track_desc">Play a song first, then tap More Info to see its details.</p>
                        <button class="btn btn-primary" onclick="Router.loadPage('home')" data-i18n="explore_music">Explore Music</button>
                    </div>`;
            }
            return;
        }

        const setEl = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = val; };
        setEl('song-title', track.title);
        setEl('song-artist', track.artist);
        setEl('song-album', track.album || '—');

        const coverEl = document.getElementById('song-cover');
        if (coverEl && track.cover) {
            coverEl.src = track.cover;
            coverEl.style.opacity = '1';
        }

        MusicRepository.getAverageColor(track.cover).then(color => {
            const hero = document.querySelector('.song-hero');
            if (hero) hero.style.setProperty('--song-color', color);
        });

        // Play action button
        const playBtn = document.getElementById('song-play-btn');
        if (playBtn) {
            playBtn.onclick = () => MediaPlayer.playTrack(track);
        }

        // Artist link
        const artistLink = document.getElementById('song-artist-link');
        if (artistLink) {
            artistLink.onclick = () => window.Router.loadPage('artist', { name: track.artist });
        }

        const getTranslatedTitle = (key, fallback, name) => {
            let text = LanguageManager.t(key);
            if (!text || text === key) text = fallback;
            return text.replace('{artist}', name);
        };

        const artistName = track.artist || 'Artist';

        try {
            const [albums, popularTracks] = await Promise.all([
                MusicRepository.search(artistName, 12, 'album', null, 0, false, this.signal).catch(() => []),
                MusicRepository.search(artistName, 12, 'song', null, 0, false, this.signal).catch(() => [])
            ]);

            if (this.signal?.aborted) return;

            const albumsRow = document.getElementById('song-albums-tracks');
            const albumsRowContainer = document.getElementById('song-albums-row');
            const albumsTitleEl = document.getElementById('song-albums-title');
            if (albumsRow && albums.length > 0) {
                if (albumsTitleEl) albumsTitleEl.textContent = getTranslatedTitle('albums_from_artist', `Albums from ${artistName}`, artistName);
                CardComponentFactory.renderCards(albumsRow, albums, 'album');
                if (albumsRowContainer) albumsRowContainer.classList.remove('is-hidden');
            }

            const popularRow = document.getElementById('song-popular-tracks');
            const popularRowContainer = document.getElementById('song-popular-row');
            const popularTitleEl = document.getElementById('song-popular-title');
            if (popularRow && popularTracks.length > 0) {
                if (popularTitleEl) popularTitleEl.textContent = getTranslatedTitle('popular_songs_from_artist', `Most popular songs from ${artistName}`, artistName);
                CardComponentFactory.renderCards(popularRow, popularTracks, 'song');
                if (popularRowContainer) popularRowContainer.classList.remove('is-hidden');
            }
        } catch (err) {
            console.error('[SongPageController] Render error:', err);
        }
    }
}

