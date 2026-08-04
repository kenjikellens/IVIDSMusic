import { BasePageController } from './BasePageController.js';
import { MusicRepository } from '../api/MusicRepository.js';
import { CardComponentFactory } from '../cards.js';
import { Debouncer } from '../utils/Debouncer.js';

/**
 * SearchPageController manages realtime search inputs, filters, and result grids.
 */
export class SearchPageController extends BasePageController {
    #debouncedSearch = null;

    constructor() {
        super();
        this.#debouncedSearch = Debouncer.debounce((query) => this.performSearch(query), 350);
    }

    /**
     * Renders Search view and binds input listener.
     * @param {Object} params
     */
    async render(params = {}) {
        this.resetAbortController();
        this.bindEvents();
        if (params.query) {
            await this.performSearch(params.query);
        }
    }

    /**
     * Performs search query via MusicRepository.
     * @param {string} query
     */
    async performSearch(query) {
        if (!query || !query.trim()) return;
        this.resetAbortController();
        const results = await MusicRepository.search(query, 20, 'all', null, 0, false, this.signal);
        const container = document.getElementById('search-results-container');
        if (container) {
            CardComponentFactory.renderCards(container, results, 'song');
        }
    }

    /**
     * Binds input search bar events.
     */
    bindEvents() {
        const input = document.getElementById('search-input');
        if (input) {
            input.oninput = (e) => this.#debouncedSearch(e.target.value);
        }
    }
}
