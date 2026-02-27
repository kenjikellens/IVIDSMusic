/**
 * IVIDS Music - Language Manager
 * Handles internationalization and dynamic UI translation.
 */
export const LanguageManager = {
    SUPPORTED_LANGUAGES: ['en', 'nl', 'fr', 'de', 'es', 'pt', 'it'],
    LANGUAGE_NAMES: {
        en: 'English',
        nl: 'Nederlands',
        fr: 'Français',
        de: 'Deutsch',
        es: 'Español',
        pt: 'Português',
        it: 'Italiano'
    },
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
            const response = await fetch(`lang/${lang}.json`);
            if (!response.ok) throw new Error(`Could not load translations for: ${lang}`);
            this.translations = await response.json();
            this.currentLanguage = lang;
            localStorage.setItem('iv_language', lang);
        } catch (error) {
            console.error('[LanguageManager] Error loading translations:', error);
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
        this.updateLanguageDisplay();

        // Dispatch event for components that might need manual adjustment
        window.dispatchEvent(new CustomEvent('iv-language-changed', { detail: { language: lang } }));
    },

    /**
     * Update the current language display text in settings
     */
    updateLanguageDisplay() {
        try {
            const display = document.getElementById('current-lang-display');
            if (display) {
                display.textContent = this.LANGUAGE_NAMES[this.currentLanguage] || this.currentLanguage;
            }
        } catch (e) { }
    },

    /**
     * Open the language selection modal and populate options
     */
    openLanguageModal() {
        const overlay = document.getElementById('lang-modal-overlay');
        const list = document.getElementById('lang-modal-list');
        if (!overlay || !list) return;

        // Generate language option buttons
        list.innerHTML = '';
        this.SUPPORTED_LANGUAGES.forEach(lang => {
            const btn = document.createElement('button');
            btn.className = 'lang-option' + (lang === this.currentLanguage ? ' active' : '');
            btn.setAttribute('data-lang', lang);
            btn.setAttribute('tabindex', '0');
            btn.innerHTML = `
                <span>${this.LANGUAGE_NAMES[lang]}</span>
                <span class="lang-check">✓</span>
            `;
            list.appendChild(btn);
        });

        overlay.style.display = 'flex';

        // Focus the first option for TV/keyboard accessibility
        const firstOption = list.querySelector('.lang-option');
        if (firstOption) setTimeout(() => firstOption.focus(), 50);
    },

    /**
     * Close the language selection modal
     */
    closeLanguageModal() {
        const overlay = document.getElementById('lang-modal-overlay');
        if (overlay) overlay.style.display = 'none';
    },

    /**
     * Bind click handlers for language UI using document-level event delegation
     */
    bindLanguageUI() {
        try {
            if (this._langListenerBound) return;

            document.body.addEventListener('click', (ev) => {
                const target = ev.target;

                // Edit button opens modal
                if (target.closest && target.closest('#lang-edit-btn')) {
                    this.openLanguageModal();
                    return;
                }

                // Close button closes modal
                if (target.closest && target.closest('#lang-modal-close')) {
                    this.closeLanguageModal();
                    return;
                }

                // Language option selects language
                const langOption = target.closest && target.closest('.lang-option[data-lang]');
                if (langOption) {
                    const lang = langOption.getAttribute('data-lang');
                    if (lang) {
                        this.setLanguage(lang);
                        this.closeLanguageModal();
                    }
                    return;
                }

                // Clicking the overlay backdrop (not the modal itself) closes it
                if (target.id === 'lang-modal-overlay') {
                    this.closeLanguageModal();
                }
            });

            this._langListenerBound = true;

            this.updateLanguageDisplay();
        } catch (e) { }
    }
};

// Global access
if (typeof window !== 'undefined') {
    window.LanguageManager = LanguageManager;
}

