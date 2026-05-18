/**
 * IVIDS Music - TV Spatial Navigation Engine
 * Intercepts D-pad arrow keys to move focus geometrically between elements.
 */

export const TVNav = {
    isEnabled: false,
    focusedElement: null,

    // Focusable selector
    selector: 'a, button, input, [tabindex="0"]',

    /**
     * Method: init
     * Description: Auto-detects the TV environment and, if present, registers the global keydown event listener
     *              for spatial D-pad navigation and applies the TV-mode body styling classes.
     */
    init() {
        // Auto-detect TV environment via UserAgent or explicit developer URL overrides
        const isTV = /tv|smarttv|googletv|appletv|hbbtv|nintendo|playstation/i.test(navigator.userAgent) || 
                     window.location.search.includes('tvMode=true');
        
        if (!isTV) {
            console.log('[TVNav] Non-TV environment detected. Spatial navigation disabled.');
            this.isEnabled = false;
            return;
        }

        console.log('[TVNav] Initializing Spatial Navigation');
        this.isEnabled = true;
        document.body.classList.add('tv-mode');

        window.addEventListener('keydown', this.handleKeyDown.bind(this));

        // Setup initial focus
        setTimeout(() => this.reinitFocus(), 500);
    },

    /**
     * Method: reinitFocus
     * Description: Scans the active document for focusable elements, configures TV-mode input states
     *              (making text inputs readOnly by default to suppress soft keyboard popups),
     *              and restores focus to the active element or falls back to standard shell links.
     */
    reinitFocus() {
        if (!this.isEnabled) return;

        // Make all text inputs readOnly by default in TV mode to prevent auto-keyboard popups
        document.querySelectorAll('input').forEach(input => {
            if (input.type === 'text' || input.type === 'number' || input.type === 'search') {
                input.readOnly = true;
                
                // Add blur listener if not already added to restore readOnly
                if (!input.dataset.tvBound) {
                    input.dataset.tvBound = 'true';
                    input.addEventListener('blur', () => {
                        if (this.isEnabled) input.readOnly = true;
                    });
                }
            }
        });

        // Find all focusable elements
        const elements = this.getFocusableElements();
        if (elements.length === 0) return;

        // If nothing is focused, or the currently focused element is no longer in DOM
        if (!document.activeElement || document.activeElement === document.body || !document.contains(document.activeElement)) {
            // Prefer sidebar home, or just the first element
            const homeBtn = document.getElementById('nav-home');
            if (homeBtn && this.isElementVisible(homeBtn)) {
                this.setFocus(homeBtn);
            } else {
                for (let el of elements) {
                    if (this.isElementVisible(el)) {
                        this.setFocus(el);
                        break;
                    }
                }
            }
        } else {
            this.focusedElement = document.activeElement;
        }
    },

    getFocusableElements() {
        return Array.from(document.querySelectorAll(this.selector))
            .filter(el => this.isElementVisible(el));
    },

    isElementVisible(el) {
        if (!el) return false;
        const rect = el.getBoundingClientRect();
        const style = window.getComputedStyle(el);
        return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none' && el.tabIndex >= 0;
    },

    setFocus(el) {
        if (!el) return;
        el.focus();
        this.focusedElement = el;

        // Smooth scroll into view with padding
        el.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'center' });
    },

    getCenter(rect) {
        return {
            x: rect.left + rect.width / 2,
            y: rect.top + rect.height / 2
        };
    },

    navigate(direction) {
        if (!this.focusedElement || !document.contains(this.focusedElement)) {
            this.reinitFocus();
            return;
        }

        const currentRect = this.focusedElement.getBoundingClientRect();
        const elements = this.getFocusableElements();

        let bestMatch = null;
        let minDistance = Infinity;

        for (let el of elements) {
            if (el === this.focusedElement) continue;

            const rect = el.getBoundingClientRect();
            let isEligible = false;
            let distance = Infinity;

            // Define overlap to prioritize elements that align along the cross-axis
            let xOverlap = Math.max(0, Math.min(currentRect.right, rect.right) - Math.max(currentRect.left, rect.left));
            let yOverlap = Math.max(0, Math.min(currentRect.bottom, rect.bottom) - Math.max(currentRect.top, rect.top));

            switch (direction) {
                case 'right':
                    // Candidate left edge must be to the right of current center
                    if (rect.left >= currentRect.left + (currentRect.width / 2)) {
                        isEligible = true;
                        // Distance = horizontal gap + geometric cross distance penalty
                        let hGap = rect.left - currentRect.right;
                        if (hGap < 0) hGap = 0; // Overlapping horizontally
                        let vCenterDist = Math.abs((rect.top + rect.height / 2) - (currentRect.top + currentRect.height / 2));
                        distance = (hGap * hGap) + (vCenterDist * vCenterDist * 2);
                        if (yOverlap > 0) distance -= yOverlap * 10; // Bonus for overlapping Y bounds
                    }
                    break;
                case 'left':
                    if (rect.right <= currentRect.left + (currentRect.width / 2)) {
                        isEligible = true;
                        let hGap = currentRect.left - rect.right;
                        if (hGap < 0) hGap = 0;
                        let vCenterDist = Math.abs((rect.top + rect.height / 2) - (currentRect.top + currentRect.height / 2));
                        distance = (hGap * hGap) + (vCenterDist * vCenterDist * 2);
                        if (yOverlap > 0) distance -= yOverlap * 10;
                    }
                    break;
                case 'down':
                    if (rect.top >= currentRect.top + (currentRect.height / 2)) {
                        isEligible = true;
                        let vGap = rect.top - currentRect.bottom;
                        if (vGap < 0) vGap = 0;
                        let hCenterDist = Math.abs((rect.left + rect.width / 2) - (currentRect.left + currentRect.width / 2));
                        distance = (vGap * vGap) + (hCenterDist * hCenterDist * 2);
                        if (xOverlap > 0) distance -= xOverlap * 10;
                    }
                    break;
                case 'up':
                    if (rect.bottom <= currentRect.top + (currentRect.height / 2)) {
                        isEligible = true;
                        let vGap = currentRect.top - rect.bottom;
                        if (vGap < 0) vGap = 0;
                        let hCenterDist = Math.abs((rect.left + rect.width / 2) - (currentRect.left + currentRect.width / 2));
                        distance = (vGap * vGap) + (hCenterDist * hCenterDist * 2);
                        if (xOverlap > 0) distance -= xOverlap * 10;
                    }
                    break;
            }

            if (isEligible && distance < minDistance) {
                minDistance = distance;
                bestMatch = el;
            }
        }

        if (bestMatch) {
            this.setFocus(bestMatch);
        }
    },

    /**
     * Method: handleKeyDown
     * Description: Global keyboard event listener that intercepts spatial navigation direction keys,
     *              handles input activation on Enter clicks, and maps hardware Back actions.
     * @param {KeyboardEvent} e - The keydown event captured from the window.
     */
    handleKeyDown(e) {
        if (!this.isEnabled) return;

        switch (e.key) {
            case 'ArrowUp':
                e.preventDefault();
                this.navigate('up');
                break;
            case 'ArrowDown':
                e.preventDefault();
                this.navigate('down');
                break;
            case 'ArrowLeft':
                e.preventDefault();
                this.navigate('left');
                break;
            case 'ArrowRight':
                e.preventDefault();
                this.navigate('right');
                break;
            case 'Enter':
                // For input elements, Enter should unlock readOnly to show soft keyboard
                if (document.activeElement && document.activeElement.tagName.toLowerCase() === 'input') {
                    const input = document.activeElement;
                    if (input.readOnly) {
                        input.readOnly = false;
                        input.focus();
                        console.log('[TVNav] Input write-mode unlocked on Enter key.');
                        e.preventDefault();
                    }
                    return;
                }

                // For everything else, click it
                if (document.activeElement && document.activeElement !== document.body) {
                    e.preventDefault();
                    document.activeElement.click();
                }
                break;
            case 'Escape':
                // Prevent going back out of app if possible
                if (document.activeElement && document.activeElement.tagName.toLowerCase() === 'input') {
                    document.activeElement.blur();
                    e.preventDefault();
                    return;
                }
                break;
            case 'Backspace':
            case 'BrowserBack':
                // Let inputs handle their own backspace (delete text) natively
                if (document.activeElement && document.activeElement.tagName.toLowerCase() === 'input') {
                    return;
                }
                break;
        }
    }
};

// Global instance
window.TVNav = TVNav;
