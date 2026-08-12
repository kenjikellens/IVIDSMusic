import { ProxyService } from './ProxyService.js';
import { DTOMapper } from '../domain/DTOMapper.js';

/**
 * DeezerProvider handles music discovery, metadata searching, genres, and chart endpoints via Deezer API.
 */
export class DeezerProvider {
    #baseUrl = 'https://api.deezer.com';

    genreMap = {
        'Pop': 132,
        'Rock': 152,
        'Hip-Hop': 116,
        'Electronic': 106,
        'Hardcore': 464,
        'R&B': 165,
        'Jazz': 129,
        'Dance': 113,
        'Alternative': 85,
    };

    /**
     * Executes metadata search on Deezer.
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
                if (type === 'artist') return DTOMapper.toArtist(item);
                if (type === 'album') return DTOMapper.toAlbum(item);
                return DTOMapper.toSong(item);
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
            if (error.name !== 'AbortError') {
                console.error('[DeezerProvider] Search error:', error);
            }
            return [];
        }
    }

    /**
     * Fetch category rows using Deezer genre chart endpoints (proper genre filtering).
     */
    async getCategories(genres = ['Pop', 'Rock', 'Hip-Hop', 'Hardcore', 'Electronic', 'Jazz', 'Dance'], signal = null) {
        const results = await Promise.all(
            genres.map(async (genre) => {
                try {
                    const genreId = this.genreMap[genre];
                    if (!genreId) return { title: genre, id: genre.toLowerCase().replace(/\s+/g, '-'), tracks: [] };

                    const url = `${this.#baseUrl}/chart/${genreId}/tracks?limit=30`;
                    const response = await ProxyService.fetch(url, { signal });
                    const data = await response.json();

                    let tracks = [];
                    if (data && data.data) {
                        tracks = data.data.map(item => DTOMapper.toSong(item));
                    }

                    // Variety filter: max 1 track per artist
                    const seen = new Set();
                    const uniqueTracks = tracks.filter(item => {
                        const key = (item.artist || '').toLowerCase();
                        if (seen.has(key)) return false;
                        seen.add(key);
                        return true;
                    }).slice(0, 12);

                    return {
                        title: genre,
                        id: genre.toLowerCase().replace(/\s+/g, '-').replace(/'/g, ''),
                        tracks: uniqueTracks
                    };
                } catch (e) {
                    if (e.name !== 'AbortError') {
                        console.error(`[DeezerProvider] Genre ${genre} failed:`, e);
                    }
                    return { title: genre, id: genre.toLowerCase().replace(/\s+/g, '-'), tracks: [] };
                }
            })
        );
        return results.filter(row => row.tracks.length > 0);
    }

    /**
     * Gets top chart tracks from Deezer.
     */
    async getChart(limit = 20) {
        try {
            const url = `${this.#baseUrl}/chart/0/tracks?limit=${limit}`;
            const response = await ProxyService.fetch(url);
            const data = await response.json();
            if (!data.data) return [];

            return data.data.map(item => DTOMapper.toSong(item));
        } catch (error) {
            if (error.name !== 'AbortError') {
                console.error('[DeezerProvider] Chart error:', error);
            }
            return [];
        }
    }
}

