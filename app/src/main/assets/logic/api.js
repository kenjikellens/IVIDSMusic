import { Config } from './config.js';
import { IndexedDBStorage } from './indexeddb-storage.js';

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
            // In Electron, direct calls are allowed because we disabled webSecurity.
            // Ignore requests that are already destined for our local Node backend (:3000).
            if (isExternal && !url.includes(':3000') && !Config.isElectron) {
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
                        artistId: item.artist?.id || null,
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
    async getCategories(genres = ['Pop', 'Rock', 'Hip-Hop', 'Hardcore', 'Electronic', 'Jazz', 'Dance'], signal = null) {
        const results = await Promise.all(
            genres.map(async (genre) => {
                try {
                    let tracks = [];

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
                            artistId: item.artist?.id || null, // Capture artist ID for recommendation scoring telemetry
                            album: item.album?.title || 'Unknown',
                            cover: item.album?.cover_big || item.album?.cover_xl,
                            previewUrl: item.preview
                        }));
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
     * Fetches the top tracks for a given artist from the Deezer API.
     * Extracts and transforms track, album, and artist metadata into a standard unified format
     * used by the application CardSystem and Player queue.
     *
     * @param {string|number} id - The unique identifier of the artist.
     * @param {number} [limit=10] - The maximum number of top tracks to retrieve.
     * @param {AbortSignal} [signal=null] - An optional signal to abort the fetch request.
     * @returns {Promise<Array<Object>>} A promise resolving to an array of unified track objects.
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
                    artistId: item.artist?.id || id || null, // Capture artist ID for recommendation scoring telemetry
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
                        artistId: item.artist?.id || album.artist?.id || null, // Capture artist ID for recommendation scoring telemetry
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
     *
     * @param {AbortSignal} [signal=null] - An optional signal to abort the fetch request.
     * @returns {Promise<Array>} List of track objects {filename, artist, title, url}
     */
    async getSavedTracks(signal = null) {
        if (Config.isNative) {
            try {
                // Native Android Mode
                const jsonStr = window.AndroidAPI.getSavedTracks();
                return JSON.parse(jsonStr || '[]');
            } catch (error) {
                console.error('[API] Native getSavedTracks error:', error);
                return [];
            }
        } else if (Config.isElectron) {
            try {
                return await window.ElectronAPI.getSavedTracks();
            } catch (error) {
                console.error('[API] Electron getSavedTracks error:', error);
                return [];
            }
        } else {
            // Web / Node.js Mode
            if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
                try {
                    const response = await fetch(`${Config.SERVER_URL}/api/saved`, { signal });
                    if (!response.ok) throw new Error(`HTTP ${response.status}`);
                    return await response.json();
                } catch (error) {
                    console.warn('[API] Local Node getSavedTracks failed, falling back to IndexedDB:', error);
                }
            }
            try {
                return await IndexedDBStorage.getSavedTracks();
            } catch (error) {
                console.error('[API] IndexedDB getSavedTracks error:', error);
                return [];
            }
        }
    },

    /**
     * Resolves the YouTube video playback streaming URL.
     *
     * @param {string} videoId - The YouTube Video ID.
     * @param {string} artist - Artist name.
     * @param {string} title - Song title.
     * @returns {Promise<Object>} Object containing status and resolved stream URL.
     */
    async playTrack(videoId, artist, title) {
        if (Config.isElectron) {
            try {
                const data = await window.ElectronAPI.playTrack(videoId);
                if (data && data.status === 'ready' && data.url) {
                    return data;
                }
                console.warn('[API] Electron yt-dlp resolution failed, falling back to client-side Invidious stream...', data);
            } catch (err) {
                console.warn('[API] Electron yt-dlp resolver error, falling back to client-side Invidious stream...', err);
            }
        }
        if (Config.isNative) {
            const params = new URLSearchParams({ videoId, artist, title });
            const response = await this._fetch(`${Config.SERVER_URL}/play?${params.toString()}`);
            return await response.json();
        }
        // Local Node server fallback
        if (!Config.isElectron && (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')) {
            try {
                const params = new URLSearchParams({ videoId, artist, title });
                const response = await fetch(`${Config.SERVER_URL}/play?${params.toString()}`);
                if (response.ok) return await response.json();
            } catch (e) {
                console.warn('[API] Local Node play server unavailable, falling back to client-side stream resolution.');
            }
        }

        // Static Web / GitHub Pages / Electron fallback mode: Query Invidious API directly
        for (const instance of this.invidiousInstances) {
            try {
                const url = `${instance}/api/v1/videos/${videoId}`;
                const finalUrl = Config.isElectron ? url : `https://corsproxy.io/?url=${encodeURIComponent(url)}`;
                const response = await fetch(finalUrl);
                if (!response.ok) continue;
                const json = await response.json();
                const streams = json.adaptiveFormats;
                if (streams) {
                    const audioStream = streams.find(s => s.type && s.type.includes('audio/'));
                    if (audioStream && audioStream.url) {
                        return { status: 'ready', url: audioStream.url };
                    }
                }
            } catch (e) {
                continue;
            }
        }
        return { status: 'error', message: 'No working stream found on Invidious' };
    },

    /**
     * Downloads and saves a track locally.
     *
     * @param {string} videoId - The YouTube Video ID.
     * @param {string} artist - Artist name.
     * @param {string} title - Song title.
     * @param {string} audioUrl - Currently playing resolved audio URL (required for IndexedDB).
     * @returns {Promise<Object>} Status of the save operation.
     */
    async saveTrack(videoId, artist, title, audioUrl) {
        if (Config.isElectron) {
            return await window.ElectronAPI.saveTrack(videoId, artist, title);
        }
        if (Config.isNative) {
            const params = new URLSearchParams({ videoId, artist, title });
            const response = await this._fetch(`${Config.SERVER_URL}/save?${params.toString()}`);
            return await response.json();
        }
        // Local Node server fallback
        if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
            try {
                const params = new URLSearchParams({ videoId, artist, title });
                const response = await fetch(`${Config.SERVER_URL}/save?${params.toString()}`);
                if (response.ok) return await response.json();
            } catch (e) {
                console.warn('[API] Local Node save server unavailable, falling back to IndexedDB.');
            }
        }

        // Static Web / GitHub Pages mode: Download stream and save to IndexedDB
        return await IndexedDBStorage.saveTrack(videoId, artist, title, audioUrl);
    },

    /**
     * Deletes a saved track from storage.
     *
     * @param {string} filename - The name of the file to delete.
     * @returns {Promise<Object>} Status of the deletion.
     */
    async deleteTrack(filename) {
        if (Config.isElectron) {
            return await window.ElectronAPI.deleteTrack(filename);
        }
        if (Config.isNative) {
            console.warn('[API] Delete track is not implemented on Android Native WebView yet.');
            return { status: 'error', message: 'Not supported on Android' };
        }
        // Static Web / GitHub Pages mode
        return await IndexedDBStorage.deleteTrack(filename);
    },


    /**
     * Gets rich metadata for a single track by Deezer ID.
     * @returns {Object} { id, title, artist, artistId, album, albumId, cover, duration, bpm, explicit, genres, releaseDate, previewUrl }
     */
    async getTrackDetails(id, signal = null) {
        try {
            const url = `${this.deezerUrl}/track/${id}`;
            const res = await this._fetch(url, { signal });
            const t = await res.json();
            return {
                id: t.id,
                title: t.title,
                artist: t.artist?.name || 'Unknown',
                artistId: t.artist?.id || null,
                album: t.album?.title || 'Unknown',
                albumId: t.album?.id || null,
                cover: t.album?.cover_big || t.album?.cover_xl || '',
                duration: t.duration || 0, // seconds
                bpm: t.bpm || null,
                explicit: t.explicit_lyrics || false,
                genres: t.genres?.data?.map(g => g.name) || [],
                releaseDate: t.release_date || null,
                previewUrl: t.preview || null,
                contributors: t.contributors?.map(c => c.name) || []
            };
        } catch (e) {
            console.error('[API] Failed to get track details', e);
            return null;
        }
    },

    /**
     * Gets tracks related to a given Deezer track ID.
     */
    async getRelatedTracks(id, signal = null) {
        try {
            const url = `${this.deezerUrl}/track/${id}/related`;
            const res = await this._fetch(url, { signal });
            const data = await res.json();
            if (data?.data) {
                return data.data.map(item => ({
                    type: 'song',
                    id: item.id,
                    title: item.title_short || item.title,
                    artist: item.artist?.name || 'Unknown',
                    album: item.album?.title || 'Unknown',
                    cover: item.album?.cover_big || item.album?.cover_xl || '',
                    previewUrl: item.preview
                }));
            }
        } catch (e) {
            console.error('[API] Failed to get related tracks', e);
        }
        return [];
    },


    /**
     * Fetches popular tracks for a given genre.
     */
    async getGenreTracks(genreName, signal = null) {
        // Deezer doesn't have a direct "search by genre name" endpoint that returns tracks easily
        // but we can search for the genre name as a query which usually yields popular results.
        return this.search(genreName, 10, 'song', null, 0, false, signal);
    }
};
