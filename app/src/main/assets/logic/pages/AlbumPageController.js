import { BasePageController } from './BasePageController.js';
import { CardComponentFactory } from '../cards.js';

/**
 * AlbumPageController manages album detail views and tracklists.
 */
export class AlbumPageController extends BasePageController {
    /**
     * Renders album detail view.
     * @param {Object} params
     */
    async render(params = {}) {
        this.resetAbortController();
    }
}
