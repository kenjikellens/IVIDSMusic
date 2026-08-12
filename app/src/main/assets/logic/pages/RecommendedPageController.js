import { BasePageController } from './BasePageController.js';
import { MusicRepository } from '../api/MusicRepository.js';
import { CardComponentFactory } from '../cards.js';

/**
 * RecommendedPageController handles personalized recommendation discovery.
 */
export class RecommendedPageController extends BasePageController {
    /**
     * Renders personalized recommendation discovery rows.
     * @param {Object} params - Route parameters
     */
    async render(params = {}) {
        this.resetAbortController();

        try {
            const container = document.getElementById('recommended-rows-container') || this.container;
            if (!container) return;

            const categories = await MusicRepository.getRecommendations(this.signal);
            if (this.signal?.aborted) return;

            container.innerHTML = '';
            const fragment = document.createDocumentFragment();

            categories.forEach(category => {
                const row = CardComponentFactory.createRow(category.title, `recommended-${category.id}`, category.tracks, 'song');
                fragment.appendChild(row);
            });

            container.appendChild(fragment);

            this.bindContainerEvent(container, 'click', '.music-card', (card, e) => {
                const playBtn = e.target.closest('.card-play-btn');
                const trackJson = card.dataset.trackJson;
                if (!trackJson) return;

                try {
                    const track = JSON.parse(trackJson);
                    if (playBtn) {
                        e.preventDefault();
                        e.stopPropagation();
                        if (window.MediaPlayer) window.MediaPlayer.playTrack(track);
                    } else if (window.Router) {
                        window.Router.loadPage('song', { id: track.id, track });
                    }
                } catch (err) {}
            });
        } catch (err) {
            console.error('[RecommendedPageController] Render error:', err);
        }
    }
}
