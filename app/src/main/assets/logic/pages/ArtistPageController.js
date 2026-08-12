import { BasePageController } from './BasePageController.js';
import { MusicRepository } from '../api/MusicRepository.js';
import { CardComponentFactory } from '../cards.js';
import { MediaPlayer } from '../player/MediaPlayer.js';
import { LanguageManager } from '../language-manager.js';


/**
 * ArtistPageController manages detailed artist view, top tracks, albums, and related artists.
 */
export class ArtistPageController extends BasePageController {
    /**
     * Renders detailed artist view.
     * @param {Object} params
     */
    async render(params = {}) {
        this.resetAbortController();

        const queryName = params.name || params.artist || params.title || '';
        if (!queryName && !params.id) return;

        const nameEl = document.getElementById('artist-name');
        if (nameEl && queryName) nameEl.textContent = queryName;

        try {
            let artist = null;
            if (queryName) {
                const results = await MusicRepository.search(queryName, 1, 'artist', null, 0, false, this.signal);
                if (results.length > 0) artist = results[0];
            }

            if (!artist) {
                artist = { id: params.id || 'artist-fallback', name: queryName || 'Artist', cover: 'svg/user.svg' };
            }

            if (this.signal?.aborted) return;

            const avatarEl = document.getElementById('artist-avatar');
            const avatarLoader = document.getElementById('artist-avatar-loader');
            const fansEl = document.getElementById('artist-fans');
            const albumsCountEl = document.getElementById('artist-albums-count');

            if (nameEl) nameEl.textContent = artist.name;
            if (fansEl) fansEl.textContent = artist.fanCount ? `${artist.fanCount.toLocaleString()} fans` : 'Artist';

            const avatarSrc = artist.cover || 'svg/user.svg';
            if (avatarEl) {
                avatarEl.onload = () => {
                    avatarEl.style.opacity = '1';
                    if (avatarLoader) avatarLoader.style.display = 'none';
                };
                avatarEl.src = avatarSrc;
            }

            MusicRepository.getAverageColor(avatarSrc).then(color => {
                const hero = document.querySelector('.artist-hero');
                if (hero) hero.style.setProperty('--artist-color', color);
            });

            // Helper for localized title string formatting
            const getTranslatedTitle = (key, fallback, name) => {
                let text = LanguageManager.t(key);
                if (!text || text === key) text = fallback;
                return text.replace('{artist}', name);
            };

            const artistName = artist.name || queryName;

            // Update row titles with formatted string
            const albumsTitleEl = document.getElementById('artist-albums-title');
            if (albumsTitleEl) albumsTitleEl.textContent = getTranslatedTitle('albums_from_artist', `Albums from ${artistName}`, artistName);

            const popularTitleEl = document.getElementById('artist-popular-title');
            if (popularTitleEl) popularTitleEl.textContent = getTranslatedTitle('popular_songs_from_artist', `Most popular songs from ${artistName}`, artistName);

            const trendingTitleEl = document.getElementById('artist-trending-title');
            if (trendingTitleEl) trendingTitleEl.textContent = getTranslatedTitle('trending_songs_from_artist', `Most trending songs from ${artistName}`, artistName);

            // Fetch Top Tracks and Albums in parallel
            const [topTracks, albums, relatedArtists] = await Promise.all([
                MusicRepository.search(artistName, 20, 'song', null, 0, false, this.signal).catch(() => []),
                MusicRepository.search(artistName, 20, 'album', null, 0, false, this.signal).catch(() => []),
                MusicRepository.search(artistName, 10, 'artist', null, 0, false, this.signal).catch(() => [])
            ]);


            if (this.signal?.aborted) return;

            if (albumsCountEl) albumsCountEl.textContent = `${albums.length || 0} albums`;

            // Wire up Play button
            const playBtn = document.getElementById('play-artist-btn');
            if (playBtn) {
                if (topTracks.length > 0) {
                    playBtn.disabled = false;
                    playBtn.onclick = () => MediaPlayer.playQueue(topTracks, 0);
                } else {
                    playBtn.disabled = true;
                }
            }

            // Wire up Shuffle button
            const shuffleBtn = document.getElementById('shuffle-artist-btn');
            if (shuffleBtn) {
                if (topTracks.length > 0) {
                    shuffleBtn.disabled = false;
                    shuffleBtn.onclick = () => {
                        const shuffled = [...topTracks].sort(() => Math.random() - 0.5);
                        MediaPlayer.playQueue(shuffled, 0);
                    };
                } else {
                    shuffleBtn.disabled = true;
                }
            }

            // Render Albums Row
            const albumsRow = document.getElementById('artist-albums');
            const albumsCont = document.getElementById('artist-albums-container');
            if (albumsRow && albumsCont) {
                if (albums.length > 0) {
                    CardComponentFactory.renderCards(albumsRow, albums, 'album');
                    albumsCont.classList.remove('is-hidden');
                } else {
                    albumsCont.classList.add('is-hidden');
                }
            }

            // Render Most Popular Songs Row
            const popularTracks = topTracks.slice(0, 10);
            const popularRow = document.getElementById('artist-popular-tracks');
            const popularCont = document.getElementById('artist-popular-container');
            if (popularRow && popularCont) {
                if (popularTracks.length > 0) {
                    CardComponentFactory.renderCards(popularRow, popularTracks, 'song');
                    popularCont.classList.remove('is-hidden');
                } else {
                    popularCont.classList.add('is-hidden');
                }
            }

            // Render Related Artists
            const relatedRow = document.getElementById('artist-related');
            const relatedCont = document.getElementById('artist-related-container');
            if (relatedRow && relatedCont) {
                const filtered = relatedArtists.filter(a => a.name !== artist.name);
                if (filtered.length > 0) {
                    CardComponentFactory.renderCards(relatedRow, filtered, 'artist');
                    relatedCont.classList.remove('is-hidden');
                } else {
                    relatedCont.classList.add('is-hidden');
                }
            }
        } catch (err) {
            console.error('[ArtistPageController] Render error:', err);
        }
    }
}
