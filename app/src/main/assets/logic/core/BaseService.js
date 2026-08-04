import { EventEmitter } from './EventEmitter.js';

/**
 * Abstract BaseService class that establishes service lifecycle contracts.
 */
export class BaseService extends EventEmitter {
    #isInitialized = false;

    /**
     * Checks if the service is currently initialized.
     * @returns {boolean}
     */
    get isInitialized() {
        return this.#isInitialized;
    }

    /**
     * Protected setter for initialization status.
     * @param {boolean} value
     */
    _setInitialized(value) {
        this.#isInitialized = Boolean(value);
    }

    /**
     * Abstract init method to be overridden by concrete services.
     * @returns {Promise<void>|void}
     */
    async init() {
        if (this.#isInitialized) return;
        this.#isInitialized = true;
    }

    /**
     * Abstract destroy method for teardown and event listener cleanup.
     */
    destroy() {
        this.removeAllListeners();
        this.#isInitialized = false;
    }
}
