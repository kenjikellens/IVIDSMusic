package com.kenjigames.ividsmusic.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.ui.components.FramedScreenContainer
import com.kenjigames.ividsmusic.ui.components.LivingAlbumHero
import com.kenjigames.ividsmusic.ui.components.MusicCard
import com.kenjigames.ividsmusic.ui.components.ShimmerSkeletonRow
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent

/**
 * Renders the Home discovery feed inside a framed viewport (max 1360dp) featuring the Living
 * Album Hero, Continue Listening shelf, Top Artists, Top Albums, and curated genre chart rows.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onOpenTrack: (Track) -> Unit,
    onOpenArtist: (artistId: String, artistName: String) -> Unit,
    onOpenAlbum: (albumId: String) -> Unit,
    onExploreTrends: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalStrings.current
    val accent = LocalDynamicAccent.current

    FramedScreenContainer(
        maxWidth = 1360.dp,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            if (!state.isHeroDismissed) {
                item(key = "hero") {
                    val hero = state.heroTrack
                    LivingAlbumHero(
                        eyebrow = strings["trending_header"],
                        title = hero?.title ?: strings["welcome_title"],
                        subtitle = if (hero != null) "${hero.artist} • ${strings["welcome_desc"]}" else strings["welcome_desc"],
                        artworkUrl = hero?.coverUrl,
                        primaryActionLabel = strings["start_listening"],
                        onPrimaryAction = {
                            if (hero != null) {
                                onPlayTrack(hero, state.genreSections.firstOrNull()?.tracks ?: listOf(hero))
                            } else {
                                onExploreTrends()
                            }
                        },
                        secondaryActionLabel = strings["explore_trends"],
                        onSecondaryAction = onExploreTrends,
                        onDismiss = viewModel::dismissHero
                    )
                }
            }

            if (state.errorMessage != null && state.genreSections.isEmpty()) {
                item(key = "error") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = IvidsColorTokens.DangerCoral.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = IvidsColorTokens.ChalkInk,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = viewModel::refresh,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent,
                                    contentColor = IvidsColorTokens.OnAccentDark
                                )
                            ) {
                                Text(strings["retry"])
                            }
                        }
                    }
                }
            }

            if (state.continueListening.isNotEmpty()) {
                item(key = "recent_history") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = strings["recently_listened"],
                            style = MaterialTheme.typography.headlineSmall,
                            color = IvidsColorTokens.ChalkInk
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(state.continueListening, key = { "recent_${it.id}" }) { track ->
                                MusicCard(
                                    title = track.title,
                                    subtitle = track.artist,
                                    artworkUrl = track.coverUrl,
                                    onClick = { onOpenTrack(track) },
                                    onPlayClick = { onPlayTrack(track, state.continueListening) }
                                )
                            }
                        }
                    }
                }
            }

            if (state.isLoading && state.genreSections.isEmpty()) {
                items(3, key = { "skeleton_$it" }) { idx ->
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = if (idx == 0) strings["top_tracks_header"] else strings["loading"],
                            style = MaterialTheme.typography.headlineSmall,
                            color = IvidsColorTokens.ChalkInk
                        )
                        ShimmerSkeletonRow(isCircular = idx == 2)
                    }
                }
            } else {
                items(state.genreSections, key = { "genre_${it.id}" }) { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = IvidsColorTokens.ChalkInk
                            )
                            TextButton(onClick = { onPlayTrack(section.tracks.first(), section.tracks) }) {
                                Text(strings["play_all"], style = MaterialTheme.typography.labelLarge, color = IvidsColorTokens.SlateMuted)
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(section.tracks, key = { "${section.id}_${it.id}" }) { track ->
                                MusicCard(
                                    title = track.title,
                                    subtitle = track.artist,
                                    artworkUrl = track.coverUrl,
                                    onClick = { onOpenTrack(track) },
                                    onPlayClick = { onPlayTrack(track, section.tracks) }
                                )
                            }
                        }
                    }
                }
            }

            if (state.topArtists.isNotEmpty()) {
                item(key = "top_artists") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = strings["top_artists_header"],
                            style = MaterialTheme.typography.headlineSmall,
                            color = IvidsColorTokens.ChalkInk
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(state.topArtists, key = { "artist_${it.id}" }) { artist ->
                                MusicCard(
                                    title = artist.name,
                                    subtitle = artist.genre.ifBlank { strings["artists"] },
                                    artworkUrl = artist.coverUrl,
                                    isCircular = true,
                                    onClick = { onOpenArtist(artist.id, artist.name) }
                                )
                            }
                        }
                    }
                }
            }

            if (state.topAlbums.isNotEmpty()) {
                item(key = "top_albums") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = strings["top_albums_header"],
                            style = MaterialTheme.typography.headlineSmall,
                            color = IvidsColorTokens.ChalkInk
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(state.topAlbums, key = { "album_${it.id}" }) { album ->
                                MusicCard(
                                    title = album.title,
                                    subtitle = album.artist,
                                    artworkUrl = album.coverUrl,
                                    badgeText = album.releaseYear?.toString(),
                                    onClick = { onOpenAlbum(album.id) }
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
