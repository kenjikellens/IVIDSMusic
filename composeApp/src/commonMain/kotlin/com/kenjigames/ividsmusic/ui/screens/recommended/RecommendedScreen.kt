package com.kenjigames.ividsmusic.ui.screens.recommended

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.ScoreEntityType
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.ui.components.LivingAlbumHero
import com.kenjigames.ividsmusic.ui.components.MusicCard
import com.kenjigames.ividsmusic.ui.components.ShimmerSkeletonRow
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent

/**
 * Displays the personalized For You feed with interest topic chips, Discovery Mix, Genre Fusion,
 * and Taste Match sections, plus Refresh Taste action.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecommendedScreen(
    viewModel: RecommendedViewModel,
    playerViewModel: PlayerViewModel,
    onOpenSongDetail: (String) -> Unit,
    onOpenArtist: (String, String) -> Unit,
    onSearchTopic: (String) -> Unit,
    onExploreMusic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalStrings.current
    val accent = LocalDynamicAccent.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item(key = "rec_hero") {
            LivingAlbumHero(
                eyebrow = strings["nav_foryou"],
                title = strings["nav_foryou"],
                subtitle = strings["foryou_desc"],
                artworkUrl = state.feed.discoveryMix.firstOrNull()?.coverUrl,
                primaryActionLabel = strings["refresh_taste"],
                onPrimaryAction = viewModel::refresh
            )
        }

        if (state.topInterests.isNotEmpty()) {
            item(key = "rec_interests") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.topInterests.forEach { score ->
                        AssistChip(
                            onClick = {
                                if (score.entityType == ScoreEntityType.ARTIST) {
                                    onOpenArtist(score.entityId, score.displayName)
                                } else {
                                    onSearchTopic(score.displayName)
                                }
                            },
                            label = {
                                Text("${score.displayName} (${score.score})", color = accent)
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = IvidsColorTokens.GrooveElevated
                            ),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }

        if (state.isLoading) {
            item(key = "rec_loading") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings["loading_recommendations"], style = MaterialTheme.typography.titleMedium, color = IvidsColorTokens.SlateMuted)
                    ShimmerSkeletonRow()
                }
            }
        } else if (!state.feed.hasHistory) {
            item(key = "rec_empty") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = IvidsColorTokens.VinylSurface,
                    border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(44.dp))
                        Text(strings["foryou_empty_title"], style = MaterialTheme.typography.titleLarge, color = IvidsColorTokens.ChalkInk)
                        Text(strings["foryou_empty_desc"], style = MaterialTheme.typography.bodyMedium, color = IvidsColorTokens.SlateMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onExploreMusic,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = IvidsColorTokens.OnAccentDark
                            )
                        ) {
                            Text(strings["explore_music"])
                        }
                    }
                }
            }
        } else {
            val sections = buildList<Pair<String, List<Track>>> {
                if (state.feed.discoveryMix.isNotEmpty()) {
                    add("Discovery Mix" to state.feed.discoveryMix)
                }
                val gfTitle = state.feed.genreFusionTitle
                if (!gfTitle.isNullOrBlank() && state.feed.genreFusionTracks.isNotEmpty()) {
                    add(gfTitle to state.feed.genreFusionTracks)
                }
                val tmTitle = state.feed.tasteMatchTitle
                if (!tmTitle.isNullOrBlank() && state.feed.tasteMatchTracks.isNotEmpty()) {
                    add(tmTitle to state.feed.tasteMatchTracks)
                }
            }
            items(sections, key = { (title, _) -> "rec_$title" }) { (title, tracks) ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(title, style = MaterialTheme.typography.headlineSmall, color = IvidsColorTokens.ChalkInk)
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(tracks, key = { "rt_${title}_${it.id}" }) { track ->
                            MusicCard(
                                title = track.title,
                                subtitle = track.artist,
                                artworkUrl = track.coverUrl,
                                onClick = { onOpenSongDetail(track.id) },
                                onPlayClick = { playerViewModel.playTrack(track, tracks) }
                            )
                        }
                    }
                }
            }
        }
    }
}
