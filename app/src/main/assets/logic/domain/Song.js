/**
 * Domain entity representing a music track/song.
 */
export class Song {
    #id = '';
    #title = '';
    #artist = '';
    #artistId = null;
    #album = '';
    #albumId = null;
    #cover = '';
    #duration = 0;
    #previewUrl = '';

    /**
     * Creates a new Song instance.
     * @param {Object} data - Song raw properties
     */
    constructor(data = {}) {
        this.#id = String(data.id || '');
        this.#title = data.title || 'Unknown Title';
        this.#artist = data.artist || 'Unknown Artist';
        this.#artistId = data.artistId || null;
        this.#album = data.album || '';
        this.#albumId = data.albumId || null;
        this.#cover = data.cover || 'svg/album-placeholder.svg';
        this.#duration = Number(data.duration) || 0;
        this.#previewUrl = data.previewUrl || '';
    }

    /** Gets song unique identifier */
    get id() { return this.#id; }

    /** Gets song title */
    get title() { return this.#title; }

    /** Gets primary artist name */
    get artist() { return this.#artist; }

    /** Gets primary artist ID */
    get artistId() { return this.#artistId; }

    /** Gets album title */
    get album() { return this.#album; }

    /** Gets album ID */
    get albumId() { return this.#albumId; }

    /** Gets cover artwork URL */
    get cover() { return this.#cover; }

    /** Gets duration in seconds */
    get duration() { return this.#duration; }

    /** Gets preview audio stream URL */
    get previewUrl() { return this.#previewUrl; }

    /** Gets item type identifier */
    get type() { return 'song'; }

    /**
     * Returns formatted mm:ss duration string.
     * @returns {string} Formatted duration
     */
    get formattedDuration() {
        if (!this.#duration) return '--:--';
        const minutes = Math.floor(this.#duration / 60);
        const seconds = Math.floor(this.#duration % 60);
        return `${minutes}:${seconds < 10 ? '0' : ''}${seconds}`;
    }

    /**
     * Converts domain model to plain JSON object representation.
     * @returns {Object} Plain object
     */
    toJSON() {
        return {
            type: 'song',
            id: this.#id,
            title: this.#title,
            artist: this.#artist,
            artistId: this.#artistId,
            album: this.#album,
            albumId: this.#albumId,
            cover: this.#cover,
            duration: this.#duration,
            previewUrl: this.#previewUrl
        };
    }
}
