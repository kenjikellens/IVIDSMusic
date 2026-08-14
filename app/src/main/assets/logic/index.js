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

            // Initialize background services
            await LanguageManager.init().catch(err => console.error(err));
            Promise.all([
                MediaPlayer.init(),
                DownloadManager.init(),
                TVNav.init()
            ]).catch(err => console.error('[IVIDS Music] Service init warning:', err));

            // Trigger initial route rendering
            Router.loadPage('home').catch(e => console.error(e));

            // Enable horizontal mouse wheel and drag scrolling for all .scroll-row containers
            let isDragging = false;
            let startX = 0;
            let scrollLeftVal = 0;
            let activeRow = null;
            let hasMoved = false;

            document.addEventListener('mousedown', (e) => {
                const scrollRow = e.target.closest('.scroll-row');
                if (!scrollRow) return;
                isDragging = true;
                hasMoved = false;
                activeRow = scrollRow;
                startX = e.clientX;
                scrollLeftVal = scrollRow.scrollLeft;
            });

            window.addEventListener('mouseup', () => {
                isDragging = false;
                activeRow = null;
            });

            document.addEventListener('mousemove', (e) => {
                if (!isDragging || !activeRow) return;
                const dx = e.clientX - startX;
                if (Math.abs(dx) > 3) {
                    hasMoved = true;
                    e.preventDefault();
                    activeRow.scrollLeft = scrollLeftVal - (dx * 1.3);
                }
            });

            document.addEventListener('click', (e) => {
                if (hasMoved && e.target.closest('.scroll-row')) {
                    e.preventDefault();
                    e.stopPropagation();
                    hasMoved = false;
                }
            }, true);

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
