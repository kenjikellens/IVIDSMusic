import { BaseService } from '../core/BaseService.js';
import { Config } from '../core/Config.js';
import { WebDownloader } from './WebDownloader.js';
import { AndroidDownloader } from './AndroidDownloader.js';
import { ElectronDownloader } from './ElectronDownloader.js';

/**
 * DownloadManagerService orchestrates multi-platform download operations and queue state.
 */
export class DownloadManagerService extends BaseService {
    #activeDownloader = null;
    #queue = [];
    #isProcessing = false;

    /**
     * Initializes DownloadManagerService and selects platform download strategy.
     */
    async init() {
        if (this.isInitialized) return;

        if (Config.isNative) {
            this.#activeDownloader = new AndroidDownloader();
        } else if (Config.isElectron) {
            this.#activeDownloader = new ElectronDownloader();
        } else {
            this.#activeDownloader = new WebDownloader();
        }

        await this.#activeDownloader.init();
        this._setInitialized(true);
    }

    /** Returns active downloader strategy instance */
    get activeDownloader() {
        return this.#activeDownloader;
    }

    /**
     * Adds track to download queue and starts processing.
     * @param {Object} trackInfo
     */
    async enqueueTrack(trackInfo) {
        if (!this.isInitialized) await this.init();
        this.#queue.push(trackInfo);
        this.emit('queueUpdated', { queue: [...this.#queue] });
        this.processQueue();
    }

    /**
     * Processes download queue sequentially.
     */
    async processQueue() {
        if (this.#isProcessing || this.#queue.length === 0) return;
        this.#isProcessing = true;

        while (this.#queue.length > 0) {
            const track = this.#queue.shift();
            this.emit('downloadStarted', { track });
            try {
                const res = await this.#activeDownloader.downloadTrack(track);
                this.emit('downloadCompleted', { track, result: res });
            } catch (err) {
                this.emit('downloadFailed', { track, error: err.message });
            }
        }

        this.#isProcessing = false;
        this.emit('queueFinished');
    }

    /**
     * Cancels active download task.
     */
    cancelCurrent() {
        if (this.#activeDownloader) {
            this.#activeDownloader.cancelDownload();
        }
    }
}

/** DownloadManager singleton instance */
export const DownloadManager = new DownloadManagerService();
