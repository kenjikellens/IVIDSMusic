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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
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
import com.kenjigames.ividsmusic.ui.components.FramedScreenContainer
import com.kenjigames.ividsmusic.ui.components.IvidsTonalPill
import com.kenjigames.ividsmusic.ui.components.LivingAlbumHero
import com.kenjigames.ividsmusic.ui.components.MusicCard
import com.kenjigames.ividsmusic.ui.components.ShimmerSkeletonRow
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.LocalLayoutMetrics
import com.kenjigames.ividsmusic.ui.theme.ividsScreenPadding
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Displays the personalized For You feed inside a framed viewport (max 1360dp) using adaptive
 * mobile gutters, clickable interest pills, Discovery Mix, Genre Fusion, and Taste Match shelves.
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
    val metrics = LocalLayoutMetrics.current

    FramedScreenContainer(
        maxWidth = 1360.dp,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = ividsScreenPadding(top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        state.topInterests.forEach { score ->
                            IvidsTonalPill(
                                onClick = {
                                    if (score.entityType == ScoreEntityType.ARTIST) {
                                        onOpenArtist(score.entityId, score.displayName)
                                    } else {
                                        onSearchTopic(score.displayName)
                                    }
                                },
                                baseColor = IvidsColorTokens.GlassSurface,
                                hoverColor = IvidsColorTokens.GlassHover,
                                baseBorderColor = IvidsColorTokens.HairlineSubtle,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = "${score.displayName} (${score.score})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IvidsColorTokens.ChalkInk
                                )
                            }
                        }
                    }
                }
            }

            if (state.isLoading) {
                item(key = "rec_loading") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(strings["loading_recommendations"], style = MaterialTheme.typography.titleMedium, color = IvidsColorTokens.SlateMuted)
                        ShimmerSkeletonRow()
                    }
                }
            } else if (!state.feed.hasHistory) {
                item(key = "rec_empty") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = IvidsColorTokens.GlassSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(46.dp))
                            Text(strings["foryou_empty_title"], style = MaterialTheme.typography.titleLarge, color = IvidsColorTokens.ChalkInk)
                            Text(strings["foryou_empty_desc"], style = MaterialTheme.typography.bodyMedium, color = IvidsColorTokens.SlateMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onExploreMusic,
                                shape = CircleShape,
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
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(title, style = MaterialTheme.typography.headlineSmall, color = IvidsColorTokens.ChalkInk)
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(if (metrics.isCompactMobile) 12.dp else 16.dp)) {
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
}
