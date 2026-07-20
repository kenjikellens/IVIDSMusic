package com.kenjigames.ividsmusic.ui.screen.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.ui.component.header.TopSearchBar
import com.kenjigames.ividsmusic.ui.component.list.TrackListItem
import com.kenjigames.ividsmusic.ui.component.shared.EmptyState
import com.kenjigames.ividsmusic.ui.theme.TextPrimary
import com.kenjigames.ividsmusic.ui.theme.Typography

/**
 * Library screen composable displaying user's saved/liked tracks with local real-time search.
 */
@Composable
fun LibraryScreen(
    onSongClick: (Song) -> Unit = {},
    onExploreClick: () -> Unit = {},
    viewModel: LibraryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val filteredSongs = if (uiState.searchQuery.isBlank()) {
        uiState.likedSongs
    } else {
        uiState.likedSongs.filter {
            it.title.contains(uiState.searchQuery, ignoreCase = true) ||
                    it.artistName.contains(uiState.searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Your Library",
            style = Typography.headlineLarge,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        TopSearchBar(
            query = uiState.searchQuery,
            onQueryChange = { viewModel.onSearchQueryChange(it) },
            placeholder = "Search saved songs..."
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredSongs.isEmpty()) {
            EmptyState(
                message = if (uiState.likedSongs.isEmpty()) "Your library is empty" else "No matching songs found",
                actionLabel = "Discover Music",
                onAction = onExploreClick
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
            ) {
                items(filteredSongs, key = { it.id }) { song ->
                    TrackListItem(
                        song = song,
                        onClick = { onSongClick(song) },
                        onMoreClick = { viewModel.deleteTrack(song) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
