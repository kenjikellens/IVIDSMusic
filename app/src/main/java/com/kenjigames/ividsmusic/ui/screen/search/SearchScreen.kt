package com.kenjigames.ividsmusic.ui.screen.search

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
import com.kenjigames.ividsmusic.ui.component.header.TopSearchBar
import com.kenjigames.ividsmusic.ui.component.list.TrackListItem
import com.kenjigames.ividsmusic.ui.component.skeleton.SkeletonRow
import com.kenjigames.ividsmusic.ui.component.shared.EmptyState

/**
 * Search screen composable with interactive TopSearchBar and paginated track list results.
 */
@Composable
fun SearchScreen(
    onSongClick: (Song) -> Unit = {},
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopSearchBar(
            query = uiState.query,
            onQueryChange = { viewModel.onQueryChange(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            SkeletonRow()
        } else if (uiState.query.isNotEmpty() && uiState.searchResults.isEmpty()) {
            EmptyState(
                message = "No songs found matching '${uiState.query}'",
                actionLabel = "Clear Search",
                onAction = { viewModel.onQueryChange("") }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
            ) {
                items(uiState.searchResults, key = { it.id }) { song ->
                    TrackListItem(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
