/**
 * IVIDS Music - Discovery Engine
 * Locally manages interest scores for tracks, artists, and genres.
 */

import { MusicAPI } from './api.js';

const STORAGE_KEY = 'iv_discovery_scores';
const MAX_SCORE = 100;
const MIN_SCORE = 0;

export const DiscoveryEngine = {
   /**
    * Loads the current scores from localStorage.
    * @returns {Object} { artists: { id: score }, tracks: { id: score }, genres: { name: score } }
    */
   getScores() {
      try {
         const data = localStorage.getItem(STORAGE_KEY);
         return JSON.parse(data || '{ "artists": {}, "tracks": {}, "genres": {}, "names": {} }');
      } catch (e) {
         console.error('[Discovery] Error reading scores', e);
         return { artists: {}, tracks: {}, genres: {}, names: {} };
      }
   },

   /**
    * Saves the scores to localStorage.
    * @param {Object} scores 
    */
   saveScores(scores) {
      try {
         localStorage.setItem(STORAGE_KEY, JSON.stringify(scores));
      } catch (e) {
         console.error('[Discovery] Error saving scores', e);
      }
   },

   updateScore(type, id, delta, halve = false, name = null) {
      if (!id) return;
      const scores = this.getScores();
      if (!scores[type]) scores[type] = {};
      if (!scores.names) scores.names = {};

      let current = scores[type][id] || 0;

      if (halve) {
         current = Math.floor(current / 2);
      } else {
         current += delta;
      }

      // Clamp values
      scores[type][id] = Math.max(MIN_SCORE, Math.min(MAX_SCORE, current));

      // Store name for UI labels if provided
      if (name) scores.names[id] = name;

      this.saveScores(scores);
      console.log(`[Discovery] Scored ${type}:${id} (${name || ''}) -> ${scores[type][id]}`);
   },

   /**
    * Scoring Triggers
    */

   /**
    * Called when a track is played.
    * @param {Object} track - The track object
    */
   async recordListen(track) {
      if (!track || !track.id) return;

      // +1 to Track
      this.updateScore('tracks', track.id, 1, false, track.title);

      // +1 to Artist
      if (track.artistId) {
         this.updateScore('artists', track.artistId, 1, false, track.artist);
      }

      // Lazy fetch rich details to get genres
      try {
         const details = await MusicAPI.getTrackDetails(track.id);
         if (details && details.genres) {
            details.genres.forEach(genre => {
               this.updateScore('genres', genre, 1);
            });
         }
      } catch (e) { console.error('[Discovery] Failed to score genres for listen', e); }
   },

   /**
    * Called when a track is played almost to the end (>90%).
    */
   recordCompletion(trackId) {
      if (!trackId) return;
      this.updateScore('tracks', trackId, 1); // Extra bonus point
   },

   /**
    * Called when a track is skipped early (<20s).
    */
   recordSkip(trackId) {
      if (!trackId) return;
      this.updateScore('tracks', trackId, -1);
   },

   recordArtistClick(artistId, artistName = null) {
      if (!artistId) return;
      this.updateScore('artists', artistId, 1, false, artistName);
   },

   async recordLike(track) {
      if (!track || !track.id) return;

      this.updateScore('tracks', track.id, 3, false, track.title);

      // Fetch genres for like bonus (+2)
      try {
         const details = await MusicAPI.getTrackDetails(track.id);
         if (details && details.genres) {
            details.genres.forEach(genre => {
               this.updateScore('genres', genre, 2);
            });
         }
      } catch (e) { console.error('[Discovery] Failed to score genres for like', e); }
   },

   recordDislike(track) {
      if (!track || !track.id) return;

      // /2 to Track
      this.updateScore('tracks', track.id, 0, true);

      // -2 to Artist
      if (track.artistId) {
         this.updateScore('artists', track.artistId, -2);
      }

      // -1 to Genres (we rely on cached data if possible to avoid API calls on dislike,
      // but for simplicity here we do a full fetch if needed. A robust version would cache track genres).
      MusicAPI.getTrackDetails(track.id).then(details => {
         if (details && details.genres) {
            details.genres.forEach(genre => {
               this.updateScore('genres', genre, -1);
            });
         }
      }).catch(e => console.error(e));
   },

   /**
    * Returns a list of interests sorted by score.
    */
   getInterests() {
      const scores = this.getScores();

      const sortedArtists = Object.entries(scores.artists || {})
         .sort(([, a], [, b]) => b - a)
         .map(([id, score]) => ({ id, score, type: 'artist', name: (scores.names || {})[id] || id }));

      const sortedTracks = Object.entries(scores.tracks || {})
         .sort(([, a], [, b]) => b - a)
         .map(([id, score]) => ({ id, score, type: 'track', title: (scores.names || {})[id] || id }));

      const sortedGenres = Object.entries(scores.genres || {})
         .sort(([, a], [, b]) => b - a)
         .map(([name, score]) => ({ name, score, type: 'genre' }));

      return { artists: sortedArtists, tracks: sortedTracks, genres: sortedGenres };
   },

   /**
    * Initializes the "For You" page UI
    */
   async initRecommended() {
      if (window.Loader) window.Loader.init();
      const container = document.getElementById('recommended-content');
      if (!container) return;

      try {
         const interests = this.getInterests();
         const signal = window.Router?.abortController?.signal;

         // If no data, show a message
         if (interests.artists.length === 0 && interests.tracks.length === 0) {
            container.innerHTML = `
               <div style="text-align:center; padding: 60px; color: var(--text-secondary);">
                  <div style="font-size: 3rem; margin-bottom: 20px;">🎧</div>
                  <h2 data-i18n="foryou_empty_title" style="color: var(--text-main); margin-bottom: 10px;">Start Listening!</h2>
                  <p data-i18n="foryou_empty_desc">Play some songs or like artists to get personalized recommendations here.</p>
               </div>
            `;
            if (window.LanguageManager) window.LanguageManager.translateUI(container);
            return;
         }

         container.innerHTML = ''; // Clear loader

         // We dynamically import CardSystem to avoid circular dependencies
         const { CardSystem } = await import('./cards.js');

         // 1. "Based on your top tracks" row
         if (interests.tracks.length > 0) {
            const topTrack = interests.tracks[0];
            const related = await MusicAPI.getRelatedTracks(topTrack.id, signal);
            if (related && related.length > 0) {
               const row = CardSystem.createRow(`Based on ${topTrack.title || 'your taste'}`, 'foryou-tracks', related);
               container.appendChild(row);
            }
         }

         // 2. "Because you like [Artist]" row
         if (interests.artists.length > 0) {
            const topArtist = interests.artists[0];
            const artistTracks = await MusicAPI.getArtistTopTracks(topArtist.id, signal);
            if (artistTracks && artistTracks.length > 0) {
               const row = CardSystem.createRow(`More from ${topArtist.id}`, 'foryou-artist', artistTracks);
               container.appendChild(row);
            }
         }

         // 3. "Genre Mixes" based on top genres
         if (interests.genres.length > 0) {
            const topGenre = interests.genres[0].name;
            const genreTracks = await MusicAPI.getGenreTracks(topGenre, signal);
            if (genreTracks && genreTracks.length > 0) {
               const row = CardSystem.createRow(`${topGenre} Essentials`, 'foryou-genre', genreTracks);
               container.appendChild(row);
            }
         }

         if (window.LanguageManager) window.LanguageManager.translateUI(container);
         if (window.Loader) window.Loader.init();

      } catch (e) {
         console.error('[initRecommended] Error:', e);
         container.innerHTML = `<div style="text-align:center; padding: 40px; color: var(--text-secondary);">Error loading recommendations.</div>`;
      }
   }
};

if (typeof window !== 'undefined') {
   window.DiscoveryEngine = DiscoveryEngine;
}
