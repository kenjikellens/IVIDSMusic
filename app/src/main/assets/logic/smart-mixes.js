import { MusicAPI } from './api.js';
import { HistorySystem } from './history.js';
import { CardSystem } from './cards.js';
import { Router } from './router.js';

/**
 * SmartMixes - AI-Powered Playlist Engine
 */
export const SmartMixes = {
   SECTION_ID: 'smart-mixes-section',
   CONTAINER_ID: 'smart-mixes-container',

   /**
    * Initializes and renders the Smart Mixes section on the Home page.
    */
   async refresh() {
      console.log('[SmartMixes] Refreshing...');
      const section = document.getElementById(this.SECTION_ID);
      const container = document.getElementById(this.CONTAINER_ID);

      if (!section || !container) {
         console.warn('[SmartMixes] UI elements not found. Section:', !!section, 'Container:', !!container);
         return;
      }

      // Force show the section immediately
      section.style.setProperty('display', 'block', 'important');
      section.style.setProperty('opacity', '1', 'important');
      section.style.setProperty('visibility', 'visible', 'important');

      console.log('[SmartMixes] UI forced visible, rendering skeletons...');
      this.renderSkeletons(container);

      try {
         const history = HistorySystem.get();
         let mixes = [];
         console.log('[SmartMixes] History count:', history.length);

         if (history.length === 0) {
            console.log('[SmartMixes] No history, fetching starters...');
            mixes = await this.getStarterMixes();
         } else {
            console.log('[SmartMixes] Fetching AI mixes...');
            mixes = await MusicAPI.getSmartMixes(history);

            if (!mixes || mixes.length === 0) {
               console.log('[SmartMixes] AI failed, falling back to starters...');
               mixes = await this.getStarterMixes();
            }
         }

         console.log('[SmartMixes] Rendering', mixes.length, 'mixes.');
         this.renderMixes(container, mixes);
      } catch (error) {
         console.error('[SmartMixes] Refresh failed:', error);
         // Don't hide it instantly, let user see it failed if it was visible
      }
   },

   /**
    * Renders 3 skeleton cards.
    */
   renderSkeletons(container) {
      container.innerHTML = '';
      for (let i = 0; i < 3; i++) {
         const skeleton = document.createElement('div');
         skeleton.className = 'skeleton-card large';
         skeleton.innerHTML = `
                <div class="skeleton-img">
                    <div class="ivids-loader poster-loader"></div>
                </div>
                <div class="skeleton-info-box">
                    <div class="skeleton-text title"></div>
                    <div class="skeleton-text desc"></div>
                </div>
            `;
         container.appendChild(skeleton);
      }
   },

   /**
    * Renders the generated mixes.
    */
   renderMixes(container, mixes) {
      container.innerHTML = '';

      mixes.forEach(mix => {
         const card = document.createElement('div');
         card.className = 'mix-card focused-item';
         card.tabIndex = 0;

         // Dynamic gradient from cover art
         const color = mix.cover_color || 'rgba(255,255,255,0.1)';
         card.style.setProperty('--mix-color', color);

         card.innerHTML = `
                <div class="mix-card-bg" style="background-image: url('${mix.cover}')"></div>
                <div class="mix-card-overlay"></div>
                <div class="mix-card-content">
                    <div class="mix-badge">✨ AI Mix</div>
                    <h3 class="mix-title">${mix.name}</h3>
                    <p class="mix-desc">${mix.description}</p>
                    <div class="mix-footer">
                        <span class="mix-count">${mix.tracks.length} tracks</span>
                        <button class="mix-play-btn">
                            <img src="svg/play.svg" alt="Play">
                        </button>
                    </div>
                </div>
            `;

         card.onclick = () => this.playMix(mix);
         card.onkeydown = (e) => { if (e.key === 'Enter') this.playMix(mix); };

         container.appendChild(card);
      });
   },

   /**
    * Queues the mix tracks and starts playback.
    */
   async playMix(mix) {
      if (!mix.tracks || mix.tracks.length === 0) return;

      // Use Global Player (initialized in index.html)
      if (window.YouTubePlayer) {
         window.YouTubePlayer.playTracks(mix.tracks);
      }
   },

   /**
    * Returns a set of curated starter mixes if AI or history is unavailable.
    */
   async getStarterMixes() {
      const starters = [
         { name: "Chill Vibes", desc: "Smooth sounds to relax and unwind.", query: "chill acoustic lofi" },
         { name: "Energy Boost", desc: "High-octane tracks to get you moving.", query: "electronic dance workout" },
         { name: "Lush Focus", desc: "Perfect background for deep work.", query: "ambient study focus" }
      ];

      return Promise.all(starters.map(async (s) => {
         const tracks = await MusicAPI.search(s.query, 12, 'song');
         return {
            name: s.name,
            description: s.desc,
            tracks: tracks,
            cover: tracks[0]?.cover || '',
            id: `starter-${s.name.toLowerCase().replace(/\s/g, '-')}`
         };
      }));
   }
};
