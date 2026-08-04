import { BasePageController } from './BasePageController.js';
import { MusicRepository } from '../api/MusicRepository.js';
import { CardComponentFactory } from '../cards.js';

/**
 * RecommendedPageController handles personalized recommendation discovery.
 */
export class RecommendedPageController extends BasePageController {
    /**
     * Renders personalized recommendations.
     * @param {Object} params
     */
    async render(params = {}) {
        this.resetAbortController();
        const mainView = document.getElementById('main-view');
        if (!mainView) return;

        try {
            const tracks = await MusicRepository.getChart(30);
            const container = mainView.querySelector('#recommended-grid') || mainView;
            CardComponentFactory.renderCards(container, tracks, 'song');
        } catch (err) {
            console.error('[RecommendedPageController] Render error:', err);
        }
    }
}
