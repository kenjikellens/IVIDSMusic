# IVIDS Music Recommendation Algorithm

This document describes the local scoring system used to generate personalized music recommendations.

## 📊 Scoring System
The algorithm tracks "Interest Scores" for both **Artists** and **Tracks** locally on the device (`iv_discovery_scores`).

| Action | Impact | Type |
| :--- | :--- | :--- |
| **Click on Track** | +1 point | Track |
| **Complete Track (>90%)** | +1 bonus point | Track |
| **Click on Track** | +1 point | Artist (of that track) |
| **Click on Track** | +1 point | Genre (of that track)* |
| **Click on Artist** | +1 point | Artist |
| **Like Track/Artist** | +3 points | Track/Artist |
| **Like Track** | +2 point | Genre (of that track)* |
| **Dislike Track/Artist** | Score / 2 | Track/Artist |
| **Dislike Track** | -2 points | Artist (of that track) |
| **Dislike Track** | -1 point | Genre (of that track)* |
| **Skip Track (<20 sec)**| -1 point | Track |

*\* Genre points are distributed among all genres tagged to a track.*

### Constraints
- **Minimum Score**: 0
- **Maximum Score**: 100
- **Storage**: Local only (privacy-first).

## 💡 Recommendation Logic
1.  **Top Interests**: The system identifies the top 3-5 tracks and artists with the highest scores.
2.  **Expansion**:
    - For high-scoring **Tracks**: Fetch "Related Tracks" via the Deezer API.
    - For high-scoring **Artists**: Fetch their official "Top Tracks".
3.  **UI Display**: These tracks are merged and displayed on the Home page in a dedicated **"✨ Recommended for You"** row.
