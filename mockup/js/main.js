/**
 * Mockup Interactive Controller
 * Demonstrates offline interactive UI behavior, skeleton-to-content toggling,
 * button state variations, and player mock state.
 */

// Global state container for mockup demonstration
const MockupState = {
    isLoading: false,
    isPlaying: false,
    currentTrack: {
        title: "Synthwave Dreams",
        artist: "Neon Skyline",
        cover: "https://picsum.photos/200/200?random=1"
    }
};

/**
 * Toggles between Skeleton loading state and Live Content state
 * Proves 1:1 skeleton layout matching without page jumpiness.
 */
function toggleSkeletonView() {
    MockupState.isLoading = !MockupState.isLoading;
    const skeletonElements = document.querySelectorAll('.skeleton-view');
    const liveElements = document.querySelectorAll('.live-view');
    const toggleBtn = document.getElementById('btn-toggle-skeleton');

    if (MockupState.isLoading) {
        skeletonElements.forEach(el => el.classList.remove('is-hidden'));
        liveElements.forEach(el => el.classList.add('is-hidden'));
        if (toggleBtn) toggleBtn.textContent = 'Show Live Content';
    } else {
        skeletonElements.forEach(el => el.classList.add('is-hidden'));
        liveElements.forEach(el => el.classList.remove('is-hidden'));
        if (toggleBtn) toggleBtn.textContent = 'Show Skeleton UI';
    }
}

/**
 * Toggles mock playback state for the player bar demonstration
 */
function togglePlayPause() {
    MockupState.isPlaying = !MockupState.isPlaying;
    const playIcon = document.getElementById('player-play-icon');
    if (playIcon) {
        playIcon.src = MockupState.isPlaying ? 'svg/close.svg' : 'svg/play.svg';
    }
}

/**
 * Initializes listeners once DOM is ready
 */
document.addEventListener('DOMContentLoaded', () => {
    // Attach skeleton toggle handler
    const skeletonBtn = document.getElementById('btn-toggle-skeleton');
    if (skeletonBtn) {
        skeletonBtn.addEventListener('click', toggleSkeletonView);
    }

    // Attach play toggle handler
    const playBtn = document.getElementById('btn-play-pause');
    if (playBtn) {
        playBtn.addEventListener('click', togglePlayPause);
    }
});
