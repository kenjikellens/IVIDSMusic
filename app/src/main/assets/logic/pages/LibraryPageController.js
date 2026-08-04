import { BasePageController } from './BasePageController.js';
import { StorageFactory } from '../storage/StorageFactory.js';
import { CardComponentFactory } from '../cards.js';

/**
 * LibraryPageController manages user's saved tracks and playlists library.
 */
export class LibraryPageController extends BasePageController {
    /**
     * Renders saved tracks in library view.
     * @param {Object} params
     */
    async render(params = {}) {
        this.resetAbortController();
        try {
            const engine = StorageFactory.getEngine();
            const tracks = await engine.getSavedTracks();
            const container = document.getElementById('library-tracks-grid');
            if (container) {
                CardComponentFactory.renderCards(container, tracks, 'song');
            }
        } catch (err) {
            console.error('[LibraryPageController] Error rendering library:', err);
        }
    }
}
