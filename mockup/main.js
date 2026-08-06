/**
 * IVIDS Music - Developer Mode Updater Mockup Logic
 */

document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const devToggle = document.getElementById('dev-mode-toggle');
    const devBadge = document.getElementById('dev-mode-badge');
    const devPanel = document.getElementById('dev-options-panel');
    const radioReleases = document.getElementById('radio-releases');
    const radioMain = document.getElementById('radio-main');
    const versionSelect = document.getElementById('dev-version-select');
    const versionSelectContainer = document.getElementById('version-select-container');
    const openDevUpdaterBtn = document.getElementById('btn-open-dev-updater');
    const checkUpdatesBtn = document.getElementById('check-updates-btn');
    
    const modalOverlay = document.getElementById('update-overlay');
    const modalCloseBtn = document.getElementById('modal-close-btn');
    const modalInstallBtn = document.getElementById('modal-install-btn');
    const modalTargetBadge = document.getElementById('modal-target-badge');
    const modalReleaseName = document.getElementById('modal-release-name');
    const modalChangelogBody = document.getElementById('modal-changelog-body');
    const modalSubtitle = document.getElementById('modal-subtitle');

    // State
    let isDevMode = false;
    let selectedMode = 'releases'; // 'releases' | 'main'
    let selectedVersion = 'v0.2.3';

    // Mock Release Data
    const mockReleases = {
        'v0.2.3': {
            name: 'Release v0.2.3 (Beta)',
            url: 'https://github.com/kenjikellens/IVIDSMusic/releases/tag/v0.2.3',
            body: '• Performance and core interface enhancements.<br>• Native WebView audio proxy optimizations.<br>• Enhanced TV navigation support.'
        },
        'v0.2.2': {
            name: 'Release v0.2.2 (Beta)',
            url: 'https://github.com/kenjikellens/IVIDSMusic/releases/tag/v0.2.2',
            body: '• Updated Android APK binary output tags.<br>• Fixed audio queue index progression.<br>• Added multi-language translation support.'
        },
        'v0.2.1': {
            name: 'Release v0.2.1 (Beta)',
            url: 'https://github.com/kenjikellens/IVIDSMusic/releases/tag/v0.2.1',
            body: '• Desktop Electron installer integration.<br>• Initial offline IndexedDB storage implementation.'
        },
        'v0.2.0': {
            name: 'Release v0.2.0 (Alpha)',
            url: 'https://github.com/kenjikellens/IVIDSMusic/releases/tag/v0.2.0',
            body: '• Initial pre-release build of IVIDS Music engine.'
        }
    };

    // Toggle Dev Mode
    devToggle.addEventListener('change', (e) => {
        isDevMode = e.target.checked;
        if (isDevMode) {
            devBadge.style.display = 'inline-block';
            devPanel.classList.add('visible');
        } else {
            devBadge.style.display = 'none';
            devPanel.classList.remove('visible');
        }
    });

    // Radio selection
    radioReleases.addEventListener('change', () => {
        selectedMode = 'releases';
        versionSelectContainer.style.opacity = '1';
        versionSelect.disabled = false;
    });

    radioMain.addEventListener('change', () => {
        selectedMode = 'main';
        versionSelectContainer.style.opacity = '0.5';
        versionSelect.disabled = true;
    });

    // Version dropdown selection
    versionSelect.addEventListener('change', (e) => {
        selectedVersion = e.target.value;
    });

    // Open Modal
    function openModal() {
        if (selectedMode === 'main') {
            modalTargetBadge.textContent = 'MAIN BRANCH (LATEST BUILD)';
            modalTargetBadge.className = 'version-badge main-branch';
            modalReleaseName.textContent = 'Main Branch Executable File';
            modalSubtitle.textContent = 'Source: Raw GitHub main branch root';
            modalChangelogBody.innerHTML = `
                • Target URL: https://raw.githubusercontent.com/kenjikellens/IVIDSMusic/main/IVIDSMusic_PC.exe<br>
                • Platform Target: PC Executable (IVIDSMusic_PC.exe)<br>
                • Description: Bleeding edge development build directly from the latest git commit on main branch.<br>
                • Note: Replaces local build with raw repository main file.
            `;
        } else {
            const rel = mockReleases[selectedVersion] || mockReleases['v0.2.3'];
            modalTargetBadge.textContent = selectedVersion;
            modalTargetBadge.className = 'version-badge remote';
            modalReleaseName.textContent = rel.name;
            modalSubtitle.textContent = 'Source: GitHub Releases API';
            modalChangelogBody.innerHTML = `
                • Target URL: ${rel.url}<br>
                • Version Tag: ${selectedVersion}<br>
                ${rel.body}
            `;
        }

        modalOverlay.style.display = 'flex';
    }

    openDevUpdaterBtn.addEventListener('click', openModal);

    checkUpdatesBtn.addEventListener('click', () => {
        if (isDevMode) {
            openModal();
        } else {
            alert('Standard Update Check:\n\nYou are on version 0.2.3 (Latest). No newer releases published.');
        }
    });

    // Close Modal
    modalCloseBtn.addEventListener('click', () => {
        modalOverlay.style.display = 'none';
    });

    // Install Action Simulation
    modalInstallBtn.addEventListener('click', () => {
        modalInstallBtn.disabled = true;
        modalInstallBtn.querySelector('span').textContent = 'Downloading (0%)...';
        
        let progress = 0;
        const interval = setInterval(() => {
            progress += 25;
            if (progress <= 100) {
                modalInstallBtn.querySelector('span').textContent = `Downloading (${progress}%)...`;
            } else {
                clearInterval(interval);
                modalInstallBtn.querySelector('span').textContent = 'Launching Installer...';
                setTimeout(() => {
                    alert('Mock Download Complete!\n\nThe update process was triggered successfully.');
                    modalInstallBtn.disabled = false;
                    modalInstallBtn.querySelector('span').textContent = 'Install Selected Target';
                    modalOverlay.style.display = 'none';
                }, 800);
            }
        }, 300);
    });
});
