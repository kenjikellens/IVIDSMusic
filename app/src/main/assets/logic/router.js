export const Router = {
    currentPage: null,
    currentParams: null,
    abortController: null,

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
