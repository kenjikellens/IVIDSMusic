import { Config } from './config.js';
import { LanguageManager } from './language-manager.js';
import { Router } from './router.js';
import { MediaPlayer } from './player/MediaPlayer.js';
import { DownloadManager } from './downloader/DownloadManager.js';
import { TVNav } from './tv-nav.js';

/**
 * Application Bootstrap class that orchestrates service initialization and app lifecycle.
 */
export class Application {
    /**
     * Initializes all core services and boots the SPA application interface with zero latency.
     */
    static async bootstrap() {
        console.log('[IVIDS Music] Bootstrapping OOP Application...');
        try {
            document.body.setAttribute('data-current-page', 'home');

            // Trigger home controller rendering immediately so loaders are instantly active
            Router.loadPage('home').catch(e => console.error(e));

            // Initialize background services concurrently
            Promise.all([
                LanguageManager.init(),
                MediaPlayer.init(),
                DownloadManager.init(),
                TVNav.init()
            ]).catch(err => console.error('[IVIDS Music] Service init warning:', err));

            // Enable horizontal mouse wheel scrolling for all .scroll-row containers
            document.addEventListener('wheel', (e) => {
                const scrollRow = e.target.closest('.scroll-row');
                if (!scrollRow) return;
                if (e.deltaY !== 0 && scrollRow.scrollWidth > scrollRow.clientWidth) {
                    e.preventDefault();
                    scrollRow.scrollLeft += e.deltaY;
                }
            }, { passive: false });

            console.log('[IVIDS Music] Application booted successfully.');
        } catch (err) {
            console.error('[IVIDS Music] Application bootstrap error:', err);
        }
    }
}

// Auto-bootstrap on DOMContentLoaded if running in browser
if (typeof window !== 'undefined') {
    window.addEventListener('DOMContentLoaded', () => Application.bootstrap());
    window.App = Application;
}
