package com.kenjigames.ividsmusic

import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.network.resolver.StreamResolver
import com.kenjigames.ividsmusic.domain.usecase.ResolveStreamUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Mock implementation of [StreamResolver] for testing resolving strategy chains.
 */
class MockStreamResolver(
    private val mockVideoId: String?,
    private val mockAudioUrl: String?
) : StreamResolver {
    override suspend fun resolveVideoId(query: String): String? = mockVideoId
    override suspend fun resolveAudioUrl(videoId: String): String? = mockAudioUrl
}

/**
 * Unit tests verifying stream resolution use cases and preview fallbacks.
 */
class StreamResolverTest {

    @Test
    fun testSuccessfulStreamResolution() = runBlocking {
        val song = Song("1", "Song A", "Artist X", coverUrl = "", videoId = "vid123")
        val resolver = MockStreamResolver("vid123", "https://stream.url/audio.mp3")

        val useCase = ResolveStreamUseCase(primaryResolver = resolver, fallbackResolver = resolver)
        val resolvedUrl = useCase(song)

        assertEquals("https://stream.url/audio.mp3", resolvedUrl)
    }

    @Test
    fun testUnresolvableStreamReturnsNull() = runBlocking {
        val song = Song("1", "Song A", "Artist X", coverUrl = "")
        val resolver = MockStreamResolver(null, null)

        val useCase = ResolveStreamUseCase(primaryResolver = resolver, fallbackResolver = resolver)
        val resolvedUrl = useCase(song)

        assertNull(resolvedUrl)
    }
}
