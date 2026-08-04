import { AbstractStorageEngine } from './storage/AbstractStorageEngine.js';

/**
 * IndexedDBStorageEngine manages offline track caching inside browser IndexedDB.
 */
export class IndexedDBStorageEngine extends AbstractStorageEngine {
    #dbName = 'IVIDSMusicDB';
    #dbVersion = 1;
    #storeName = 'tracks';
    #db = null;

    /**
     * Initializes the IndexedDB database connection.
     * @returns {Promise<IDBDatabase>}
     */
    async initDb() {
        if (this.#db) return this.#db;

        return new Promise((resolve, reject) => {
            const request = indexedDB.open(this.#dbName, this.#dbVersion);

            request.onupgradeneeded = (event) => {
                const db = event.target.result;
                if (!db.objectStoreNames.contains(this.#storeName)) {
                    const store = db.createObjectStore(this.#storeName, { keyPath: 'filename' });
                    store.createIndex('videoId', 'videoId', { unique: false });
                }
            };

            request.onsuccess = (event) => {
                this.#db = event.target.result;
                this._setInitialized(true);
                resolve(this.#db);
            };

            request.onerror = (event) => {
                console.error('[IndexedDBStorage] Database initialization failed:', event.target.error);
                reject(event.target.error);
            };
        });
    }

    /**
     * Downloads an audio stream as a blob and saves it into IndexedDB.
     * @param {string} videoId
     * @param {string} artist
     * @param {string} title
     * @param {string} audioUrl
     * @returns {Promise<Object>} Status report with Blob URL.
     */
    async saveTrack(videoId, artist, title, audioUrl) {
        const db = await this.initDb();
        const cleanArtist = artist.replace(/[/\\?%*:|"<>]/g, '').trim();
        const cleanTitle = title.replace(/[/\\?%*:|"<>]/g, '').trim();
        const filename = `${cleanArtist} - ${cleanTitle}.m4a`;

        try {
            const response = await fetch(audioUrl);
            if (!response.ok) throw new Error(`HTTP ${response.status} fetching audio stream`);
            const blob = await response.blob();

            return new Promise((resolve, reject) => {
                const transaction = db.transaction(this.#storeName, 'readwrite');
                const store = transaction.objectStore(this.#storeName);

                const trackData = {
                    filename,
                    videoId,
                    artist: cleanArtist,
                    title: cleanTitle,
                    blob,
                    timestamp: Date.now()
                };

                const request = store.put(trackData);
                request.onsuccess = () => {
                    resolve({
                        status: 'saved',
                        message: 'Track cached successfully in IndexedDB',
                        url: URL.createObjectURL(blob)
                    });
                };
                request.onerror = (event) => reject(event.target.error);
            });
        } catch (error) {
            console.error('[IndexedDBStorage] Failed to save track:', error);
            return { status: 'error', message: error.message };
        }
    }

    /**
     * Retrieves all saved tracks from IndexedDB as runtime Blob URLs.
     * @returns {Promise<Array<Object>>}
     */
    async getSavedTracks() {
        const db = await this.initDb();
        return new Promise((resolve, reject) => {
            const transaction = db.transaction(this.#storeName, 'readonly');
            const store = transaction.objectStore(this.#storeName);
            const request = store.getAll();

            request.onsuccess = () => {
                const tracks = request.result.map(item => ({
                    filename: item.filename,
                    artist: item.artist,
                    title: item.title,
                    url: URL.createObjectURL(item.blob)
                }));
                resolve(tracks);
            };
            request.onerror = (event) => reject(event.target.error);
        });
    }

    /**
     * Deletes a saved track by filename key.
     * @param {string} filename
     * @returns {Promise<Object>}
     */
    async deleteTrack(filename) {
        const db = await this.initDb();
        return new Promise((resolve, reject) => {
            const transaction = db.transaction(this.#storeName, 'readwrite');
            const store = transaction.objectStore(this.#storeName);
            const request = store.delete(filename);

            request.onsuccess = () => resolve({ status: 'deleted' });
            request.onerror = (event) => reject(event.target.error);
        });
    }
}

/** IndexedDBStorage singleton */
export const IndexedDBStorage = new IndexedDBStorageEngine();
