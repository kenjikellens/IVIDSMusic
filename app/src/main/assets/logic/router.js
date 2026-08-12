import { BaseService } from './core/BaseService.js';
import { LanguageManager } from './language-manager.js';
import { HomePageController } from './pages/HomePageController.js';
import { SearchPageController } from './pages/SearchPageController.js';
import { RecommendedPageController } from './pages/RecommendedPageController.js';
import { ArtistPageController } from './pages/ArtistPageController.js';
import { AlbumPageController } from './pages/AlbumPageController.js';
import { SongPageController } from './pages/SongPageController.js';
import { LibraryPageController } from './pages/LibraryPageController.js';
import { SettingsPageController } from './pages/SettingsPageController.js';
import { DownloaderPageController } from './pages/DownloaderPageController.js';

/**
 * RouterService orchestrates SPA page transitions and PageController lifecycle.
 * Features in-memory HTML template caching and idle route prewarming for 0ms latency transitions.
 */
export class RouterService extends BaseService {
    #currentPage = null;
    #currentParams = null;
    #activeController = null;
    #controllers = new Map();
    #templateCache = new Map();

    constructor() {
        super();
        this.#registerControllers();
        this.prewarmTemplates();
    }

    #registerControllers() {
        this.#controllers.set('home', new HomePageController());
        this.#controllers.set('search', new SearchPageController());
        this.#controllers.set('recommended', new RecommendedPageController());
        this.#controllers.set('artist', new ArtistPageController());
        this.#controllers.set('album', new AlbumPageController());
        this.#controllers.set('song', new SongPageController());
        this.#controllers.set('library', new LibraryPageController());
        this.#controllers.set('settings', new SettingsPageController());
        this.#controllers.set('downloader', new DownloaderPageController());
    }

    /** Returns currently loaded page name */
    get currentPage() { return this.#currentPage; }

    /** Returns active routing parameters */
    get currentParams() { return this.#currentParams; }

    /**
     * Prewarms static HTML templates during browser idle periods.
     */
    prewarmTemplates() {
        const pages = ['home', 'search', 'recommended', 'library', 'downloader', 'settings', 'artist', 'album', 'song'];
        const idleCallback = typeof window !== 'undefined' && window.requestIdleCallback ? window.requestIdleCallback : (fn) => setTimeout(fn, 1000);

        idleCallback(() => {
            pages.forEach(async (page) => {
                if (!this.#templateCache.has(page)) {
                    try {
                        const res = await fetch(`pages/${page}.html`);
                        if (res.ok) {
                            const html = await res.text();
                            this.#templateCache.set(page, html);
                        }
                    } catch (e) {
                        // Silent prewarm catch
                    }
                }
            });
        });
    }

    /**
     * Prefetches and caches specific page HTML template.
     * @param {string} pageName
     */
    async prefetchPage(pageName) {
        if (this.#templateCache.has(pageName)) return;
        try {
            const response = await fetch(`pages/${pageName}.html`);
            if (response.ok) {
                const html = await response.text();
                this.#templateCache.set(pageName, html);
            }
        } catch (e) {}
    }

    /**
     * Backward compatibility stub for page prefetching.
     */
    async prefetchAllPages() {
        this.prewarmTemplates();
        return Promise.resolve();
    }

    /**
     * Dynamically loads a specified page into the main view with zero latency.
     * @param {string} pageName
     * @param {Object} [params]
     */
    async loadPage(pageName, params = null) {
        if (this.#currentPage === pageName && JSON.stringify(this.#currentParams) === JSON.stringify(params)) {
            return;
        }

        const mainView = document.getElementById('main-view');
        if (!mainView) return;

        if (this.#activeController) {
            this.#activeController.destroy();
            this.#activeController = null;
        }

        try {
            let html = this.#templateCache.get(pageName);
            if (!html) {
                const response = await fetch(`pages/${pageName}.html`);
                if (!response.ok) throw new Error(`Could not load page: ${pageName}`);
                html = await response.text();
                this.#templateCache.set(pageName, html);
            }

            this.#currentPage = pageName;
            this.#currentParams = params;
            document.body.setAttribute('data-current-page', pageName);

            const temp = document.createElement('div');
            temp.innerHTML = html;
            temp.querySelectorAll('script').forEach(s => s.remove());

            mainView.innerHTML = temp.innerHTML;
            LanguageManager.translateUI(mainView);
            if (window.Loader) window.Loader.init();

            document.querySelectorAll('.nav-links a, .bottom-nav a').forEach(link => {
                link.classList.remove('active');
                if (link.id === `nav-${pageName}` || link.id === `mobile-nav-${pageName}`) {
                    link.classList.add('active');
                }
            });

            const controller = this.#controllers.get(pageName);
            if (controller) {
                this.#activeController = controller;
                controller.container = mainView;
                controller.render(params || {}).catch(err => console.error('[RouterService] Render error:', err));
            }

            mainView.scrollTop = 0;
            this.emit('pageLoaded', { pageName, params });
        } catch (error) {
            console.error('[RouterService] Navigation error:', error);
            mainView.innerHTML = `<div style="padding: 40px;"><h1>Error</h1><p>${error.message}</p></div>`;
        }
    }
}

/** Router singleton instance */
export const Router = new RouterService();

if (typeof window !== 'undefined') {
    window.Router = Router;
}

