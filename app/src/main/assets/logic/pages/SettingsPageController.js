import { BasePageController } from './BasePageController.js';
import { LanguageManager } from '../language-manager.js';

/**
 * SettingsPageController manages application settings, theme toggles, and i18n language options.
 */
export class SettingsPageController extends BasePageController {
    /**
     * Renders settings view.
     * @param {Object} params
     */
    async render(params = {}) {
        this.resetAbortController();
        this.bindEvents();
    }

    /** Binds settings UI interaction handlers */
    bindEvents() {
        LanguageManager.bindLanguageUI();
    }
}
