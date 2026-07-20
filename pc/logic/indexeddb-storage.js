/**
 * IndexedDBStorage handles offline music caching inside browser databases.
 * Stores audio streams as binary Blobs and creates temporary runtime Blob URLs
 * to allow static Web builds (such as GitHub Pages) to function offline.
 */
export const IndexedDBStorage = {
    dbName: 'IVIDSMusicDB',
    dbVersion: 1,
    storeName: 'tracks',
    _db: null,

    /**
     * Initializes and opens the IndexedDB database structure.
     * Creates object stores and indices if they do not exist.
     *
     * @returns {Promise<IDBDatabase>} The initialized database connection.
     */
    async initDb() {
        if (this._db) return this._db;

        return new Promise((resolve, reject) => {
            const request = indexedDB.open(this.dbName, this.dbVersion);

            request.onupgradeneeded = (event) => {
                const db = event.target.result;
                if (!db.objectStoreNames.contains(this.storeName)) {
                    const store = db.createObjectStore(this.storeName, { keyPath: 'filename' });
                    store.createIndex('videoId', 'videoId', { unique: false });
                }
            };

            request.onsuccess = (event) => {
                this._db = event.target.result;
                resolve(this._db);
            };

            request.onerror = (event) => {
                console.error('IndexedDB initialization failed:', event.target.error);
                reject(event.target.error);
            };
        });
    },

    /**
     * Downloads an audio stream URL as a blob and commits it to IndexedDB.
     *
     * @param {string} videoId The YouTube video identifier.
     * @param {string} artist The track artist.
     * @param {string} title The track title.
     * @param {string} audioUrl Resolved streaming endpoint.
     * @returns {Promise<Object>} Status report containing the blob URL.
     */
    async saveTrack(videoId, artist, title, audioUrl) {
        const db = await this.initDb();
        const cleanArtist = artist.replace(/[/\\?%*:|"<>]/g, '').trim();
        const cleanTitle = title.replace(/[/\\?%*:|"<>]/g, '').trim();
        const filename = `${cleanArtist} - ${cleanTitle}.m4a`;

        try {
            // Fetch audio stream from resolved URL
            const response = await fetch(audioUrl);
            if (!response.ok) throw new Error(`HTTP ${response.status} when fetching audio stream`);
            const blob = await response.blob();

            return new Promise((resolve, reject) => {
                const transaction = db.transaction(this.storeName, 'readwrite');
                const store = transaction.objectStore(this.storeName);
                
                const trackData = {
                    filename: filename,
                    videoId: videoId,
                    artist: cleanArtist,
                    title: cleanTitle,
                    blob: blob,
                    timestamp: Date.now()
                };

                const request = store.put(trackData);

                request.onsuccess = () => {
                    resolve({
                        status: 'saved',
                        message: 'Track cached successfully in browser IndexedDB',
                        url: URL.createObjectURL(blob)
                    });
                };

                request.onerror = (event) => {
                    reject(event.target.error);
                };
            });
        } catch (error) {
            console.error('Failed to save track in IndexedDB:', error);
            return { status: 'error', message: error.message };
        }
    },

    /**
     * Reads all saved tracks from the IndexedDB instance.
     * Maps blobs to runtime-accessible Blob URLs.
     *
     * @returns {Promise<Array<Object>>} List of cached tracks.
     */
    async getSavedTracks() {
        const db = await this.initDb();

        return new Promise((resolve, reject) => {
            const transaction = db.transaction(this.storeName, 'readonly');
            const store = transaction.objectStore(this.storeName);
            const request = store.getAll();

            request.onsuccess = () => {
                const tracks = request.result.map(item => {
                    const blobUrl = URL.createObjectURL(item.blob);
                    return {
                        filename: item.filename,
                        artist: item.artist,
                        title: item.title,
                        url: blobUrl
                    };
                });
                resolve(tracks);
            };

            request.onerror = (event) => {
                reject(event.target.error);
            };
        });
    },

    /**
     * Deletes a saved track entry from the IndexedDB store.
     *
     * @param {string} filename The name of the file key to delete.
     * @returns {Promise<Object>} Deletion result.
     */
    async deleteTrack(filename) {
        const db = await this.initDb();

        return new Promise((resolve, reject) => {
            const transaction = db.transaction(this.storeName, 'readwrite');
            const store = transaction.objectStore(this.storeName);
            const request = store.delete(filename);

            request.onsuccess = () => {
                resolve({ status: 'deleted' });
            };

            request.onerror = (event) => {
                reject(event.target.error);
            };
        });
    }
};
