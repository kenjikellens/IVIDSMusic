# IVIDS Music Recommendation Algorithm

`RecommendationEngine` (`core/domain/src/commonMain/kotlin/com/kenjigames/ividsmusic/domain/usecase/RecommendationEngine.kt`) computes personalized recommendations strictly on-device with zero external telemetry.

## Local Scoring Rules

Scores are persisted in SQLDelight's `discovery_scores` table (`SqlScoreRepository`) and clamped strictly to `[0, 100]`:

| User Action | Method | Score Delta | Target Type |
|---|---|---|---|
| **Track Click** | `recordTrackClick` | `+1` | Track |
| **Track Click** | `recordTrackClick` | `+1` | Artist |
| **Track Click** | `recordTrackClick` | `+1` (distributed) | Track Genres |
| **Track Completion (>90%)** | `recordTrackCompletion` | `+1` bonus | Track |
| **Artist Click** | `recordArtistClick` | `+1` | Artist |
| **Like Track** | `recordLikeTrack` | `+3` track, `+2` artist, `+2` (distributed) genres | Track, Artist & Genres |
| **Like Artist** | `recordLikeArtist` | `+3` | Artist |
| **Dislike Track** | `recordDislikeTrack` | `score / 2` track, `-2` artist, `-1` genres | Track, Artist, Genres |
| **Dislike Artist** | `recordDislikeArtist` | `score / 2` | Artist |
| **Early Skip (<20s)** | `recordEarlySkip` | `-1` | Track |

## Feed Generation (`generateRecommendations`)

1. **Top Interests Extraction**: Selects the user's top-scoring tracks (`3-5`), artists (`3-5`), and top `3` genres weighted by a 21-day half-life recency decay (`recencyWeight`).
2. **Parallel Expansion**: Queries `MusicDiscoveryRepository` in parallel for track-related expansions, artist top tracks, and genre tracks.
3. **Cosine Similarity Ranking & Diversity**: Maps positive scores logarithmically (`ln(1 + score)`), applies negative coordinates only to explicitly penalized zero scores alongside active `era_*` and `popularity` seed axes, and diversifies **Discovery Mix** (max 2 tracks per artist, genre spread) before emitting **Genre Fusion** and **Taste Match** collections.
