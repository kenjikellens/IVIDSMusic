import { BaseService } from './core/BaseService.js';
import { LRUCache } from './utils/LRUCache.js';
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
 * RouterService orchestrates SPA page transitions, template caching, and PageController lifecycle.
 */
export class RouterService extends BaseService {
    #currentPage = null;
    #currentParams = null;
    #templateCache = new LRUCache(30);
    #activeController = null;
    #controllers = new Map();

    constructor() {
        super();
        this.#registerControllers();
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
     * Dynamically loads a specified page into the main view, updating navigation state and executing PageController lifecycle.
     * @param {string} pageName
     * @param {Object} [params]
     */
    async loadPage(pageName, params = null) {
        if (this.#currentPage === pageName && JSON.stringify(this.#currentParams) === JSON.stringify(params)) {
            return;
        }

        const mainView = document.getElementById('main-view');
        if (!mainView) return;

        mainView.innerHTML = '<div class="page-loading-overlay"><div class="ivids-loader"></div></div>';

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

            document.querySelectorAll('.nav-links a').forEach(link => {
                link.classList.remove('active');
                if (link.id === `nav-${pageName}`) link.classList.add('active');
            });

            const controller = this.#controllers.get(pageName);
            if (controller) {
                this.#activeController = controller;
                controller.container = mainView;
                await controller.render(params || {});
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
