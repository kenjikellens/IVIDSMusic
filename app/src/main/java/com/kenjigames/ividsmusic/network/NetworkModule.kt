package com.kenjigames.ividsmusic.network

import com.kenjigames.ividsmusic.network.api.DeezerApiService
import com.kenjigames.ividsmusic.network.resolver.InvidiousStreamResolver
import com.kenjigames.ividsmusic.network.resolver.PipedStreamResolver
import com.kenjigames.ividsmusic.network.resolver.StreamResolver
import com.kenjigames.ividsmusic.network.resolver.YouTubeInnertubeResolver
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

    /** Primary stream resolver replicating yt-dlp native Android Innertube player API calls */
    val youtubeInnertubeResolver: StreamResolver by lazy {
        YouTubeInnertubeResolver(okHttpClient)
    }

    /** Secondary stream resolver using Piped API pool */
    val pipedStreamResolver: StreamResolver by lazy {
        PipedStreamResolver(okHttpClient)
    }

    /** Fallback stream resolver using Invidious pool */
    val invidiousStreamResolver: StreamResolver by lazy {
        InvidiousStreamResolver(okHttpClient)
    }
}
