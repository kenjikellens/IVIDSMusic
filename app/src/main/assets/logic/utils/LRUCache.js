/**
 * LRUCache provides a Least Recently Used in-memory cache mechanism.
 */
export class LRUCache {
    #capacity;
    #cache = new Map();

    /**
     * Constructs an LRU cache with the specified capacity limit.
     * @param {number} capacity - Maximum number of items allowed in cache.
     */
    constructor(capacity = 50) {
        this.#capacity = capacity;
    }

    /**
     * Retrieves an item from cache and marks it as recently used.
     * @param {string} key
     * @returns {*} Cached value or undefined.
     */
    get(key) {
        if (!this.#cache.has(key)) return undefined;
        const val = this.#cache.get(key);
        this.#cache.delete(key);
        this.#cache.set(key, val);
        return val;
    }

    /**
     * Checks if a key exists in cache.
     * @param {string} key
     * @returns {boolean}
     */
    has(key) {
        return this.#cache.has(key);
    }

    /**
     * Stores a key-value pair in cache, evicting the oldest entry if over capacity.
     * @param {string} key
     * @param {*} value
     */
    set(key, value) {
        if (this.#cache.has(key)) {
            this.#cache.delete(key);
        } else if (this.#cache.size >= this.#capacity) {
            const firstKey = this.#cache.keys().next().value;
            this.#cache.delete(firstKey);
        }
        this.#cache.set(key, value);
    }

    /**
     * Deletes a specific item from cache.
     * @param {string} key
     */
    delete(key) {
        this.#cache.delete(key);
    }

    /**
     * Clears all items from cache.
     */
    clear() {
        this.#cache.clear();
    }
}
