package com.kenjigames.ividsmusic.ui.screen.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kenjigames.ividsmusic.domain.model.Album
import com.kenjigames.ividsmusic.domain.model.Artist
import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.ui.component.header.TopSearchBar
import com.kenjigames.ividsmusic.ui.component.row.HorizontalTileRow
import com.kenjigames.ividsmusic.ui.component.shared.EmptyState
import com.kenjigames.ividsmusic.ui.component.skeleton.SkeletonRow

/**
 * Search screen composable rendering search results in the Home Screen format (Songs -> Albums -> Artists).
 *
 * @param onSongClick Callback when a song tile is tapped
 * @param onAlbumClick Callback when an album tile is tapped
 * @param onArtistClick Callback when an artist tile is tapped
 * @param viewModel SearchViewModel instance
 */
@Composable
fun SearchScreen(
    onSongClick: (Song) -> Unit = {},
    onAlbumClick: (Album) -> Unit = {},
    onArtistClick: (Artist) -> Unit = {},
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Search Bar Top Component
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            TopSearchBar(
                query = uiState.query,
                onQueryChange = { viewModel.onQueryChange(it) }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (uiState.isLoading) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SkeletonRow(title = "Searching Songs...")
                Spacer(modifier = Modifier.height(16.dp))
                SkeletonRow(title = "Searching Albums...")
            }
        } else if (uiState.query.isNotEmpty() && uiState.isEmpty) {
            EmptyState(
                message = "No results found matching '${uiState.query}'",
                actionLabel = "Clear Search",
                onAction = { viewModel.onQueryChange("") }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Section 1: Songs
                if (uiState.songs.isNotEmpty()) {
                    item(key = "SearchSongs", contentType = "SongsRow") {
                        HorizontalTileRow(
                            title = "Songs",
                            items = uiState.songs,
                            onItemClick = { item -> if (item is Song) onSongClick(item) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Section 2: Albums
                if (uiState.albums.isNotEmpty()) {
                    item(key = "SearchAlbums", contentType = "AlbumsRow") {
                        HorizontalTileRow(
                            title = "Albums",
                            items = uiState.albums,
                            onItemClick = { item -> if (item is Album) onAlbumClick(item) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Section 3: Artists
                if (uiState.artists.isNotEmpty()) {
                    item(key = "SearchArtists", contentType = "ArtistsRow") {
                        HorizontalTileRow(
                            title = "Artists",
                            items = uiState.artists,
                            onItemClick = { item -> if (item is Artist) onArtistClick(item) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
