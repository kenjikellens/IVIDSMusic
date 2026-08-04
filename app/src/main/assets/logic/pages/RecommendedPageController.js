import { BasePageController } from './BasePageController.js';

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
            if (window.DiscoveryEngine && window.DiscoveryEngine.initRecommended) {
                await window.DiscoveryEngine.initRecommended(params);
            } else {
                const { DiscoveryEngine } = await import('../recommendations.js');
                await DiscoveryEngine.initRecommended(params);
            }
        } catch (err) {
            console.error('[RecommendedPageController] Render error:', err);
        }
    }
}
