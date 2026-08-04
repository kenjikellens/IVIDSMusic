import { BasePageController } from './BasePageController.js';
import { MusicRepository } from '../api/MusicRepository.js';
import { CardComponentFactory } from '../cards.js';
import { MediaPlayer } from '../player/MediaPlayer.js';

let isHeroDismissed = false;

/**
 * HomePageController manages the Home view categories and top charts.
 */
export class HomePageController extends BasePageController {
    /**
     * Renders Home page category rows in-place into pre-rendered skeletons.
     * @param {Object} params
     */
    async render(params = {}) {
        this.resetAbortController();

        try {
            const genres = ['Pop', 'Rock', 'Hip-Hop', 'Hardcore', 'Electronic', 'Jazz', 'Dance'];
            const results = await Promise.all(
                genres.map(async (genre) => {
                    try {
                        const tracks = await MusicRepository.search(genre, 12, 'song');
                        return {
                            id: genre.toLowerCase().replace(/\s+/g, '-'),
                            tracks
                        };
                    } catch (e) {
                        return { id: genre.toLowerCase(), tracks: [] };
                    }
                })
            );

            if (this.signal?.aborted) return;

            results.forEach(category => {
                const rowContent = document.getElementById(`content-${category.id}`);
                if (rowContent) {
                    const existingCards = Array.from(rowContent.children);
                    category.tracks.forEach((track, index) => {
                        if (index < existingCards.length) {
                            CardComponentFactory.hydrateCard(existingCards[index], track);
                        } else {
                            rowContent.appendChild(CardComponentFactory.createCard(track));
                        }
                    });

                    if (existingCards.length > category.tracks.length) {
                        for (let i = category.tracks.length; i < existingCards.length; i++) {
                            existingCards[i].remove();
                        }
                    }
                }
            });

            const heroBtn = document.getElementById('play-hero-btn');
            if (heroBtn && results[0]?.tracks[0]) {
                heroBtn.onclick = () => MediaPlayer.playTrack(results[0].tracks[0]);
            }

            const hero = document.querySelector('.hero');
            const closeBtn = document.getElementById('close-hero-btn');
            if (hero) {
                if (isHeroDismissed) {
                    hero.classList.add('is-hidden');
                } else if (closeBtn) {
                    closeBtn.onclick = (e) => {
                        e.stopPropagation();
                        hero.classList.add('is-hidden');
                        isHeroDismissed = true;
                    };
                }
            }
        } catch (err) {
            console.error('[HomePageController] Render error:', err);
        }
    }
}
