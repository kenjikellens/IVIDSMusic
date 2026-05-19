export const Router = {
    currentPage: null,
    currentParams: null,
    abortController: null,

    /**
     * Dynamically loads a specified page into the main view, handles query/history attributes,
     * updates active sidebar/navigation elements, and dynamically manages localized header titles.
     * @param {string} pageName The name of the page layout file to retrieve.
     * @param {Object} [params=null] Optional routing parameters containing page-specific state.
     */
    async loadPage(pageName, params = null) {
        // Allow re-loading the same page if parameters changed
        if (this.currentPage === pageName && JSON.stringify(this.currentParams) === JSON.stringify(params)) {
            return;
        }

        const mainView = document.getElementById('main-view');
        if (!mainView) return;

        // 1. Instant Visual Clear & Abort previous requests
        mainView.innerHTML = '<div class="page-loading-overlay"><div class="ivids-loader"></div></div>';
        if (this.abortController) {
            this.abortController.abort();
            console.log(`[Router] Aborted requests from: ${this.currentPage}`);
        }
        this.abortController = new AbortController();

        try {
            console.log(`[Router] Loading: ${pageName}`, params);
            const response = await fetch(`pages/${pageName}.html?v=${Date.now()}`, { signal: this.abortController.signal });
            if (!response.ok) throw new Error(`Could not load page: ${pageName}`);

            const html = await response.text();

            this.currentParams = params;
            this.currentPage = pageName;

            // Set current page and active query status as body attributes for clean responsive styling
            document.body.setAttribute('data-current-page', pageName);
            if (pageName === 'search' && params && params.query && params.query.trim()) {
                document.body.setAttribute('data-search-has-query', 'true');
            } else {
                document.body.removeAttribute('data-search-has-query');
            }

            // Parse HTML safely
            const temp = document.createElement('div');
            temp.innerHTML = html;

            // Extract scripts to run them manually
            const scripts = Array.from(temp.querySelectorAll('script'));
            scripts.forEach(s => s.remove());

            // Clear view and inject new HTML
            mainView.innerHTML = temp.innerHTML;

            // Apply translations to the new content
            if (window.LanguageManager) {
                window.LanguageManager.translateUI(mainView);
            }

            // Update header title dynamically
            const headerTitleEl = document.getElementById('header-page-title');
            if (headerTitleEl) {
                const pageTitleKeys = {
                    'home': 'home',
                    'recommended': 'nav_foryou',
                    'search': 'search',
                    'library': 'library',
                    'profile': 'you',
                    'settings': 'settings',
                    'song': 'song'
                };
                const key = pageTitleKeys[pageName];
                if (key && window.LanguageManager && window.LanguageManager.translations[key]) {
                    headerTitleEl.textContent = window.LanguageManager.translations[key];
                } else if (params && (params.name || params.title)) {
                    headerTitleEl.textContent = params.name || params.title;
                } else {
                    headerTitleEl.textContent = pageName.charAt(0).toUpperCase() + pageName.slice(1);
                }
            }

            // Clean up old dynamic scripts
            document.querySelectorAll('.dynamic-script').forEach(s => s.remove());

            // Update sidebar active state immediately for instant feedback
            document.querySelectorAll('.nav-links a').forEach(link => {
                link.classList.remove('active');
                if (link.id === `nav-${pageName}`) link.classList.add('active');
            });

            // Initialize Page Logic via PageSystem (NO JS IN HTML)
            if (pageName === 'recommended') {
                const { DiscoveryEngine } = await import('./recommendations.js');
                if (DiscoveryEngine.initRecommended) {
                    await DiscoveryEngine.initRecommended(params || {});
                }
            } else {
                const { PageSystem } = await import('./pages.js');
                const initMethod = `init${pageName.charAt(0).toUpperCase() + pageName.slice(1)}`;
                if (PageSystem[initMethod]) {
                    await PageSystem[initMethod](params || {});
                }
            }

            mainView.scrollTop = 0;

            if (window.TVNav) {
                setTimeout(() => window.TVNav.reinitFocus(), 100);
            }

        } catch (error) {
            console.error('[Router] Error:', error);
            mainView.innerHTML = `<div style="padding: 40px;"><h1>Error</h1><p>${error.message}</p></div>`;
        }
    }
};

window.Router = Router;
