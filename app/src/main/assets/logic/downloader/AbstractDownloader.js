import { BaseService } from '../core/BaseService.js';

/**
 * AbstractDownloader defines the contract for platform-specific download managers.
 */
export class AbstractDownloader extends BaseService {
    /**
     * Initiates track download/caching.
     * @param {Object} trackInfo
     * @returns {Promise<Object>}
     */
    async downloadTrack(trackInfo) {
        throw new Error("Method 'downloadTrack()' must be implemented by concrete subclass.");
    }

    /**
     * Cancels active download task.
     */
    cancelDownload() {
        throw new Error("Method 'cancelDownload()' must be implemented by concrete subclass.");
    }
}
