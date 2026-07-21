package com.kenjigames.ividsmusic.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.ui.component.row.HorizontalTileRow
import com.kenjigames.ividsmusic.ui.component.skeleton.SkeletonRow
import com.kenjigames.ividsmusic.ui.component.shared.ErrorState

/**
 * Landing Home screen composable rendering curated genre rows and recommendation sections.
 *
 * @param onSongClick Callback when a song is tapped
 * @param onSeeAllClick Callback when a section "See All" is tapped
 * @param viewModel HomeViewModel instance
 */
@Composable
fun HomeScreen(
    onSongClick: (Song) -> Unit = {},
    onSeeAllClick: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.errorMessage != null && uiState.allGenreNames.isEmpty()) {
            ErrorState(
                message = uiState.errorMessage ?: "Failed to load home content",
                onRetry = { viewModel.loadHomeContent() }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
            ) {
                // "Recommended for You" Row
                item(key = "RecommendedForYou", contentType = "HeaderRow") {
                    if (uiState.recommendedSongs.isNotEmpty()) {
                        HorizontalTileRow(
                            title = "Recommended for You",
                            items = uiState.recommendedSongs,
                            onSeeAllClick = { onSeeAllClick("Recommended") },
                            onItemClick = { item -> if (item is Song) onSongClick(item) }
                        )
                    } else {
                        SkeletonRow(title = "Recommended for You")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Genre Rows
                items(
                    items = uiState.allGenreNames,
                    key = { genreTitle -> genreTitle },
                    contentType = { "GenreRow" }
                ) { genreTitle ->
                    val tracks = uiState.genreTracks[genreTitle]
                    if (tracks != null && tracks.isNotEmpty()) {
                        HorizontalTileRow(
                            title = genreTitle,
                            items = tracks,
                            onSeeAllClick = { onSeeAllClick(genreTitle) },
                            onItemClick = { item -> if (item is Song) onSongClick(item) }
                        )
                    } else {
                        SkeletonRow(title = genreTitle)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
