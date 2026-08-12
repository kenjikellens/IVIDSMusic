import { BasePageController } from './BasePageController.js';
import { MusicRepository } from '../api/MusicRepository.js';
import { CardComponentFactory } from '../cards.js';
import { MediaPlayer } from '../player/MediaPlayer.js';

/**
 * AlbumPageController manages album detail views, tracklists, and album play actions.
 */
export class AlbumPageController extends BasePageController {
    /**
     * Renders album detail view.
     * @param {Object} params
     */
    async render(params = {}) {
        this.resetAbortController();
        if (!params || !params.id) return;

        try {
            const albumResults = await MusicRepository.search(params.id, 1, 'album', null, 0, false, this.signal);
            if (this.signal?.aborted) return;

            const album = albumResults.length > 0 ? albumResults[0] : null;

            const titleEl = document.getElementById('album-title');
            const artistEl = document.getElementById('album-artist');
            const coverEl = document.getElementById('album-cover');
            const coverLoader = document.getElementById('album-cover-loader');
            const countEl = document.getElementById('album-tracks-count');

            if (album) {
                if (titleEl) titleEl.textContent = album.title;
                if (artistEl) {
                    artistEl.textContent = album.artist;
                    artistEl.onclick = () => window.Router.loadPage('artist', { name: album.artist });
                }
                if (countEl) countEl.textContent = `${album.trackCount || 0} tracks`;

                if (coverEl && album.cover) {
                    coverEl.onload = () => {
                        coverEl.style.opacity = '1';
                        if (coverLoader) coverLoader.style.display = 'none';
                    };
                    coverEl.src = album.cover;
                }

                MusicRepository.getAverageColor(album.cover).then(color => {
                    const hero = document.querySelector('.album-hero');
                    if (hero) hero.style.setProperty('--primary-color', color);
                });
            }

            // Fetch tracks belonging to album / artist
            const query = album ? `${album.title} ${album.artist}` : params.id;
            const tracks = await MusicRepository.search(query, 25, 'song', null, 0, false, this.signal);
            if (this.signal?.aborted) return;

            // Wire up Play Album Button
            const playBtn = document.getElementById('play-album-btn');
            if (playBtn && tracks.length > 0) {
                playBtn.disabled = false;
                playBtn.onclick = () => MediaPlayer.playQueue(tracks, 0);
            }

            // Render Tracklist with container delegation
            const tracklistCont = document.getElementById('album-tracklist');
            if (tracklistCont) {
                tracklistCont.innerHTML = '';
                const fragment = document.createDocumentFragment();

                tracks.forEach((track, index) => {
                    const trackRow = document.createElement('div');
                    trackRow.className = 'album-track-row container-hover-effect';
                    trackRow.tabIndex = 0;
                    trackRow.dataset.trackIndex = index;
                    trackRow.innerHTML = `
                        <div class="track-number">${index + 1}</div>
                        <div class="track-details">
                            <div class="track-title">${track.title}</div>
                            <div class="track-artist">${track.artist}</div>
                        </div>
                        <div class="track-duration">${track.formattedDuration || '--:--'}</div>
                    `;
                    fragment.appendChild(trackRow);
                });

                tracklistCont.appendChild(fragment);

                this.bindContainerEvent(tracklistCont, 'click', '.album-track-row', (rowEl) => {
                    const index = Number(rowEl.dataset.trackIndex);
                    if (!isNaN(index) && tracks[index]) {
                        MediaPlayer.playQueue(tracks, index);
                    }
                });
            }
        } catch (err) {
            console.error('[AlbumPageController] Render error:', err);
        }
    }
}
