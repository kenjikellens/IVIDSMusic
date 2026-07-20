package com.kenjigames.ividsmusic.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        if (uiState.isLoading) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                SkeletonRow()
                Spacer(modifier = Modifier.height(16.dp))
                SkeletonRow()
            }
        } else if (uiState.errorMessage != null) {
            ErrorState(
                message = uiState.errorMessage ?: "Failed to load home content",
                onRetry = { viewModel.loadHomeContent() }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 80.dp)
            ) {
                // "Recommended for You" Row
                if (uiState.recommendedSongs.isNotEmpty()) {
                    HorizontalTileRow(
                        title = "Recommended for You",
                        items = uiState.recommendedSongs,
                        onSeeAllClick = { onSeeAllClick("Recommended") },
                        onItemClick = { item -> if (item is Song) onSongClick(item) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Genre Rows
                uiState.genreTracks.forEach { (genreTitle, tracks) ->
                    if (tracks.isNotEmpty()) {
                        HorizontalTileRow(
                            title = genreTitle,
                            items = tracks,
                            onSeeAllClick = { onSeeAllClick(genreTitle) },
                            onItemClick = { item -> if (item is Song) onSongClick(item) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
