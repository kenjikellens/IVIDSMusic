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
    * Constructs a normalized mathematical feature vector for a track.
    * Represents coordinates for genres, artist, era, and popularity.
    * 
    * @param {Object} track - Raw track object
    * @returns {Object} Feature vector mapping dimension keys to coordinate values
    */
   createTrackVector(track) {
      const vector = {};
      
      // 1. Artist dimension (one-hot coordinate weight 1.0)
      if (track.artistId) {
         vector[`artist_${track.artistId}`] = 1.0;
      } else if (track.artist) {
         // Fallback to name if ID is unavailable
         vector[`artist_${track.artist.toLowerCase().trim()}`] = 1.0;
      }

      // 2. Genre dimension (one-hot weights)
      if (track.genres && Array.isArray(track.genres)) {
         track.genres.forEach(genre => {
            vector[`genre_${genre.toLowerCase().trim()}`] = 1.0;
         });
      } else if (track.genre) {
         vector[`genre_${track.genre.toLowerCase().trim()}`] = 1.0;
      }

      // 3. Era/Year dimension (one-hot binned coordinate weights)
      if (track.year) {
         const year = parseInt(track.year, 10);
         if (!isNaN(year)) {
            if (year < 1990) vector['era_classic'] = 1.0;
            else if (year >= 1990 && year < 2005) vector['era_90s_00s'] = 1.0;
            else if (year >= 2005 && year < 2018) vector['era_modern'] = 1.0;
            else vector['era_recent'] = 1.0;
         }
      }

      // 4. Popularity Proxy dimension (normalized between 0.0 and 1.0)
      if (track.rank) {
         const rank = parseFloat(track.rank);
         vector['popularity'] = Math.min(1.0, Math.max(0.0, rank / 1000000.0));
      } else {
         vector['popularity'] = 0.5; // Default middle-ground weight
      }

      // Normalize the vector to unit length (Euclidean normalization)
      let sumSq = 0;
      for (const val of Object.values(vector)) {
         sumSq += val * val;
      }
      const magnitude = Math.sqrt(sumSq);
      
      if (magnitude > 0) {
         for (const key in vector) {
            vector[key] /= magnitude;
         }
      }

      return vector;
   },

   /**
    * Builds a cumulative taste vector for the user by projecting weighted telemetry coordinates.
    * 
    * @param {Object} scores - Telemetry scores fetched from localStorage
    * @returns {Object} Sparse vector representing the user's multi-dimensional preference coordinates
    */
   buildUserProfileVector(scores) {
      const userVector = {};

      // 1. Project Genre preferences
      if (scores.genres) {
         Object.entries(scores.genres).forEach(([genre, score]) => {
            const coordinate = `genre_${genre.toLowerCase().trim()}`;
            // Scale weights: Neutral threshold centered around 10
            const weight = (score - 10) / 10.0;
            userVector[coordinate] = (userVector[coordinate] || 0) + weight;
         });
      }

      // 2. Project Artist preferences
      if (scores.artists) {
         Object.entries(scores.artists).forEach(([artistId, score]) => {
            const coordinate = `artist_${artistId}`;
            const weight = (score - 15) / 10.0;
            userVector[coordinate] = (userVector[coordinate] || 0) + weight;
         });
      }

      return userVector;
   },

   /**
    * Calculates the Cosine Similarity between two sparse vectors.
    * Projects the dot product divided by the product of their magnitudes.
    * 
    * @param {Object} vectorA - User taste vector
    * @param {Object} vectorB - Track feature vector
    * @returns {number} Value between -1.0 and 1.0 representing angular similarity
    */
   calculateCosineSimilarity(vectorA, vectorB) {
      let dotProduct = 0;
      let sumSqA = 0;
      let sumSqB = 0;

      const allKeys = new Set([...Object.keys(vectorA), ...Object.keys(vectorB)]);

      allKeys.forEach(key => {
         const valA = vectorA[key] || 0;
         const valB = vectorB[key] || 0;

         dotProduct += valA * valB;
         sumSqA += valA * valA;
         sumSqB += valB * valB;
      });

      const magnitudeA = Math.sqrt(sumSqA);
      const magnitudeB = Math.sqrt(sumSqB);

      if (magnitudeA === 0 || magnitudeB === 0) return 0.0;

      return dotProduct / (magnitudeA * magnitudeB);
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

         // If no telemetry history exists, show a localized empty state message
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

         container.innerHTML = `
            <div class="skeleton-list" style="padding: 20px;">
               ${`<div class="skeleton-card track-skeleton" style="width: 100%; height: 80px; margin-bottom: 15px;"></div>`.repeat(3)}
            </div>
         `;

         // Dynamic import CardSystem to avoid circular imports
         const { CardSystem } = await import('./cards.js');

         // 1. Gather a High-Fidelity Candidate Pool
         const candidatePool = [];
         const savedTracks = window.MusicAPI?.getSavedTracks ? await window.MusicAPI.getSavedTracks() : [];
         const savedIds = new Set(savedTracks.map(t => t.id));

         // Fetch charts for a solid popular fallback base
         try {
            const chartTracks = await MusicAPI.getArtistTopTracks('5390796', signal); // Deezer Editorial charts fallback
            if (chartTracks) candidatePool.push(...chartTracks);
         } catch (e) {}

         // Gather candidates based on top 3 tracks
         const topTracks = interests.tracks.slice(0, 3);
         for (const t of topTracks) {
            try {
               const related = await MusicAPI.getRelatedTracks(t.id, signal);
               if (related) candidatePool.push(...related);
            } catch (e) {}
         }

         // Gather candidates based on top 3 artists
         const topArtists = interests.artists.slice(0, 3);
         for (const a of topArtists) {
            try {
               const artistTracks = await MusicAPI.getArtistTopTracks(a.id, signal);
               if (artistTracks) candidatePool.push(...artistTracks);
            } catch (e) {}
         }

         // Gather candidates based on top 3 genres
         const topGenres = interests.genres.slice(0, 3);
         for (const g of topGenres) {
            try {
               const genreTracks = await MusicAPI.getGenreTracks(g.name, signal);
               if (genreTracks) candidatePool.push(...genreTracks);
            } catch (e) {}
         }

         // Deduplicate candidate pool by unique Deezer IDs
         const uniquePool = [];
         const seenIds = new Set();
         candidatePool.forEach(track => {
            if (track && track.id && !seenIds.has(track.id)) {
               seenIds.add(track.id);
               uniquePool.push(track);
            }
         });

         // 2. Perform Cosine Similarity Projections
         const scores = this.getScores();
         const userVector = this.buildUserProfileVector(scores);
         const trackScores = scores.tracks || {};

         const scoredPool = uniquePool.map(track => {
            const trackVector = this.createTrackVector(track);
            let similarity = this.calculateCosineSimilarity(userVector, trackVector);

            // Apply penalty if the track is in history but has early skips
            const historyScore = trackScores[track.id] || 0;
            if (historyScore < 0) {
               similarity -= 0.4;
            }

            return { track, similarity };
         });

         // Sort candidates in descending order of similarity
         scoredPool.sort((a, b) => b.similarity - a.similarity);

         // Filter out already saved tracks for the dynamic "Discovery Mix"
         const discoveryMixCandidates = scoredPool
            .filter(item => !savedIds.has(item.track.id))
            .map(item => item.track);

         container.innerHTML = '';

         // --- Row 1: Discovery Mix For You ---
         if (discoveryMixCandidates.length > 0) {
            const discoveryList = discoveryMixCandidates.slice(0, 12);
            // Dynamic Header Translation Match
            const row = CardSystem.createRow('Discovery Mix For You', 'foryou-discovery-mix', discoveryList);
            container.appendChild(row);
         }

         // --- Row 2: Dynamic Genre Fusion Mix ---
         if (topGenres.length > 0) {
            const genreA = topGenres[0].name;
            const genreB = topGenres[1]?.name || topGenres[0].name;
            
            // Filter candidates that align closely with the user's top genres
            const fusionCandidates = scoredPool
               .filter(item => {
                  const name = item.track.genre || '';
                  return name.toLowerCase().includes(genreA.toLowerCase()) || 
                         name.toLowerCase().includes(genreB.toLowerCase());
               })
               .map(item => item.track)
               .slice(0, 10);

            if (fusionCandidates.length > 0) {
               const fusionTitle = genreA === genreB ? `${genreA} Fusion` : `${genreA} & ${genreB} Fusion`;
               const row = CardSystem.createRow(fusionTitle, 'foryou-genre-fusion', fusionCandidates);
               container.appendChild(row);
            }
         }

         // --- Row 3: Taste Match (Top Artists Blend) ---
         if (topArtists.length > 0) {
            const artistA = topArtists[0];
            const artistB = topArtists[1] || topArtists[0];
            
            const artistCandidates = scoredPool
               .filter(item => {
                  const t = item.track;
                  return (t.artistId && (t.artistId === artistA.id || t.artistId === artistB.id)) ||
                         (t.artist && (t.artist.toLowerCase() === artistA.name.toLowerCase() || 
                                       t.artist.toLowerCase() === artistB.name.toLowerCase()));
               })
               .map(item => item.track)
               .slice(0, 10);

            if (artistCandidates.length > 0) {
               const matchTitle = artistA.id === artistB.id ? `More from ${artistA.name}` : `Best of ${artistA.name} & ${artistB.name}`;
               const row = CardSystem.createRow(matchTitle, 'foryou-taste-match', artistCandidates);
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
