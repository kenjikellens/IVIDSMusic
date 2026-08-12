import { ProxyService } from './ProxyService.js';
import { DTOMapper } from '../domain/DTOMapper.js';

/**
 * iTunesProvider handles supplemental music metadata searches via iTunes Search API.
 */
export class iTunesProvider {
    #baseUrl = 'https://itunes.apple.com/search';

    /**
     * Searches iTunes metadata catalog.
     * @param {string} query
     * @param {number} limit
     * @param {string} entity
     * @returns {Promise<Array<Object>>}
     */
    async search(query, limit = 20, entity = 'song') {
        try {
            const url = `${this.#baseUrl}?term=${encodeURIComponent(query)}&limit=${limit}&entity=${entity}`;
            const response = await ProxyService.fetch(url);
            const data = await response.json();
            if (!data.results) return [];

            return data.results.map(item => DTOMapper.toSong(item));
        } catch (error) {
            console.error('[iTunesProvider] Search error:', error);
            return [];
        }
    }
}

