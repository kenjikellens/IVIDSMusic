package com.kenjigames.ividsmusic.network

import com.kenjigames.ividsmusic.network.api.DeezerApiService
import com.kenjigames.ividsmusic.network.resolver.InvidiousStreamResolver
import com.kenjigames.ividsmusic.network.resolver.StreamResolver
import com.kenjigames.ividsmusic.network.resolver.YouTubeHtmlScraper
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Centralized singleton network module providing HTTP clients, Retrofit services, and stream resolvers.
 */
object NetworkModule {

    /** OkHttpClient instance configured with 15-second timeouts */
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /** Retrofit service for Deezer API queries */
    val deezerApiService: DeezerApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.deezer.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeezerApiService::class.java)
    }

    /** Primary stream resolver using Invidious pool */
    val invidiousStreamResolver: StreamResolver by lazy {
        InvidiousStreamResolver(okHttpClient)
    }

    /** Fallback stream resolver using YouTube HTML scraping */
    val youtubeHtmlScraper: StreamResolver by lazy {
        YouTubeHtmlScraper(okHttpClient)
    }
}
