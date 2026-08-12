import { BasePageController } from './BasePageController.js';
import { MusicRepository } from '../api/MusicRepository.js';
import { CardComponentFactory } from '../cards.js';
import { MediaPlayer } from '../player/MediaPlayer.js';

let isHeroDismissed = false;

/**
 * HomePageController manages the Home view categories, lazy loading, and top charts.
 */
export class HomePageController extends BasePageController {
    /**
     * Renders Home page category rows using Deezer genre chart endpoints.
     * @param {Object} params
     */
    async render(params = {}) {
        this.resetAbortController();

        try {
            const rowsContainer = document.getElementById('home-rows-container');
            if (rowsContainer) {
                // Bind container event delegation for card clicks & play buttons
                this.bindContainerEvent(rowsContainer, 'click', '.music-card', (card, e) => {
                    const playBtn = e.target.closest('.card-play-btn');
                    const trackJson = card.dataset.trackJson;
                    if (!trackJson) return;

                    try {
                        const track = JSON.parse(trackJson);
                        if (playBtn) {
                            e.preventDefault();
                            e.stopPropagation();
                            MediaPlayer.playTrack(track);
                        } else if (e.target.classList.contains('artist-link')) {
                            e.preventDefault();
                            e.stopPropagation();
                            if (window.Router) window.Router.loadPage('artist', { name: e.target.dataset.name || track.artist });
                        } else {
                            if (track.type === 'artist') {
                                if (window.Router) window.Router.loadPage('artist', { name: track.name || track.title });
                            } else if (track.type === 'album') {
                                if (window.Router) window.Router.loadPage('album', { id: track.id });
                            } else {
                                if (window.Router) window.Router.loadPage('song', { id: track.id, track });
                            }
                        }
                    } catch (err) {
                        console.error('[HomePageController] Card click parse error:', err);
                    }
                });
            }

            const populateRow = (category) => {
                const rowContent = document.getElementById(`content-${category.id}`);
                if (!rowContent || !category.tracks) return;
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
            };

            // 1. Fetch top 2 initial categories immediately
            const initialCategories = await Promise.all([
                MusicRepository.getCategories(['Pop'], this.signal).then(cats => cats[0]),
                MusicRepository.getCategories(['Rock'], this.signal).then(cats => cats[0])
            ]);

            if (this.signal?.aborted) return;
            initialCategories.filter(Boolean).forEach(cat => populateRow(cat));

            // Wire up Hero Listen button
            const heroBtn = document.getElementById('play-hero-btn');
            if (heroBtn && initialCategories[0]?.tracks[0]) {
                heroBtn.onclick = () => MediaPlayer.playTrack(initialCategories[0].tracks[0]);
            }

            // Lazy load remaining off-screen categories
            const lazyCategories = [
                { genre: 'Hip-Hop', id: 'row-hip-hop' },
                { genre: 'Hardcore', id: 'row-hardcore' },
                { genre: 'Electronic', id: 'row-electronic' },
                { genre: 'Jazz', id: 'row-jazz' },
                { genre: 'Dance', id: 'row-dance' }
            ];

            if (typeof IntersectionObserver !== 'undefined') {
                const rowObserver = new IntersectionObserver((entries, observer) => {
                    entries.forEach(entry => {
                        if (entry.isIntersecting) {
                            const rowEl = entry.target;
                            const genre = rowEl.dataset.lazyGenre;
                            if (genre) {
                                rowEl.removeAttribute('data-lazy-genre');
                                MusicRepository.getCategories([genre], this.signal).then(cats => {
                                    if (!this.signal?.aborted && cats[0]) populateRow(cats[0]);
                                }).catch(() => {});
                            }
                            observer.unobserve(rowEl);
                        }
                    });
                }, { rootMargin: '400px 0px', threshold: 0.01 });

                lazyCategories.forEach(item => {
                    const rowEl = document.getElementById(item.id);
                    if (rowEl) {
                        rowEl.dataset.lazyGenre = item.genre;
                        rowObserver.observe(rowEl);
                    }
                });
            } else {
                MusicRepository.getCategories(lazyCategories.map(c => c.genre), this.signal).then(rows => {
                    if (!this.signal?.aborted) rows.forEach(cat => populateRow(cat));
                }).catch(() => {});
            }

            // Setup Hero dismissal
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
