import { BasePageController } from './BasePageController.js';
import { DownloadManager } from '../downloader/DownloadManager.js';

/**
 * DownloaderPageController handles standalone downloader view, URL parsing, and queue status.
 */
export class DownloaderPageController extends BasePageController {
    /**
     * Renders downloader view.
     * @param {Object} params
     */
    async render(params = {}) {
        this.resetAbortController();
        this.bindEvents();
    }

    /** Binds downloader input and button handlers */
    bindEvents() {
        const btn = document.getElementById('downloader-start-btn');
        const input = document.getElementById('downloader-url-input');
        if (btn && input) {
            btn.onclick = () => {
                const url = input.value.trim();
                if (url) {
                    DownloadManager.enqueueTrack({ videoId: url, title: 'Downloaded Track', artist: 'YouTube' });
                }
            };
        }
    }
}
