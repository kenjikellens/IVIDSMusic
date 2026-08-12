import { BaseService } from '../core/BaseService.js';

/**
 * BasePageController defines the lifecycle contract for individual page controllers.
 */
export class BasePageController extends BaseService {
    #container = null;
    #abortController = null;
    #eventListeners = [];

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
     * Binds a container-level delegated event listener for high performance.
     * @param {HTMLElement|string} target - Target element or selector string
     * @param {string} eventType - Event type (e.g. 'click')
     * @param {string} selector - Selector to match child target (e.g. '.music-card')
     * @param {Function} handler - Event handler function (element, event)
     */
    bindContainerEvent(target, eventType, selector, handler) {
        const containerEl = typeof target === 'string' ? document.querySelector(target) : target;
        if (!containerEl) return;

        const listener = (event) => {
            const matchedEl = event.target.closest(selector);
            if (matchedEl && containerEl.contains(matchedEl)) {
                handler(matchedEl, event);
            }
        };

        containerEl.addEventListener(eventType, listener);
        this.#eventListeners.push({ element: containerEl, type: eventType, listener });
    }

    /**
     * Removes all container-level event listeners bound by this controller.
     */
    clearContainerEvents() {
        this.#eventListeners.forEach(({ element, type, listener }) => {
            if (element) element.removeEventListener(type, listener);
        });
        this.#eventListeners = [];
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
        this.clearContainerEvents();
        if (this.#abortController) {
            this.#abortController.abort();
            this.#abortController = null;
        }
        super.destroy();
    }
}

