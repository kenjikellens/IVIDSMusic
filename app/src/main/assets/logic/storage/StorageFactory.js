import { Config } from '../config.js';
import { IndexedDBStorage } from '../indexeddb-storage.js';
import { AndroidStorageEngine } from './AndroidStorage.js';
import { ElectronStorageEngine } from './ElectronStorage.js';

/**
 * StorageFactory dynamically resolves and returns the active platform storage engine strategy.
 */
export class StorageFactory {
    static #androidEngine = null;
    static #electronEngine = null;

    /**
     * Resolves and returns the platform-specific storage engine singleton.
     * @returns {AbstractStorageEngine}
     */
    static getEngine() {
        if (Config.isNative) {
            if (!this.#androidEngine) this.#androidEngine = new AndroidStorageEngine();
            return this.#androidEngine;
        }
        if (Config.isElectron) {
            if (!this.#electronEngine) this.#electronEngine = new ElectronStorageEngine();
            return this.#electronEngine;
        }
        return IndexedDBStorage;
    }
}
