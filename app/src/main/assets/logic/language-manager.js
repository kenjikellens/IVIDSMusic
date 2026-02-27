/**
 * IVIDS Music - Language Manager
 * Handles internationalization and dynamic UI translation.
 */
export const LanguageManager = {
    SUPPORTED_LANGUAGES: ['en', 'nl', 'fr'],
    defaultLanguage: 'en',
    currentLanguage: 'en',
    translations: {},

    /**
     * Initialize LanguageManager
     */
    async init() {
        const saved = localStorage.getItem('iv_language');
        this.currentLanguage = saved && this.SUPPORTED_LANGUAGES.includes(saved) ? saved : this.defaultLanguage;

        await this.loadTranslations(this.currentLanguage);
        this.translateUI();

        console.log(`[LanguageManager] Initialized with language: ${this.currentLanguage}`);
    },

    /**
     * Load translations from JSON file
     * @param {string} lang 
     */
    async loadTranslations(lang) {
        try {
            // Remove query params which can break file:/// loading in some Android WebViews
            const response = await fetch(`lang/${lang}.json`);
            if (!response.ok) throw new Error(`Could not load translations for: ${lang}`);
            this.translations = await response.json();
            this.currentLanguage = lang;
            localStorage.setItem('iv_language', lang);
        } catch (error) {
            console.error('[LanguageManager] Error loading translations:', error);
            // Fallback to English if not already failed
            if (lang !== 'en') await this.loadTranslations('en');
        }
    },

    /**
     * Translate all elements with data-i18n attribute
     * @param {HTMLElement} root - Optional root element to search within
     */
    translateUI(root = document) {
        // 1. Text content and basic placeholder
        const elements = root.querySelectorAll('[data-i18n]');
        elements.forEach(el => {
            const key = el.getAttribute('data-i18n');
            const translation = this.translations[key];
            if (translation) {
                if (el.tagName === 'INPUT' && (el.type === 'text' || el.type === 'search' || el.type === 'number')) {
                    el.placeholder = translation;
                } else if (el.hasAttribute('title')) {
                    el.title = translation;
                } else {
                    // Only replace text content if no children, to avoid losing icons
                    if (el.children.length === 0) {
                        el.textContent = translation;
                    }
                }
            }
        });

        // 2. Explicit title translation
        root.querySelectorAll('[data-i18n-title]').forEach(el => {
            const key = el.getAttribute('data-i18n-title');
            if (this.translations[key]) el.title = this.translations[key];
        });

        // 3. Explicit placeholder translation
        root.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
            const key = el.getAttribute('data-i18n-placeholder');
            if (this.translations[key]) el.placeholder = this.translations[key];
        });
    },

    /**
     * Change the application language
     * @param {string} lang 
     */
    async setLanguage(lang) {
        if (!this.SUPPORTED_LANGUAGES.includes(lang)) {
            console.warn(`[LanguageManager] Language ${lang} not supported.`);
            return;
        }

        await this.loadTranslations(lang);
        this.translateUI();

        // Update settings UI if present
        this.updateLanguageButtons(lang);

        // Dispatch event for components that might need manual adjustment
        window.dispatchEvent(new CustomEvent('iv-language-changed', { detail: { language: lang } }));
    },

    /**
     * Update active state for language buttons in the settings UI
     */
    updateLanguageButtons(value) {
        try {
            const buttons = document.querySelectorAll('.lang-btn[data-lang]');
            buttons.forEach(btn => {
                if (btn.getAttribute('data-lang') === value) {
                    btn.classList.add('active');
                } else {
                    btn.classList.remove('active');
                }
            });
        } catch (e) { }
    },

    /**
     * Bind click handlers for language buttons in settings UI
     */
    bindLanguageUI() {
        try {
            const container = document.getElementById('language-options-container');
            if (!container) return;

            // Generate buttons if they don't exist? No, let's assume they are in the HTML
            // but we can make it dynamic here if we want.

            container.addEventListener('click', (ev) => {
                const btn = ev.target.closest && ev.target.closest('.lang-btn');
                if (!btn) return;
                const lang = btn.getAttribute('data-lang');
                if (lang) this.setLanguage(lang);
            });

            this.updateLanguageButtons(this.currentLanguage);
        } catch (e) { }
    }
};

// Global access
if (typeof window !== 'undefined') {
    window.LanguageManager = LanguageManager;
}
