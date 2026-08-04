import { BaseService } from '../core/BaseService.js';

/**
 * BasePageController defines the lifecycle contract for individual page controllers.
 */
export class BasePageController extends BaseService {
    #container = null;
    #abortController = null;

    /** Returns active container DOM element */
    get container() { return this.#container; }
    set container(el) { this.#container = el; }

    /** Returns active AbortSignal */
    get signal() {
        if (!this.#abortController) {
            this.#abortController = new AbortController();
        }
        return this.#abortController.signal;
    }

    /**
     * Resets abort controller and cancels active fetch requests for this page.
     */
    resetAbortController() {
        if (this.#abortController) {
            this.#abortController.abort();
        }
        this.#abortController = new AbortController();
    }

    /**
     * Abstract render method called when navigating to page.
     * @param {Object} [params] - Navigation parameters.
     * @returns {Promise<void>|void}
     */
    async render(params = {}) {
        throw new Error("Method 'render()' must be implemented by concrete PageController subclass.");
    }

    /**
     * Abstract event binding method.
     */
    bindEvents() {
        // Optional override by subclasses
    }

    /**
     * Destroys page controller instance, cleaning up event listeners and active requests.
     */
    destroy() {
        if (this.#abortController) {
            this.#abortController.abort();
            this.#abortController = null;
        }
        super.destroy();
    }
}
