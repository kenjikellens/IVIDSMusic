import { BasePageController } from './BasePageController.js';
import { PageSystem } from '../pages.js';

/**
 * ArtistPageController manages artist details, top tracks, and albums.
 */
export class ArtistPageController extends BasePageController {
    /**
     * Renders detailed artist view.
     * @param {Object} params
     */
    async render(params = {}) {
        this.resetAbortController();
        await PageSystem.initArtist(params);
    }
}
