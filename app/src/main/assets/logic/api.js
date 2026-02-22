import { Config } from './config.js';

/**
 * MusicAPI - Powered by Deezer (via Proxy) and iTunes
 * Provides authentic genre and year-based music discovery.
 */
export const MusicAPI = {
    // Local server proxy to bypass CORS
    proxyUrl: `${Config.SERVER_URL}/proxy?url=`,

    // Base APIs
    deezerUrl: 'https://api.deezer.com',
    itunesUrl: 'https://itunes.apple.com/search',

    // List of public Invidious instances for YouTube playback
    invidiousInstances: [
        'https://invidious.flokinet.to',
        'https://iv.melmac.space',
        'https://invidious.drgns.space'
    ],

    /**
     * Universal search using Deezer for better metadata/filtering
     */
    async search(query = 'top hits', limit = 20, type = 'all', yearRange = null, offset = 0, unique = false) {
        try {
            // Build Deezer Advanced Query
            let q = query;
            if (yearRange) q += ` year:${yearRange}`;

            // Map types to Deezer search endpoints
            let endpoint = 'search';
            if (type === 'artist') endpoint = 'search/artist';
            else if (type === 'album') endpoint = 'search/album';

            const url = `${this.deezerUrl}/${endpoint}?q=${encodeURIComponent(q)}&limit=${unique ? 50 : limit}&index=${offset}`;
            const response = await fetch(this.proxyUrl + encodeURIComponent(url));
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
                        album: item.album?.title || 'Unknown',
                        cover: item.album?.cover_big || item.album?.cover_xl,
                        previewUrl: item.preview
                    };
                }
            });

            // Perform variety filtering
            if (unique) {
                const seen = new Set();
                results = results.filter(item => {
                    const key = item.artist || item.name;
                    if (seen.has(key)) return false;
                    seen.add(key);
                    return true;
                }).slice(0, limit);
            }

            return results;
        } catch (error) {
            console.error('Deezer Error, falling back to iTunes:', error);
            return this.searchiTunes(query, limit, type);
        }
    },

    /**
     * Fallback to iTunes if Deezer/Proxy fails
     */
    async searchiTunes(query, limit, type) {
        try {
            const ent = type === 'artist' ? 'musicArtist' : (type === 'album' ? 'album' : 'song');
            const url = `${this.itunesUrl}?term=${encodeURIComponent(query)}&entity=${ent}&limit=${limit}`;
            const res = await fetch(url);
            const data = await res.json();
            return data.results.map(item => ({
                id: item.trackId || item.collectionId || item.artistId,
                title: item.trackName || item.collectionName || item.artistName,
                artist: item.artistName,
                cover: (item.artworkUrl100 || '').replace('100x100bb', '600x600bb'),
                type: type === 'all' ? 'song' : type
            }));
        } catch (e) { return []; }
    },

    /**
     * Fetch authentic categories using Deezer's ranking and date filters
     */
    async getCategories(genres = ['Pop', 'Rock', 'Hip-Hop', 'Hardcore', '90\'s', 'Electronic']) {
        const results = await Promise.all(
            genres.map(async (genre) => {
                let tracks = [];

                if (genre === '90\'s') {
                    // THE FIX: True year-range filtering (1990-1999) sorted by popularity
                    tracks = await this.search('top hits', 12, 'album', '1990-1999', 0, true);
                } else if (genre === 'Hardcore') {
                    // THE FIX: Authentic genre search instead of broken Genre IDs
                    tracks = await this.search('Hardcore Punk', 12, 'album', null, 0, true);
                } else {
                    tracks = await this.search(genre, 12, 'album', null, 0, true);
                }

                return {
                    title: genre,
                    id: genre.toLowerCase().replace(/\s+/g, '-').replace(/'/g, ''),
                    tracks
                };
            })
        );
        return results.filter(row => row.tracks.length > 0);
    },

    async getRecommendations() {
        return this.getCategories();
    },

    /**
     * Get specific YouTube Video ID using redundant Invidious instances
     */
    async getYouTubeVideoId(query) {
        const searchQuery = encodeURIComponent(query);
        for (const instance of this.invidiousInstances) {
            try {
                const url = `${instance}/api/v1/search?q=${searchQuery}&type=video&limit=1`;
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 10000);
                const response = await fetch(url, { signal: controller.signal });
                clearTimeout(timeoutId);
                if (!response.ok) continue;
                const data = await response.json();
                if (data && data.length > 0) return data[0].videoId;
            } catch (error) { continue; }
        }
        return null;
    },

    async getArtistImage(name) {
        const data = await this.search(name, 1, 'artist');
        return data.length ? data[0].cover : null;
    },

    async getAverageColor(imageUrl) {
        if (!imageUrl) return 'rgba(255,255,255,0.05)';
        if (!this._colorCache) this._colorCache = new Map();
        if (this._colorCache.has(imageUrl)) return this._colorCache.get(imageUrl);

        return new Promise((resolve) => {
            const img = new Image();
            img.crossOrigin = "Anonymous";
            img.src = imageUrl;
            img.onload = () => {
                try {
                    const canvas = document.createElement("canvas");
                    canvas.width = 16; canvas.height = 16;
                    const ctx = canvas.getContext("2d");
                    ctx.drawImage(img, 0, 0, 16, 16);
                    const data = ctx.getImageData(0, 0, 16, 16).data;
                    let r = 0, g = 0, b = 0;
                    for (let i = 0; i < data.length; i += 4) {
                        r += data[i]; g += data[i + 1]; b += data[i + 2];
                    }
                    const count = data.length / 4;
                    const color = `rgb(${Math.round(r / count)}, ${Math.round(g / count)}, ${Math.round(b / count)})`;
                    this._colorCache.set(imageUrl, color);
                    resolve(color);
                } catch (e) { resolve("rgba(255,255,255,0.05)"); }
            };
            img.onerror = () => resolve("rgba(255,255,255,0.05)");
        });
    }
};
