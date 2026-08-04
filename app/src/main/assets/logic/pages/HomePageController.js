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
     * Renders Home page category rows using Deezer genre chart endpoints.
     * @param {Object} params
     */
    async render(params = {}) {
        this.resetAbortController();

        try {
            const rows = await MusicRepository.getRecommendations(this.signal);
            if (this.signal?.aborted) return;

            rows.forEach(category => {
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
            if (heroBtn && rows[0]?.tracks[0]) {
                heroBtn.onclick = () => MediaPlayer.playTrack(rows[0].tracks[0]);
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
