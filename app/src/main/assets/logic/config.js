/**
 * EnvironmentConfig manages environment detection, platform flags, and server URLs.
 */
export class EnvironmentConfig {
    #isNative;
    #isElectron;
    #isWeb;
    #serverUrl;

    /**
     * Constructs and evaluates the current application runtime environment.
     */
    constructor() {
        const hasWindow = typeof window !== 'undefined';
        this.#isNative = hasWindow && window.location.hostname.includes("appassets.androidplatform.net");
        this.#isElectron = hasWindow && window.navigator && window.navigator.userAgent.toLowerCase().includes("electron");
        this.#isWeb = !this.#isNative && !this.#isElectron;

        let baseServerUrl = "http://127.0.0.1:3000";
        if (hasWindow && (window.location.protocol === 'http:' || window.location.protocol === 'https:')) {
            baseServerUrl = `${window.location.protocol}//${window.location.hostname}:3000`;
        }
        this.#serverUrl = this.#isNative ? "/api" : baseServerUrl;
    }

    /** Returns true if running in Android Native WebView */
    get isNative() { return this.#isNative; }

    /** Returns true if running in Electron PC Desktop */
    get isElectron() { return this.#isElectron; }

    /** Returns true if running in Web / Browser environment */
    get isWeb() { return this.#isWeb; }

    /** Returns the base server proxy URL */
    get SERVER_URL() { return this.#serverUrl; }
}

/** Config singleton instance */
export const Config = new EnvironmentConfig();

if (typeof window !== 'undefined') {
    window.Config = Config;
}
