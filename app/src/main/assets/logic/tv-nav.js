import { BaseService } from './core/BaseService.js';

/**
 * TVNavEngine provides Android TV spatial D-pad navigation.
 */
export class TVNavEngine extends BaseService {
    #isEnabled = false;
    #focusedElement = null;
    #selector = 'a, button, input, select, [tabindex="0"]';
    #boundKeyDownHandler = null;

    /** Returns spatial navigation state */
    get isEnabled() { return this.#isEnabled; }

    /**
     * Initializes spatial navigation and auto-detects TV environment.
     */
    async init() {
        if (this.isInitialized) return;

        window.addEventListener('keydown', (e) => {
            if (e.altKey && e.key.toLowerCase() === 't') {
                e.preventDefault();
                this.toggleMode();
            }
        });

        const isTV = /tv|smarttv|googletv|appletv|hbbtv|nintendo|playstation/i.test(navigator.userAgent) ||
                     window.location.search.includes('tvMode=true');

        if (isTV) {
            this.enable();
        }
        this._setInitialized(true);
    }

    /** Enables TV mode */
    enable() {
        if (this.#isEnabled) return;
        this.#isEnabled = true;
        document.body.classList.add('tv-mode');

        if (this.#boundKeyDownHandler) {
            window.removeEventListener('keydown', this.#boundKeyDownHandler);
        }
        this.#boundKeyDownHandler = this.#handleKeyDown.bind(this);
        window.addEventListener('keydown', this.#boundKeyDownHandler);
    }

    /** Disables TV mode */
    disable() {
        if (!this.#isEnabled) return;
        this.#isEnabled = false;
        document.body.classList.remove('tv-mode');
        if (this.#boundKeyDownHandler) {
            window.removeEventListener('keydown', this.#boundKeyDownHandler);
        }
    }

    /** Toggles TV mode */
    toggleMode() {
        if (this.#isEnabled) {
            this.disable();
        } else {
            this.enable();
        }
    }

    #handleKeyDown(e) {
        if (!this.#isEnabled) return;
        const keys = ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Enter'];
        if (keys.includes(e.key)) {
            this.emit('navigate', { key: e.key });
        }
    }

    /** Reinitializes focus on current page */
    reinitFocus() {
        if (!this.#isEnabled) return;
        const firstFocusable = document.querySelector(this.#selector);
        if (firstFocusable) firstFocusable.focus();
    }
}

/** TVNav singleton instance */
export const TVNav = new TVNavEngine();

if (typeof window !== 'undefined') {
    window.TVNav = TVNav;
}
