# 🌐 Networking & API Integration Architecture (Retrofit / OkHttp)

This document describes the native Kotlin network layer for music discovery and audio stream resolution in **IVIDS Music**.

---

## 📡 Networking Stack

| Purpose | Library | Key Functions |
|---|---|---|
| **HTTP Client** | **OkHttp 4.x** | Connection pooling, custom headers, rate-limiting retry interceptor |
| **REST Client** | **Retrofit 2.x** | Type-safe API calls for Deezer & Invidious |
| **JSON Serialization** | **Kotlinx Serialization** | High-performance JSON parsing into Kotlin data classes |
| **Async Execution** | **Kotlin Coroutines + Flow** | Non-blocking asynchronous I/O and reactive data streams |

---

## 🔌 API Services

### 1. `DeezerApiService.kt` (Music Discovery & Metadata)
Base URL: `https://api.deezer.com/`

```kotlin
interface DeezerApiService {
    @GET("search")
    suspend fun searchTracks(
        @Query("q") query: String,
        @Query("limit") limit: Int = 25,
        @Query("index") index: Int = 0
    ): DeezerSearchResponse

    @GET("chart/0/tracks")
    suspend fun getGenreChart(
        @Query("limit") limit: Int = 20
    ): DeezerTrackListResponse

    @GET("artist/{id}")
    suspend fun getArtistDetails(@Path("id") artistId: String): DeezerArtistResponse

    @GET("artist/{id}/top")
    suspend fun getArtistTopTracks(@Path("id") artistId: String): DeezerTrackListResponse

    @GET("album/{id}")
    suspend fun getAlbumDetails(@Path("id") albumId: String): DeezerAlbumResponse
}
```

### 2. `iTunesApiService.kt` (Redundant Fallback)
Base URL: `https://itunes.apple.com/`
- Serves as an automatic network fallback if Deezer API endpoints are degraded or rate-limited.

### 3. `InvidiousApiService.kt` (YouTube Audio Stream Resolution)
Rotates across public Invidious & Piped instances (e.g. `vid.puffyan.us`, `invidious.nerdvpn.de`):

```kotlin
interface InvidiousApiService {
    @GET("api/v1/search")
    suspend fun searchVideos(
        @Query("q") searchQuery: String,
        @Query("type") type: String = "video"
    ): List<InvidiousVideoDto>

    @GET("api/v1/videos/{id}")
    suspend fun getVideoStreams(
        @Path("id") videoId: String
    ): InvidiousStreamDetailsDto
}
```

---

## 🛡️ Resilience & HTML Scraping Fallback

If all external Invidious API instances fail or become rate-limited:
1. **Fallback Scraper (`YouTubeScraper.kt`)**: Executes a direct OkHttp GET request to `https://www.youtube.com/results?search_query=...` with standard browser `User-Agent` headers.
2. **Regex Parsing**: Extracts `videoId` from raw HTML response (`ytInitialData` JSON block).
3. **Stream Resolution**: Resolves audio stream URL from `videoId`.

---

## 🎨 Canvas Palette Tinting (`ColorExtractor.kt`)

In place of HTML Canvas pixel sampling, native Android uses **Coil Compose** with the **Android Palette API**:

```kotlin
suspend fun extractAccentColor(context: Context, imageUrl: String): Color {
    val request = ImageRequest.Builder(context)
        .data(imageUrl)
        .allowHardware(false) // Required for Palette sampling
        .build()
    
    val drawable = context.imageLoader.execute(request).drawable
    val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return DefaultAccentColor
    
    val palette = Palette.from(bitmap).generate()
    val dominantColorInt = palette.getVibrantColor(palette.getDominantColor(0xFF6200EE.toInt()))
    return Color(dominantColorInt)
}
```
Provides dynamic accent colors for backgrounds and player bars across all screens.
