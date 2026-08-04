import { BasePageController } from './BasePageController.js';
import { MusicRepository } from '../api/MusicRepository.js';
import { CardComponentFactory } from '../cards.js';

/**
 * HomePageController manages the Home view categories and top charts.
 */
export class HomePageController extends BasePageController {
    /**
     * Renders Home page carousels and top hits.
     * @param {Object} params
     */
    async render(params = {}) {
        this.resetAbortController();
        const mainView = document.getElementById('main-view');
        if (!mainView) return;

        try {
            const chartTracks = await MusicRepository.getChart(20);
            const container = mainView.querySelector('#home-top-hits-grid') || mainView;
            CardComponentFactory.renderCards(container, chartTracks, 'song');
        } catch (err) {
            console.error('[HomePageController] Render error:', err);
        }
    }
}
