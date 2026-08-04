import { BasePageController } from './BasePageController.js';
import { MediaPlayer } from '../player/MediaPlayer.js';

/**
 * SongPageController manages full-screen track player view.
 */
export class SongPageController extends BasePageController {
    /**
     * Renders detailed song view.
     * @param {Object} params
     */
    async render(params = {}) {
        this.resetAbortController();
        const currentTrack = MediaPlayer.currentTrack;
        if (currentTrack) {
            const titleEl = document.getElementById('song-page-title');
            const artistEl = document.getElementById('song-page-artist');
            const coverEl = document.getElementById('song-page-cover');
            if (titleEl) titleEl.textContent = currentTrack.title || '';
            if (artistEl) artistEl.textContent = currentTrack.artist || '';
            if (coverEl) coverEl.src = currentTrack.cover || 'gui/gemini-logo.png';
        }
    }
}
