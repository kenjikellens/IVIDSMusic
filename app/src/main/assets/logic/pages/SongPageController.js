import { BasePageController } from './BasePageController.js';
import { PageSystem } from '../pages.js';

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
        await PageSystem.initSong(params);
    }
}
