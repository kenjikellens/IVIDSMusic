/**
 * Domain entity representing a music artist.
 */
export class Artist {
    #id = '';
    #name = '';
    #cover = '';
    #genre = '';
    #fanCount = 0;

    /**
     * Creates a new Artist instance.
     * @param {Object} data - Artist raw properties
     */
    constructor(data = {}) {
        this.#id = String(data.id || '');
        this.#name = data.name || 'Unknown Artist';
        this.#cover = data.cover || 'svg/user.svg';
        this.#genre = data.genre || 'Artist';
        this.#fanCount = Number(data.fanCount) || 0;
    }

    /** Gets artist unique identifier */
    get id() { return this.#id; }

    /** Gets artist name */
    get name() { return this.#name; }

    /** Gets cover/picture URL */
    get cover() { return this.#cover; }

    /** Gets genre title */
    get genre() { return this.#genre; }

    /** Gets total fan count */
    get fanCount() { return this.#fanCount; }

    /** Gets item type identifier */
    get type() { return 'artist'; }

    /**
     * Converts domain model to plain JSON object representation.
     * @returns {Object} Plain object
     */
    toJSON() {
        return {
            type: 'artist',
            id: this.#id,
            name: this.#name,
            cover: this.#cover,
            genre: this.#genre,
            fanCount: this.#fanCount
        };
    }
}
