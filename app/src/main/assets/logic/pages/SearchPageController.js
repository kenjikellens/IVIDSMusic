import { BasePageController } from './BasePageController.js';
import { MusicRepository } from '../api/MusicRepository.js';
import { CardComponentFactory } from '../cards.js';
import { Debouncer } from '../utils/Debouncer.js';
import { LanguageManager } from '../language-manager.js';

/**
 * SearchPageController manages realtime search inputs, filter chips, year filters,
 * load-more pagination, and dynamic result grid/row rendering.
 */
export class SearchPageController extends BasePageController {
    #debouncedSearch = null;
    #currentQuery = '';
    #currentType = null;
    #currentYear = null;
    #currentOffset = 0;
    #pageSize = 24;
    #isLoadingMore = false;

    /**
     * Constructs SearchPageController and initializes debounced search callback.
     */
    constructor() {
        super();
        this.#debouncedSearch = Debouncer.debounce((query) => this.performSearch(query, this.#currentType, this.#currentYear), 350);
    }

    /**
     * Renders Search view, exposes window bindings, and executes initial search if query parameter exists.
     * @param {Object} params - Routing parameters containing query, type, or year.
     */
    async render(params = {}) {
        this.resetAbortController();
        this.#setupWindowBindings();
        this.bindEvents(params);

        const query = (params.query || '').trim();
        const type = params.type || null;
        const year = params.year || null;

        if (query) {
            await this.performSearch(query, type, year);
        } else {
            this.showBrowseState();
        }
    }

    /**
     * Exposes global window function hooks required by search.html inline event handlers.
     */
    #setupWindowBindings() {
        window.performSearch = (query, type, year) => {
            if (window.Router && window.Router.currentPage !== 'search') {
                window.Router.loadPage('search', { query, type, year });
            } else {
                this.performSearch(query, type ?? this.#currentType, year ?? this.#currentYear);
            }
        };

        window.setSearchFilter = (type) => this.setSearchFilter(type);
        window.applyYearFilter = () => this.applyYearFilter();
        window.loadMoreResults = () => this.loadMoreResults();
    }

    /**
     * Removes global window function bindings when leaving the search page.
     */
    #removeWindowBindings() {
        delete window.performSearch;
        delete window.setSearchFilter;
        delete window.applyYearFilter;
        delete window.loadMoreResults;
    }

    /**
     * Shows initial browse hero state and resets UI elements when search query is empty.
     */
    showBrowseState() {
        this.#currentQuery = '';
        this.#currentType = null;
        this.#currentYear = null;
        this.#currentOffset = 0;

        const get = id => document.getElementById(id);
        const browseHero = get('browse-hero');
        const resultsHeader = get('results-header');
        const rowsCont = get('results-rows-container');
        const gridView = get('single-category-grid');
        const searchInput = get('browse-search-input');
        const clearBtn = get('search-clear-btn');

        if (browseHero) browseHero.classList.remove('is-hidden');
        if (resultsHeader) resultsHeader.classList.add('is-hidden');
        if (rowsCont) rowsCont.classList.add('is-hidden');
        if (gridView) gridView.classList.add('is-hidden');
        if (searchInput) searchInput.value = '';
        if (clearBtn) clearBtn.style.display = 'none';
    }

    /**
     * Executes music search query via MusicRepository and updates DOM views.
     * @param {string} query - Search term input.
     * @param {string|null} [type=null] - Category filter type (null for all rows, 'all'/'artist'/'album' for grid).
     * @param {string|number|null} [year=null] - Optional release year filter.
     * @param {boolean} [append=false] - Whether to append results for pagination.
     */
    async performSearch(query, type = null, year = null, append = false) {
        const trimmedQuery = (query || '').trim();
        if (!trimmedQuery) {
            this.showBrowseState();
            return;
        }

        this.#currentQuery = trimmedQuery;
        this.#currentType = type;
        this.#currentYear = year;
        if (!append) this.#currentOffset = 0;

        const get = id => document.getElementById(id);
        const browseHero = get('browse-hero');
        const resultsHeader = get('results-header');
        const rowsCont = get('results-rows-container');
        const gridView = get('single-category-grid');

        if (browseHero) browseHero.classList.add('is-hidden');
        if (resultsHeader) resultsHeader.classList.remove('is-hidden');

        const resultsFor = LanguageManager.t('results_for') || 'Results for';
        if (get('search-subtitle')) {
            get('search-subtitle').textContent = `${resultsFor} "${trimmedQuery}"${year ? ` (${year})` : ''}`;
        }

        // Single category grid view
        if (type) {
            if (rowsCont) rowsCont.classList.add('is-hidden');
            if (gridView) gridView.classList.remove('is-hidden');

            const typeLabelKey = type === 'artist' ? 'artists' : (type === 'album' ? 'albums' : 'songs');
            const categoryTitle = LanguageManager.t(typeLabelKey) || (type.charAt(0).toUpperCase() + type.slice(1) + "s");
            if (get('grid-title')) get('grid-title').textContent = categoryTitle;

            const grid = get('grid-results');
            if (grid && !append) {
                grid.innerHTML = '';
                const skeletonRow = CardComponentFactory.createSkeletonRow('', '', 12);
                grid.appendChild(skeletonRow);
            }

            const data = await MusicRepository.search(trimmedQuery, this.#pageSize, type, year, this.#currentOffset, false, this.signal);
            if (this.signal?.aborted) return;

            if (grid) {
                if (!append) grid.innerHTML = '';
                if (append) {
                    const fragment = document.createDocumentFragment();
                    data.forEach(item => fragment.appendChild(CardComponentFactory.createCard(item)));
                    grid.appendChild(fragment);
                } else {
                    CardComponentFactory.renderCards(grid, data, type);
                }
            }

            // Update load more button visibility
            const loadMoreContainer = get('load-more-container');
            if (loadMoreContainer) {
                loadMoreContainer.classList.toggle('is-hidden', data.length < this.#pageSize);
            }
        } else { // Overview multi-row view (Artists, Songs, Albums)
            if (rowsCont) rowsCont.classList.remove('is-hidden');
            if (gridView) gridView.classList.add('is-hidden');

            const showSkeleton = (id, containerId) => {
                const parent = get(containerId);
                const container = get(id);
                if (parent) parent.classList.remove('is-hidden');
                if (container) {
                    container.innerHTML = '';
                    for (let i = 0; i < 6; i++) {
                        const skel = document.createElement('div');
                        skel.className = 'music-card card-placeholder container-hover-effect';
                        skel.innerHTML = '<div class="card-image-box"><div class="ivids-loader"><img src="svg/loader.svg" alt="Loading" class="ivids-loader-img"></div></div><div class="card-info-box"><div class="card-title"></div><div class="card-artist"></div></div>';
                        container.appendChild(skel);
                    }
                }
            };

            showSkeleton('artists-results', 'artists-row-container');
            showSkeleton('songs-results', 'songs-row-container');
            showSkeleton('albums-results', 'albums-row-container');

            const [artists, songs, albums] = await Promise.all([
                MusicRepository.search(trimmedQuery, 12, 'artist', year, 0, false, this.signal).catch(() => []),
                MusicRepository.search(trimmedQuery, 12, 'song', year, 0, false, this.signal).catch(() => []),
                MusicRepository.search(trimmedQuery, 12, 'album', year, 0, false, this.signal).catch(() => [])
            ]);

            if (this.signal?.aborted) return;

            const fill = (id, items, containerId, defaultType) => {
                const container = get(id);
                if (!container) return;
                CardComponentFactory.renderCards(container, items, defaultType);
                const parent = get(containerId);
                if (parent) {
                    if (items && items.length > 0) parent.classList.remove('is-hidden');
                    else parent.classList.add('is-hidden');
                }
            };

            fill('artists-results', artists, 'artists-row-container', 'artist');
            fill('songs-results', songs, 'songs-row-container', 'song');
            fill('albums-results', albums, 'albums-row-container', 'album');
        }
    }

    /**
     * Changes search filter type and executes fresh search query.
     * @param {string|null} type - Filter category type.
     */
    async setSearchFilter(type) {
        if (!this.#currentQuery) return;
        await this.performSearch(this.#currentQuery, type, this.#currentYear);
    }

    /**
     * Reads year filter input value and refreshes active search results.
     */
    async applyYearFilter() {
        const yearInput = document.getElementById('year-input');
        const yearVal = yearInput ? yearInput.value.trim() : null;
        if (!this.#currentQuery) return;
        await this.performSearch(this.#currentQuery, this.#currentType, yearVal || null);
    }

    /**
     * Loads next page of category grid results for pagination.
     */
    async loadMoreResults() {
        if (!this.#currentQuery || !this.#currentType || this.#isLoadingMore) return;

        this.#isLoadingMore = true;
        this.#currentOffset += this.#pageSize;
        try {
            await this.performSearch(this.#currentQuery, this.#currentType, this.#currentYear, true);
        } finally {
            this.#isLoadingMore = false;
        }
    }

    /**
     * Binds input search bar listeners, enter key submit, and clear button handlers.
     * @param {Object} params - Route parameters.
     */
    bindEvents(params) {
        const get = id => document.getElementById(id);
        const searchInput = get('browse-search-input');
        const clearBtn = get('search-clear-btn');
        const headerSearchInput = get('header-search-input');

        const initialQuery = params.query || '';

        if (searchInput) {
            searchInput.value = initialQuery;
            searchInput.oninput = (e) => {
                const val = e.target.value;
                if (clearBtn) clearBtn.style.display = val.trim() ? 'flex' : 'none';
                if (headerSearchInput) headerSearchInput.value = val;
                this.#debouncedSearch(val);
            };
        }

        if (headerSearchInput && initialQuery) {
            headerSearchInput.value = initialQuery;
        }

        if (clearBtn) {
            clearBtn.style.display = initialQuery.trim() ? 'flex' : 'none';
            clearBtn.onclick = () => {
                if (searchInput) {
                    searchInput.value = '';
                    searchInput.focus();
                }
                if (headerSearchInput) headerSearchInput.value = '';
                clearBtn.style.display = 'none';
                this.showBrowseState();
            };
        }
    }

    /**
     * Cleans up page controller event listeners, active requests, and window bindings.
     */
    destroy() {
        this.#removeWindowBindings();
        super.destroy();
    }
}
