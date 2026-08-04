import { BaseService } from './core/BaseService.js';

/**
 * LanguageManagerService handles internationalization, translations, and dynamic UI localized strings.
 */
export class LanguageManagerService extends BaseService {
    SUPPORTED_LANGUAGES = ['en', 'nl', 'fr', 'de', 'es', 'pt', 'it', 'zh', 'hi', 'ar', 'ru', 'ro', 'ja', 'tr', 'ko', 'pl'];
    
    LANGUAGE_NAMES = {
        en: 'English',
        nl: 'Nederlands',
        fr: 'Français',
        de: 'Deutsch',
        es: 'Español',
        pt: 'Português',
        it: 'Italiano',
        zh: 'Chinese (简体中文)',
        hi: 'Hindi (हिन्दी)',
        ar: 'Arabic (العربية)',
        ru: 'Russian (Русский)',
        ro: 'Romanian (Română)',
        ja: 'Japanese (日本語)',
        tr: 'Turkish (Türkçe)',
        ko: 'Korean (한국어)',
        pl: 'Polish (Polski)'
    };

    #defaultLanguage = 'en';
    #currentLanguage = 'en';
    #translations = {};
    #langListenerBound = false;

    /** Returns current active language code */
    get currentLanguage() {
        return this.#currentLanguage;
    }

    /** Returns current dictionary object */
    get translations() {
        return this.#translations;
    }

    /**
     * Initializes LanguageManagerService, loading saved preferences or defaults.
     */
    async init() {
        if (this.isInitialized) return;

        const saved = localStorage.getItem('iv_language');
        this.#currentLanguage = saved && this.SUPPORTED_LANGUAGES.includes(saved) ? saved : this.#defaultLanguage;

        await this.loadTranslations(this.#currentLanguage);
        this.translateUI();
        this._setInitialized(true);

        console.log(`[LanguageManager] Initialized with language: ${this.#currentLanguage}`);
    }

    /**
     * Loads translation keys from target JSON file.
     * @param {string} lang - Target language code.
     */
    async loadTranslations(lang) {
        try {
            const response = await fetch(`lang/${lang}.json`);
            if (!response.ok) throw new Error(`Could not load translations for: ${lang}`);
            this.#translations = await response.json();
            this.#currentLanguage = lang;
            localStorage.setItem('iv_language', lang);
        } catch (error) {
            console.error('[LanguageManager] Error loading translations:', error);
            if (lang !== 'en') await this.loadTranslations('en');
        }
    }

    /**
     * Applies current translation dictionary to all matching DOM elements with data-i18n attributes.
     * @param {HTMLElement|Document} root - Target root element.
     */
    translateUI(root = document) {
        const elements = root.querySelectorAll('[data-i18n]');
        elements.forEach(el => {
            const key = el.getAttribute('data-i18n');
            const translation = this.#translations[key];
            if (translation) {
                if (el.tagName === 'INPUT' && (el.type === 'text' || el.type === 'search' || el.type === 'number')) {
                    el.placeholder = translation;
                } else if (el.hasAttribute('title')) {
                    el.title = translation;
                } else if (el.children.length === 0) {
                    el.textContent = translation;
                }
            }
        });

        root.querySelectorAll('[data-i18n-title]').forEach(el => {
            const key = el.getAttribute('data-i18n-title');
            if (this.#translations[key]) el.title = this.#translations[key];
        });

        root.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
            const key = el.getAttribute('data-i18n-placeholder');
            if (this.#translations[key]) el.placeholder = this.#translations[key];
        });
    }

    /**
     * Dynamically switches application active language.
     * @param {string} lang - Target language code.
     */
    async setLanguage(lang) {
        if (!this.SUPPORTED_LANGUAGES.includes(lang)) {
            console.warn(`[LanguageManager] Language ${lang} not supported.`);
            return;
        }

        await this.loadTranslations(lang);
        this.translateUI();
        this.updateLanguageDisplay();
        this.emit('languageChanged', { language: lang });

        if (typeof window !== 'undefined') {
            window.dispatchEvent(new CustomEvent('iv-language-changed', { detail: { language: lang } }));
        }
    }

    /**
     * Updates language display element in Settings.
     */
    updateLanguageDisplay() {
        try {
            const display = document.getElementById('current-lang-display');
            if (display) {
                display.textContent = this.LANGUAGE_NAMES[this.#currentLanguage] || this.#currentLanguage;
            }
        } catch (e) { }
    }

    /**
     * Opens language selection modal with accessible options.
     */
    openLanguageModal() {
        const overlay = document.getElementById('lang-modal-overlay');
        const list = document.getElementById('lang-modal-list');
        if (!overlay || !list) return;

        list.innerHTML = '';
        const langCount = this.SUPPORTED_LANGUAGES.length;
        const cols = langCount > 7 ? (langCount > 14 ? 3 : 2) : 1;
        const rows = Math.ceil(langCount / cols);
        list.style.setProperty('--lang-rows', rows);
        list.style.setProperty('--lang-cols', cols);

        this.SUPPORTED_LANGUAGES.forEach(lang => {
            const btn = document.createElement('button');
            btn.className = 'lang-option' + (lang === this.#currentLanguage ? ' active' : '');
            btn.setAttribute('data-lang', lang);
            btn.setAttribute('tabindex', '0');
            btn.innerHTML = `
                <span>${this.LANGUAGE_NAMES[lang]}</span>
                <span class="lang-check">✓</span>
            `;
            list.appendChild(btn);
        });

        overlay.style.display = 'flex';
        const firstOption = list.querySelector('.lang-option');
        if (firstOption) setTimeout(() => firstOption.focus(), 50);
    }

    /**
     * Closes language selection modal.
     */
    closeLanguageModal() {
        const overlay = document.getElementById('lang-modal-overlay');
        if (overlay) overlay.style.display = 'none';
    }

    /**
     * Binds document-level event delegation for language UI interactions.
     */
    bindLanguageUI() {
        try {
            if (this.#langListenerBound) return;

            document.body.addEventListener('click', (ev) => {
                const target = ev.target;
                if (target.closest && target.closest('#lang-edit-btn')) {
                    this.openLanguageModal();
                    return;
                }
                if (target.closest && target.closest('#lang-modal-close')) {
                    this.closeLanguageModal();
                    return;
                }
                const langOption = target.closest && target.closest('.lang-option[data-lang]');
                if (langOption) {
                    const lang = langOption.getAttribute('data-lang');
                    if (lang) {
                        this.setLanguage(lang);
                        this.closeLanguageModal();
                    }
                    return;
                }
                if (target.id === 'lang-modal-overlay') {
                    this.closeLanguageModal();
                }
            });

            this.#langListenerBound = true;
            this.updateLanguageDisplay();
        } catch (e) { }
    }
}

/** LanguageManager singleton instance */
export const LanguageManager = new LanguageManagerService();

if (typeof window !== 'undefined') {
    window.LanguageManager = LanguageManager;
}
