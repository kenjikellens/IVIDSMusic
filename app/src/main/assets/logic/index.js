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
     * Initializes all core services and boots the SPA application interface.
     */
    static async bootstrap() {
        console.log('[IVIDS Music] Bootstrapping OOP Application...');
        try {
            await LanguageManager.init();
            await MediaPlayer.init();
            await DownloadManager.init();
            await TVNav.init();

            // Load initial home route
            await Router.loadPage('home');
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
