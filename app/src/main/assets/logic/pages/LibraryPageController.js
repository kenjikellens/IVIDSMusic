import { BasePageController } from './BasePageController.js';
import { StorageFactory } from '../storage/StorageFactory.js';
import { CardComponentFactory } from '../cards.js';
import { MediaPlayer } from '../player/MediaPlayer.js';

/**
 * LibraryPageController manages user's saved tracks and playlists library.
 */
export class LibraryPageController extends BasePageController {
    /**
     * Renders saved tracks and playlists in library view.
     * @param {Object} params
     */
    async render(params = {}) {
        this.resetAbortController();

        try {
            const engine = StorageFactory.getEngine();
            const tracks = await engine.getSavedTracks();

            if (this.signal?.aborted) return;

            const container = document.getElementById('library-tracks-grid') || document.getElementById('library-list-container');
            const emptyState = document.getElementById('library-empty-state');

            if (container) {
                if (!tracks || tracks.length === 0) {
                    if (emptyState) emptyState.classList.remove('is-hidden');
                    container.classList.add('is-hidden');
                } else {
                    if (emptyState) emptyState.classList.add('is-hidden');
                    container.classList.remove('is-hidden');
                    CardComponentFactory.renderCards(container, tracks, 'song');

                    this.bindContainerEvent(container, 'click', '.music-card', (card) => {
                        const trackJson = card.dataset.trackJson;
                        if (trackJson) {
                            try {
                                const track = JSON.parse(trackJson);
                                MediaPlayer.playTrack(track);
                            } catch (e) {}
                        }
                    });
                }
            }
        } catch (err) {
            console.error('[LibraryPageController] Error rendering library:', err);
        }
    }
}
