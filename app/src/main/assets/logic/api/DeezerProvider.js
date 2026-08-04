import { ProxyService } from './ProxyService.js';

/**
 * DeezerProvider handles music discovery, metadata searching, genres, and chart endpoints via Deezer API.
 */
export class DeezerProvider {
    #baseUrl = 'https://api.deezer.com';

    /**
     * Executes metadata search on Deezer.
     * @param {string} query
     * @param {number} limit
     * @param {string} type - 'all', 'artist', 'album'
     * @param {string} [yearRange]
     * @param {number} [offset]
     * @param {boolean} [unique]
     * @param {AbortSignal} [signal]
     * @returns {Promise<Array<Object>>}
     */
    async search(query = 'top hits', limit = 20, type = 'all', yearRange = null, offset = 0, unique = false, signal = null) {
        try {
            let q = query;
            if (yearRange) q += ` year:${yearRange}`;

            let endpoint = 'search';
            if (type === 'artist') endpoint = 'search/artist';
            else if (type === 'album') endpoint = 'search/album';

            const url = `${this.#baseUrl}/${endpoint}?q=${encodeURIComponent(q)}&limit=${unique ? 50 : limit}&index=${offset}`;
            const response = await ProxyService.fetch(url, { signal });
            const data = await response.json();
            if (!data.data) return [];

            let results = data.data.map(item => {
                if (type === 'artist') {
                    return {
                        type: 'artist',
                        id: item.id,
                        name: item.name,
                        cover: item.picture_big || item.picture_medium,
                        genre: 'Artist'
                    };
                } else if (type === 'album') {
                    return {
                        type: 'album',
                        id: item.id,
                        title: item.title,
                        artist: item.artist?.name || 'Unknown',
                        cover: item.cover_big || item.cover_xl
                    };
                } else {
                    return {
                        type: 'song',
                        id: item.id,
                        title: item.title,
                        artist: item.artist?.name || 'Unknown',
                        artistId: item.artist?.id || null,
                        album: item.album?.title || 'Unknown',
                        cover: item.album?.cover_big || item.album?.cover_xl,
                        previewUrl: item.preview
                    };
                }
            });

            if (unique) {
                const seen = new Set();
                results = results.filter(item => {
                    const key = type === 'artist' ? item.name.toLowerCase() : (type === 'album' ? item.title.toLowerCase() : `${item.title}-${item.artist}`.toLowerCase());
                    if (seen.has(key)) return false;
                    seen.add(key);
                    return true;
                }).slice(0, limit);
            }

            return results;
        } catch (error) {
            console.error('[DeezerProvider] Search error:', error);
            return [];
        }
    }

    /**
     * Gets top chart tracks from Deezer.
     * @param {number} limit
     * @returns {Promise<Array<Object>>}
     */
    async getChart(limit = 20) {
        try {
            const url = `${this.#baseUrl}/chart/0/tracks?limit=${limit}`;
            const response = await ProxyService.fetch(url);
            const data = await response.json();
            if (!data.data) return [];

            return data.data.map(item => ({
                type: 'song',
                id: item.id,
                title: item.title,
                artist: item.artist?.name || 'Unknown',
                artistId: item.artist?.id || null,
                album: item.album?.title || 'Unknown',
                cover: item.album?.cover_big || item.album?.cover_xl,
                previewUrl: item.preview
            }));
        } catch (error) {
            console.error('[DeezerProvider] Chart error:', error);
            return [];
        }
    }
}
