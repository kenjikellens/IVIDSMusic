import { Config } from './config.js';

/**
 * MusicAPI - Powered by Deezer (via Proxy) and iTunes
 * Provides authentic genre and year-based music discovery.
 */
export const MusicAPI = {
    // Local server proxy to bypass CORS
    get proxyUrl() {
        return `${Config.SERVER_URL}/proxy?url=`;
    },

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
     * Proxy-aware fetch helper
     * Routes external requests through the proxy in Native Mode to avoid CORS issues.
     */
    async _fetch(url, options = {}) {
        let finalUrl = url;

        const isExternal = url.startsWith('http') && typeof window !== 'undefined' && !url.startsWith(window.location.origin);

        if (Config.isNative) {
            // Only proxy external URLs
            if (isExternal && !url.includes('appassets.androidplatform.net')) {
                finalUrl = this.proxyUrl + encodeURIComponent(url);
            }
        } else {
            // In web mode, route external requests (like Deezer APIs) through public corsproxy.
            // Ignore requests that are already destined for our local Node backend (:3000).
            if (isExternal && !url.includes(':3000')) {
                finalUrl = `https://corsproxy.io/?url=${encodeURIComponent(url)}`;
            }
        }

        const response = await fetch(finalUrl, options);
        if (!response.ok) throw new Error(`HTTP ${response.status} for ${url}`);
        return response;
    },

    /**
     * Universal search using Deezer for better metadata/filtering
     */
    async search(query = 'top hits', limit = 20, type = 'all', yearRange = null, offset = 0, unique = false, signal = null) {
        try {
            // Build Deezer Advanced Query
            let q = query;
            if (yearRange) q += ` year:${yearRange}`;

            // Map types to Deezer search endpoints
            let endpoint = 'search';
            if (type === 'artist') endpoint = 'search/artist';
            else if (type === 'album') endpoint = 'search/album';

            const url = `${this.deezerUrl}/${endpoint}?q=${encodeURIComponent(q)}&limit=${unique ? 50 : limit}&index=${offset}`;
            const response = await this._fetch(url, { signal });
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
    async searchiTunes(query, limit, type, signal = null) {
        try {
            const ent = type === 'artist' ? 'musicArtist' : (type === 'album' ? 'album' : 'song');
            const url = `${this.itunesUrl}?term=${encodeURIComponent(query)}&entity=${ent}&limit=${limit}`;
            const res = await this._fetch(url, { signal });
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

    // Deezer genre IDs (from https://api.deezer.com/genre)
    genreMap: {
        'Pop': 132,
        'Rock': 152,
        'Hip-Hop': 116,
        'Electronic': 106,
        'Hardcore': 464,      // Metal (closest match)
        'R&B': 165,
        'Jazz': 129,
        'Dance': 113,
        'Alternative': 85,
    },

    /**
     * Fetch genre categories using Deezer chart endpoints (proper genre filtering)
     */
    async getCategories(genres = ['Pop', 'Rock', 'Hip-Hop', 'Hardcore', '90\'s', 'Electronic'], signal = null) {
        const results = await Promise.all(
            genres.map(async (genre) => {
                try {
                    let tracks = [];

                    if (genre === '90\'s') {
                        // Deezer has no "decade" genre, so use advanced search with dur_min to get variety
                        // Search for popular tracks from the 90s era
                        const queries = ['best of 90s', '90s dance hits', '90s rock hits', '90s pop classics'];
                        const randomQuery = queries[Math.floor(Math.random() * queries.length)];
                        const url = `${this.deezerUrl}/search?q=${encodeURIComponent(randomQuery)}&limit=30`;

                        const response = await this._fetch(url, { signal });
                        const data = await response.json();
                        if (data.data) {
                            tracks = data.data.map(item => ({
                                type: 'song',
                                id: item.id,
                                title: item.title,
                                artist: item.artist?.name || 'Unknown',
                                album: item.album?.title || 'Unknown',
                                cover: item.album?.cover_big || item.album?.cover_xl,
                                previewUrl: item.preview
                            }));
                        }
                    } else {
                        // Use Deezer chart endpoint for proper genre filtering
                        const genreId = this.genreMap[genre];
                        if (!genreId) return { title: genre, id: genre.toLowerCase(), tracks: [] };

                        const url = `${this.deezerUrl}/chart/${genreId}/tracks?limit=30`;

                        const response = await this._fetch(url, { signal });
                        const data = await response.json();

                        if (data.data) {
                            tracks = data.data.map(item => ({
                                type: 'song',
                                id: item.id,
                                title: item.title_short || item.title,
                                artist: item.artist?.name || 'Unknown',
                                album: item.album?.title || 'Unknown',
                                cover: item.album?.cover_big || item.album?.cover_xl,
                                previewUrl: item.preview
                            }));
                        }
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
                    console.error(`[API] Genre ${genre} failed:`, e);
                    return { title: genre, id: genre.toLowerCase(), tracks: [] };
                }
            })
        );
        return results.filter(row => row.tracks.length > 0);
    },

    async getRecommendations(signal = null) {
        return this.getCategories(undefined, signal);
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
                const response = await this._fetch(url, { signal: controller.signal });
                clearTimeout(timeoutId);
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
                const response = await this._fetch(url, { signal: controller.signal });
                clearTimeout(timeoutId);
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
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 6000);
            const res = await this._fetch(ytUrl, { signal: controller.signal });
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
     * Gets full artist profile data by name.
     */
    async getArtistByName(name, signal = null) {
        try {
            // Step 1: Search for the artist to get their ID
            const searchUrl = `${this.deezerUrl}/search/artist?q=${encodeURIComponent(name)}&limit=1`;
            const searchRes = await this._fetch(searchUrl, { signal });
            const searchData = await searchRes.json();
            if (!searchData?.data?.length) return null;

            const artistId = searchData.data[0].id;

            // Step 2: Fetch the direct /artist/{id} endpoint for accurate stats
            const detailUrl = `${this.deezerUrl}/artist/${artistId}`;
            const detailRes = await this._fetch(detailUrl, { signal });
            const artist = await detailRes.json();
            return artist || null;
        } catch (e) {
            console.error('[API] Failed to get artist by name', e);
        }
        return null;
    },

    /**
     * Gets artist's top tracks formatted for CardSystem.
     */
    async getArtistTopTracks(id, limit = 10, signal = null) {
        try {
            const url = `${this.deezerUrl}/artist/${id}/top?limit=${limit}`;
            const res = await this._fetch(url, { signal });
            const data = await res.json();

            if (data && data.data) {
                return data.data.map(item => ({
                    type: 'song',
                    id: item.id,
                    title: item.title_short || item.title,
                    artist: item.artist?.name || 'Unknown',
                    album: item.album?.title || 'Unknown',
                    cover: item.album?.cover_big || item.album?.cover_xl,
                    previewUrl: item.preview
                }));
            }
        } catch (e) {
            console.error('[API] Failed to get artist top tracks', e);
        }
        return [];
    },

    /**
     * Gets artist's albums formatted for CardSystem.
     */
    async getArtistAlbums(id, limit = 50, artistName = 'Unknown', signal = null) {
        try {
            const url = `${this.deezerUrl}/artist/${id}/albums?limit=${limit}`;
            const res = await this._fetch(url, { signal });
            const data = await res.json();

            if (data && data.data) {
                return data.data
                    .filter(item => item.record_type === 'album')
                    .map(item => ({
                        type: 'album',
                        id: item.id,
                        title: item.title,
                        artist: artistName,
                        cover: item.cover_big || item.cover_xl,
                        releaseDate: item.release_date
                    }));
            }
        } catch (e) {
            console.error('[API] Failed to get artist albums', e);
        }
        return [];
    },

    /**
     * Gets full album details and its tracks.
     */
    async getAlbumDetails(id, signal = null) {
        try {
            const url = `${this.deezerUrl}/album/${id}`;
            const res = await this._fetch(url, { signal });
            const album = await res.json();

            if (album && album.tracks && album.tracks.data) {
                return {
                    id: album.id,
                    title: album.title,
                    artist: album.artist.name,
                    cover: album.cover_big || album.cover_xl,
                    releaseDate: album.release_date,
                    nb_tracks: album.nb_tracks,
                    tracks: album.tracks.data.map(item => ({
                        type: 'song',
                        id: item.id,
                        title: item.title_short || item.title,
                        artist: item.artist?.name || album.artist.name,
                        album: album.title,
                        cover: album.cover_big || album.cover_xl,
                        previewUrl: item.preview
                    }))
                };
            }
        } catch (e) {
            console.error('[API] Failed to get album details', e);
        }
        return null;
    },

    /**
     * Gets a list of user's saved tracks.
     * @returns {Promise<Array>} List of track objects {filename, artist, title, url}
     */
    async getSavedTracks(signal = null) {
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
                const response = await fetch(`${Config.SERVER_URL}/api/saved`, { signal });
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                return await response.json();
            } catch (error) {
                console.error('[API] Web getSavedTracks error:', error);
                return [];
            }
        }
    }
};
