/**
 * IVIDS Music - Playlist Management System
 * Handles creation, deletion, and track additions/removals for custom playlists saved in local storage.
 */
export const PlaylistManager = {
    // Local storage key for storing user-created playlists
    STORAGE_KEY: 'iv_playlists',

    // Predefined harmonious aesthetic CSS linear gradients for playlist cover backdrops
    GRADIENTS: [
        'linear-gradient(135deg, #ff5e62, #ff9966)',
        'linear-gradient(135deg, #00c6ff, #0072ff)',
        'linear-gradient(135deg, #f21b3f, #ab0000)',
        'linear-gradient(135deg, #11998e, #38ef7d)',
        'linear-gradient(135deg, #654ea3, #eaafc8)',
        'linear-gradient(135deg, #f12711, #f5af19)'
    ],

    /**
     * Retrieves all user playlists from localStorage.
     * @returns {Array<Object>} List of saved playlist objects
     */
    getPlaylists() {
        try {
            const data = localStorage.getItem(this.STORAGE_KEY);
            return data ? JSON.parse(data) : [];
        } catch (e) {
            console.error('[PlaylistManager] Failed to read playlists', e);
            return [];
        }
    },

    /**
     * Retrieves a specific playlist by its unique ID.
     * @param {string} id - The playlist unique ID
     * @returns {Object|null} The playlist object, or null if not found
     */
    getPlaylist(id) {
        const playlists = this.getPlaylists();
        return playlists.find(p => p.id === id) || null;
    },

    /**
     * Creates a new custom playlist and persists it to localStorage.
     * @param {string} name - Name of the playlist
     * @param {string} description - Optional playlist description
     * @returns {Object} The newly created playlist object
     */
    createPlaylist(name, description = '') {
        const playlists = this.getPlaylists();
        const cleanName = name ? name.trim() : 'My Playlist';

        // Select a gradient dynamically based on count to ensure variety
        const gradientIndex = playlists.length % this.GRADIENTS.length;
        const coverGradient = this.GRADIENTS[gradientIndex];

        const newPlaylist = {
            id: 'pl_' + Date.now() + '_' + Math.floor(Math.random() * 1000),
            name: cleanName,
            description: description.trim(),
            cover: coverGradient,
            tracks: [],
            createdAt: new Date().toISOString()
        };

        playlists.push(newPlaylist);
        localStorage.setItem(this.STORAGE_KEY, JSON.stringify(playlists));
        
        // Dispatch global event so UI components can update instantly
        window.dispatchEvent(new CustomEvent('iv-playlists-updated'));

        return newPlaylist;
    },

    /**
     * Deletes an existing playlist by its unique ID.
     * @param {string} id - ID of the playlist to delete
     * @returns {boolean} True if successfully deleted, false otherwise
     */
    deletePlaylist(id) {
        let playlists = this.getPlaylists();
        const originalLength = playlists.length;
        
        playlists = playlists.filter(p => p.id !== id);
        
        if (playlists.length === originalLength) return false;

        localStorage.setItem(this.STORAGE_KEY, JSON.stringify(playlists));
        window.dispatchEvent(new CustomEvent('iv-playlists-updated'));
        return true;
    },

    /**
     * Adds a track to a target playlist, ensuring no duplicate tracks are entered.
     * @param {string} playlistId - Target playlist unique ID
     * @param {Object} track - The Deezer/YouTube track object to append
     * @returns {Object} { success: boolean, message: string } Result details
     */
    addTrack(playlistId, track) {
        if (!track || !track.title || !track.artist) {
            return { success: false, message: 'Invalid track data.' };
        }

        const playlists = this.getPlaylists();
        const playlistIndex = playlists.findIndex(p => p.id === playlistId);

        if (playlistIndex === -1) {
            return { success: false, message: 'Playlist not found.' };
        }

        const playlist = playlists[playlistIndex];
        
        // Prevent duplicate songs within the same playlist
        const exists = playlist.tracks.some(t => 
            (t.id && t.id === track.id) || 
            (t.title.toLowerCase() === track.title.toLowerCase() && t.artist.toLowerCase() === track.artist.toLowerCase())
        );

        if (exists) {
            return { success: false, message: 'Track is already in this playlist.', code: 'exists' };
        }

        // Standardize track object format
        const trackToSave = {
            id: track.id || ('tr_' + Date.now() + '_' + Math.floor(Math.random() * 1000)),
            title: track.title,
            artist: track.artist,
            cover: track.cover || 'svg/library.svg',
            duration: track.duration || 0,
            album: track.album || '',
            year: track.year || '',
            isSaved: track.isSaved || false
        };

        playlist.tracks.push(trackToSave);
        playlists[playlistIndex] = playlist;

        localStorage.setItem(this.STORAGE_KEY, JSON.stringify(playlists));
        window.dispatchEvent(new CustomEvent('iv-playlists-updated'));

        return { success: true, message: 'Track added to playlist!' };
    },

    /**
     * Removes a track from a target playlist.
     * @param {string} playlistId - Target playlist unique ID
     * @param {string|number} trackId - ID of the track to remove
     * @returns {boolean} True if successfully removed, false otherwise
     */
    removeTrack(playlistId, trackId) {
        const playlists = this.getPlaylists();
        const playlistIndex = playlists.findIndex(p => p.id === playlistId);

        if (playlistIndex === -1) return false;

        const playlist = playlists[playlistIndex];
        const originalLength = playlist.tracks.length;

        playlist.tracks = playlist.tracks.filter(t => t.id !== trackId);

        if (playlist.tracks.length === originalLength) return false;

        playlists[playlistIndex] = playlist;
        localStorage.setItem(this.STORAGE_KEY, JSON.stringify(playlists));
        window.dispatchEvent(new CustomEvent('iv-playlists-updated'));

        return true;
    }
};

// Bind to window for global access
if (typeof window !== 'undefined') {
    window.PlaylistManager = PlaylistManager;
}
