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
| **Like Track** | `recordLikeTrack` | `+3` track, `+2` (distributed) | Track & Genres |
| **Like Artist** | `recordLikeArtist` | `+3` | Artist |
| **Dislike Track** | `recordDislikeTrack` | `score / 2` track, `-2` artist, `-1` genres | Track, Artist, Genres |
| **Dislike Artist** | `recordDislikeArtist` | `score / 2` | Artist |
| **Early Skip (<20s)** | `recordEarlySkip` | `-1` | Track |

## Feed Generation (`generateRecommendations`)

1. **Top Interests Extraction**: Selects the user's top-scoring tracks (`3-5`), artists (`3-5`), and top `3` genres from local scores.
2. **Parallel Expansion**: Queries `MusicDiscoveryRepository` in parallel for track-related expansions, artist top tracks, and genre tracks.
3. **Cosine Similarity Ranking**: Ranks candidate tracks against the user's local taste vector and emits **Discovery Mix**, **Genre Fusion**, and **Taste Match** collections surfaced in `HomeScreen` and `RecommendedScreen`.
