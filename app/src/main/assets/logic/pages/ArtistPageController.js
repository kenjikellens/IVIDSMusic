import { BasePageController } from './BasePageController.js';
import { MusicRepository } from '../api/MusicRepository.js';
import { CardComponentFactory } from '../cards.js';

/**
 * ArtistPageController manages artist details, top tracks, and albums.
 */
export class ArtistPageController extends BasePageController {
    /**
     * Renders artist profile view.
     * @param {Object} params
     */
    async render(params = {}) {
        this.resetAbortController();
        const { id, name } = params;
        if (!id && !name) return;

        const mainView = document.getElementById('main-view');
        if (!mainView) return;

        try {
            const tracks = await MusicRepository.search(name || '', 10, 'song');
            const container = mainView.querySelector('#artist-top-tracks') || mainView;
            CardComponentFactory.renderCards(container, tracks, 'song');
        } catch (err) {
            console.error('[ArtistPageController] Render error:', err);
        }
    }
}
