/**
 * Debouncer provides utility methods for debouncing and throttling execution.
 */
export class Debouncer {
    /**
     * Creates a debounced version of a function that delays execution.
     * @param {Function} fn - Function to debounce.
     * @param {number} wait - Delay in milliseconds.
     * @returns {Function}
     */
    static debounce(fn, wait = 300) {
        let timeout;
        return function (...args) {
            clearTimeout(timeout);
            timeout = setTimeout(() => fn.apply(this, args), wait);
        };
    }

    /**
     * Creates a throttled version of a function that limits execution frequency.
     * @param {Function} fn - Function to throttle.
     * @param {number} limit - Minimum interval in milliseconds between calls.
     * @returns {Function}
     */
    static throttle(fn, limit = 200) {
        let inThrottle;
        return function (...args) {
            if (!inThrottle) {
                fn.apply(this, args);
                inThrottle = true;
                setTimeout(() => { inThrottle = false; }, limit);
            }
        };
    }
}
