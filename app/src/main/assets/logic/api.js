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
     * Fetch categories using iTunes
     */
    async getCategories(genres = ['Pop', 'Rock', 'Hip-Hop', 'Hardcore', '90\'s', 'Electronic']) {
        const results = await Promise.all(
            genres.map(async (genre) => {
                let tracks = [];

                if (genre === '90\'s') {
                    tracks = await this.searchiTunes('90s hits', 12, 'all');
                } else if (genre === 'Hardcore') {
                    tracks = await this.searchiTunes('hardcore', 12, 'all');
                } else {
                    tracks = await this.searchiTunes(genre, 12, 'all');
                }

                // Perform rough variety filtering
                const seen = new Set();
                const uniqueTracks = tracks.filter(item => {
                    const key = `${item.artist?.toLowerCase() || ''}-${item.title?.toLowerCase() || ''}`;
                    if (seen.has(key)) return false;
                    seen.add(key);
                    return true;
                }).slice(0, 12);

                return {
                    title: genre,
                    id: genre.toLowerCase().replace(/\s+/g, '-').replace(/'/g, ''),
                    tracks: uniqueTracks
                };
            })
        );
        return results.filter(row => row.tracks.length > 0);
    },

    async getRecommendations() {
        return this.getCategories();
    },

    /**
     * Get specific YouTube Video ID using redundant instances and search fallbacks
     */
    async getYouTubeVideoId(query) {
        const searchQuery = encodeURIComponent(query);

        // 1. Try Multiple Invidious Instances
        const instances = [
            'https://invidious.flokinet.to',
            'https://iv.melmac.space',
            'https://invidious.drgns.space',
            'https://invidious.perennialte.chs.org',
            'https://yt.artemislena.eu'
        ];

        for (const instance of instances) {
            try {
                const url = `${instance}/api/v1/search?q=${searchQuery}&type=video&limit=1`;
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 4000); // 4s timeout
                const response = await fetch(url, { signal: controller.signal });
                clearTimeout(timeoutId);
                if (!response.ok) continue;
                const data = await response.json();
                if (data && data.length > 0 && data[0].videoId) return data[0].videoId;
            } catch (error) { continue; } // Silent fail, try next
        }

        // 2. Try Piped API Fallback
        const pipedInstances = [
            'https://pipedapi.kavin.rocks',
            'https://pipedapi.moomoo.me',
            'https://pipedapi.syncpundit.io'
        ];
        for (const instance of pipedInstances) {
            try {
                const url = `${instance}/search?q=${searchQuery}&filter=all`;
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 4000);
                const response = await fetch(url, { signal: controller.signal });
                clearTimeout(timeoutId);
                if (!response.ok) continue;
                const data = await response.json();
                if (data && data.items && data.items.length > 0) {
                    const item = data.items.find(i => i.url && i.url.includes('/watch?v='));
                    if (item) return item.url.split('v=')[1].split('&')[0];
                }
            } catch (error) { continue; }
        }

        // 3. Bulletproof Fallback: Proxy HTML scraping directly from YouTube
        // This relies on the local proxy extracting the raw YouTube HTML and pulling the first video ID match.
        try {
            const ytUrl = `https://www.youtube.com/results?search_query=${searchQuery}`;
            const proxyUrl = this.proxyUrl + encodeURIComponent(ytUrl);
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 6000);
            const res = await fetch(proxyUrl, { signal: controller.signal });
            clearTimeout(timeoutId);
            const html = await res.text();

            // Standard scraping regex for YouTube INITIAL_DATA video IDs
            const match = html.match(/"videoId":"([^"]{11})"/);
            if (match && match[1]) {
                return match[1];
            }
        } catch (e) {
            console.error('[Search] All YouTube Video Search methods failed.', e);
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
    },

    /**
     * Gets a list of user's saved tracks.
     * @returns {Promise<Array>} List of track objects {filename, artist, title, url}
     */
    async getSavedTracks() {
        if (window.AndroidAPI) {
            try {
                // Native Android Mode
                const jsonStr = window.AndroidAPI.getSavedTracks();
                return JSON.parse(jsonStr || '[]');
            } catch (error) {
                console.error('[API] Native getSavedTracks error:', error);
                return [];
            }
        } else {
            // Web / Node.js Mode
            try {
                const response = await fetch(`${Config.SERVER_URL}/api/saved`);
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                return await response.json();
            } catch (error) {
                console.error('[API] Web getSavedTracks error:', error);
                return [];
            }
        }
    }
};
