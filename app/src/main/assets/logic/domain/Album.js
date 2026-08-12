/**
 * Domain entity representing a music album.
 */
export class Album {
    #id = '';
    #title = '';
    #artist = '';
    #artistId = null;
    #cover = '';
    #trackCount = 0;
    #releaseYear = '';

    /**
     * Creates a new Album instance.
     * @param {Object} data - Album raw properties
     */
    constructor(data = {}) {
        this.#id = String(data.id || '');
        this.#title = data.title || 'Unknown Album';
        this.#artist = data.artist || 'Unknown Artist';
        this.#artistId = data.artistId || null;
        this.#cover = data.cover || 'svg/album-placeholder.svg';
        this.#trackCount = Number(data.trackCount) || 0;
        this.#releaseYear = String(data.releaseYear || '');
    }

    /** Gets album unique identifier */
    get id() { return this.#id; }

    /** Gets album title */
    get title() { return this.#title; }

    /** Gets primary artist name */
    get artist() { return this.#artist; }

    /** Gets primary artist ID */
    get artistId() { return this.#artistId; }

    /** Gets cover artwork URL */
    get cover() { return this.#cover; }

    /** Gets total track count */
    get trackCount() { return this.#trackCount; }

    /** Gets album release year */
    get releaseYear() { return this.#releaseYear; }

    /** Gets item type identifier */
    get type() { return 'album'; }

    /**
     * Converts domain model to plain JSON object representation.
     * @returns {Object} Plain object
     */
    toJSON() {
        return {
            type: 'album',
            id: this.#id,
            title: this.#title,
            artist: this.#artist,
            artistId: this.#artistId,
            cover: this.#cover,
            trackCount: this.#trackCount,
            releaseYear: this.#releaseYear
        };
    }
}
