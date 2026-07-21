package com.kenjigames.ividsmusic.ui.screen.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.ui.component.row.HorizontalTileRow
import com.kenjigames.ividsmusic.ui.component.shared.ErrorState
import com.kenjigames.ividsmusic.ui.component.skeleton.SkeletonRow
import com.kenjigames.ividsmusic.ui.theme.GlassBorder
import com.kenjigames.ividsmusic.ui.theme.PrimaryAccent
import com.kenjigames.ividsmusic.ui.theme.TextMuted
import com.kenjigames.ividsmusic.ui.theme.TextPrimary
import com.kenjigames.ividsmusic.ui.theme.Typography

/**
 * Clean Home screen composable rendering quick category chips, curated genre rows,
 * and recommendation sections with modern mobile typography matching Spotify/YouTube Music.
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
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Popular", "Relax", "Workout", "Focus", "Party")

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.errorMessage != null && uiState.allGenreNames.isEmpty()) {
            ErrorState(
                message = uiState.errorMessage ?: "Failed to load home content",
                onRetry = { viewModel.loadHomeContent() }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Sleek Category Filter Chips (Spotify Style)
                item(key = "CategoryChips", contentType = "ChipRow") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = selectedCategory == category
                            Surface(
                                modifier = Modifier.clickable { selectedCategory = category },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) PrimaryAccent else Color(0x1F222232),
                                border = BorderStroke(1.dp, if (isSelected) PrimaryAccent else GlassBorder)
                            ) {
                                Text(
                                    text = category,
                                    style = Typography.labelLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    ),
                                    color = if (isSelected) Color.White else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

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
                    Spacer(modifier = Modifier.height(16.dp))
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
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
