package com.kenjigames.ividsmusic.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.repository.MusicRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Source definition for fetching category tracks */
sealed class CategorySource {
    data class Chart(val genreId: Int) : CategorySource()
    data class Search(val query: String) : CategorySource()
}

/** State object for the Home screen UI */
data class HomeUiState(
    val isLoading: Boolean = true,
    val allGenreNames: List<String> = emptyList(),
    val genreTracks: Map<String, List<Song>> = emptyMap(),
    val recommendedSongs: List<Song> = emptyList(),
    val errorMessage: String? = null
)

/** ViewModel powering the Home screen */
class HomeViewModel(
    private val musicRepository: MusicRepository = MusicRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Extended categories mapping: Chart ID for official genres, Search query for subgenres
    private val categoriesMap = linkedMapOf<String, CategorySource>(
        "Pop" to CategorySource.Chart(132),
        "Rock" to CategorySource.Chart(152),
        "Hip-Hop" to CategorySource.Chart(116),
        "Electronic" to CategorySource.Chart(106),
        "Hardcore" to CategorySource.Chart(464),
        "Hardstyle" to CategorySource.Search("hardstyle"),
        "Uptempo" to CategorySource.Search("uptempo hardcore"),
        "Speedcore" to CategorySource.Search("speedcore"),
        "Frenchcore" to CategorySource.Search("frenchcore"),
        "French Indie" to CategorySource.Search("french indie"),
        "Phonk" to CategorySource.Search("phonk"),
        "Drum & Bass" to CategorySource.Search("drum and bass"),
        "Dubstep" to CategorySource.Search("dubstep"),
        "Psytrance" to CategorySource.Search("psytrance"),
        "Synthwave" to CategorySource.Search("synthwave"),
        "Gabber" to CategorySource.Search("gabber"),
        "Nightcore" to CategorySource.Search("nightcore"),
        "Eurodance" to CategorySource.Search("eurodance"),
        "Hardtekk" to CategorySource.Search("hardtekk"),
        "R&B" to CategorySource.Chart(165),
        "Jazz" to CategorySource.Chart(129),
        "Dance" to CategorySource.Chart(113),
        "Alternative" to CategorySource.Chart(85),
        "Classical" to CategorySource.Chart(98),
        "Country" to CategorySource.Chart(84),
        "Reggae" to CategorySource.Chart(144),
        "Soul & Funk" to CategorySource.Chart(169),
        "Blues" to CategorySource.Chart(153),
        "Latin" to CategorySource.Chart(197),
        "Reggaeton" to CategorySource.Chart(117),
        "Folk" to CategorySource.Chart(80),
        "Soundtracks" to CategorySource.Chart(173),
        "Techno" to CategorySource.Chart(107),
        "House" to CategorySource.Chart(108),
        "Punk" to CategorySource.Chart(154),
        "Indie Pop" to CategorySource.Chart(133)
    )

    init {
        loadHomeContent()
    }

    private suspend fun fetchSongsForSource(source: CategorySource): List<Song> {
        return when (source) {
            is CategorySource.Chart -> musicRepository.getGenreChart(source.genreId, 12).getOrNull() ?: emptyList()
            is CategorySource.Search -> musicRepository.searchTracks(source.query, 12).getOrNull() ?: emptyList()
        }
    }

    fun loadHomeContent() {
        viewModelScope.launch {
            // Instantly publish all genre names so UI renders skeleton rows under every header immediately
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    allGenreNames = categoriesMap.keys.toList(),
                    errorMessage = null
                ) 
            }

            try {
                // 1. Fetch Top Charts (Recommended) + First batch of 4 categories
                val recommendedDeferred = async { musicRepository.getGenreChart(0, 20) }
                val initialBatchDeferred = categoriesMap.entries.take(4).map { (name, source) ->
                    async { name to fetchSongsForSource(source) }
                }

                val recommended = recommendedDeferred.await().getOrNull() ?: emptyList()
                val initialResults = initialBatchDeferred.awaitAll().toMap()

                _uiState.update { state ->
                    val updatedMap = LinkedHashMap(state.genreTracks)
                    initialResults.forEach { (name, tracks) ->
                        if (tracks.isNotEmpty()) updatedMap[name] = tracks
                    }
                    state.copy(
                        recommendedSongs = recommended,
                        genreTracks = updatedMap
                    )
                }

                // 2. Fetch remaining categories in background chunks of 5
                val remainingGenres = categoriesMap.entries.drop(4)
                remainingGenres.chunked(5).forEach { chunk ->
                    val chunkResults = chunk.map { (name, source) ->
                        async { name to fetchSongsForSource(source) }
                    }.awaitAll().toMap()

                    _uiState.update { state ->
                        val updatedMap = LinkedHashMap(state.genreTracks)
                        chunkResults.forEach { (name, tracks) ->
                            if (tracks.isNotEmpty()) updatedMap[name] = tracks
                        }
                        state.copy(genreTracks = updatedMap)
                    }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }
}
