import { BaseService } from '../core/BaseService.js';

/**
 * AbstractStorageEngine defines the contract interface for platform storage strategies.
 */
export class AbstractStorageEngine extends BaseService {
    /**
     * Saves a track to persistent local storage.
     * @param {Object} trackData - Metadata and audio payload.
     * @returns {Promise<Object>} Status response.
     */
    async saveTrack(trackData) {
        throw new Error("Method 'saveTrack()' must be implemented by concrete subclass.");
    }

    /**
     * Retrieves all saved tracks from storage.
     * @returns {Promise<Array<Object>>} List of cached tracks.
     */
    async getSavedTracks() {
        throw new Error("Method 'getSavedTracks()' must be implemented by concrete subclass.");
    }

    /**
     * Deletes a track entry from storage by identifier/filename.
     * @param {string} identifier
     * @returns {Promise<Object>} Deletion result.
     */
    async deleteTrack(identifier) {
        throw new Error("Method 'deleteTrack()' must be implemented by concrete subclass.");
    }
}
