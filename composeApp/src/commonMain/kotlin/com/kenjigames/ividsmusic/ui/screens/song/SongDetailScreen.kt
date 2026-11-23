package com.kenjigames.ividsmusic.ui.screens.song

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.ui.components.FramedScreenContainer
import com.kenjigames.ividsmusic.ui.components.IvidsTonalPill
import com.kenjigames.ividsmusic.ui.components.LivingAlbumHero
import com.kenjigames.ividsmusic.ui.components.MusicCard
import com.kenjigames.ividsmusic.ui.components.ShimmerSkeletonRow
import com.kenjigames.ividsmusic.ui.components.ToastController
import com.kenjigames.ividsmusic.ui.components.TrackRowItem
import com.kenjigames.ividsmusic.ui.components.formatDurationSeconds
import com.kenjigames.ividsmusic.ui.components.rememberTrackPlaylistModalState
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.player.PlayerUiState
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Displays detailed metadata for a track inside a framed viewport (max 1360dp) alongside
 * tonal pill actions, genre chips, artist albums, and similar tracks.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SongDetailScreen(
    viewModel: SongDetailViewModel,
    playerState: PlayerUiState,
    playerViewModel: PlayerViewModel,
    playlists: List<Playlist>,
    likedIds: Set<String>,
    downloadedIds: Set<String>,
    libraryRepository: LibraryRepository,
    toastController: ToastController,
    onOpenArtist: (String, String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenSongDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalStrings.current
    val accent = LocalDynamicAccent.current
    val playlistModal = rememberTrackPlaylistModalState(playlists, libraryRepository, toastController)

    FramedScreenContainer(
        maxWidth = 1360.dp,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            if (state.isLoading) {
                item(key = "song_loading") { ShimmerSkeletonRow() }
            } else if (state.track == null) {
                item(key = "song_empty") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = IvidsColorTokens.GlassSurface
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(22.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings["song_no_track"], style = MaterialTheme.typography.titleLarge, color = IvidsColorTokens.ChalkInk)
                                Text(state.errorMessage ?: strings["song_no_track_desc"], color = IvidsColorTokens.SlateMuted)
                            }
                            Button(onClick = viewModel::load, shape = CircleShape) { Text(strings["retry"]) }
                        }
                    }
                }
            } else {
                val track = state.track!!
                val isLiked = track.id in likedIds || track.isLiked

                item(key = "song_hero") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        LivingAlbumHero(
                            eyebrow = strings["song"],
                            title = track.title,
                            subtitle = "${track.artist} • ${track.album.ifBlank { "Single" }} • ${track.releaseYear ?: "—"} • ${formatDurationSeconds(track.durationSeconds)}",
                            artworkUrl = track.coverUrl,
                            primaryActionLabel = strings["play"],
                            onPrimaryAction = {
                                playerViewModel.playTrack(track, listOf(track) + state.similarTracks)
                            },
                            secondaryActionLabel = strings["play_next"],
                            onSecondaryAction = { playerViewModel.enqueueNext(track) }
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SongActionPill(
                                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                label = strings["add_to_queue"],
                                accent = accent,
                                onClick = { playerViewModel.enqueueLast(track) }
                            )
                            SongActionPill(
                                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                label = strings["add_to_playlist"],
                                accent = accent,
                                onClick = { playlistModal.openPicker(track) }
                            )
                            SongActionPill(
                                icon = Icons.Rounded.Download,
                                label = strings["download_track"],
                                accent = accent,
                                onClick = { playerViewModel.downloadTrack(track) }
                            )
                            SongActionPill(
                                icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                label = strings["like"],
                                accent = accent,
                                highlighted = isLiked,
                                onClick = { playerViewModel.toggleTrackLike(track) }
                            )
                            SongActionPill(
                                icon = Icons.Rounded.Person,
                                label = strings["view_artist"],
                                accent = accent,
                                onClick = { onOpenArtist(track.artistId, track.artist) }
                            )
                            if (track.albumId.isNotBlank()) {
                                SongActionPill(
                                    icon = Icons.Rounded.Album,
                                    label = track.album.ifBlank { strings["albums"] },
                                    accent = accent,
                                    onClick = { onOpenAlbum(track.albumId) }
                                )
                            }
                        }

                        if (track.genres.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                track.genres.forEach { genre ->
                                    Surface(
                                        shape = CircleShape,
                                        color = IvidsColorTokens.GlassSurface
                                    ) {
                                        Text(
                                            text = genre,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = IvidsColorTokens.SlateMuted,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (state.artistAlbums.isNotEmpty()) {
                    item(key = "song_artist_albums") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = strings.format("albums_from_artist", "artist" to track.artist),
                                style = MaterialTheme.typography.headlineSmall,
                                color = IvidsColorTokens.ChalkInk
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(state.artistAlbums, key = { "sa_${it.id}" }) { album ->
                                    MusicCard(
                                        title = album.title,
                                        subtitle = album.releaseYear?.toString() ?: album.artist,
                                        artworkUrl = album.coverUrl,
                                        onClick = { onOpenAlbum(album.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.similarTracks.isNotEmpty()) {
                    item(key = "similar_header") {
                        Text(
                            text = strings["similar_tracks"],
                            style = MaterialTheme.typography.headlineSmall,
                            color = IvidsColorTokens.ChalkInk
                        )
                    }
                    itemsIndexed(state.similarTracks, key = { idx, t -> "sim_${idx}_${t.id}" }) { idx, sim ->
                        TrackRowItem(
                            index = idx + 1,
                            track = sim,
                            isCurrentlyPlaying = playerState.currentTrack?.id == sim.id,
                            isLiked = sim.id in likedIds || sim.isLiked,
                            isDownloaded = sim.id in downloadedIds || sim.isDownloaded,
                            onPlay = { playerViewModel.playTrack(sim, state.similarTracks) },
                            onToggleLike = { playerViewModel.toggleTrackLike(sim) },
                            onPlayNext = { playerViewModel.enqueueNext(sim) },
                            onAddToQueue = { playerViewModel.enqueueLast(sim) },
                            onAddToPlaylist = { playlistModal.openPicker(sim) },
                            onDownload = { playerViewModel.downloadTrack(sim) },
                            onOpenArtist = { onOpenArtist(sim.artistId, sim.artist) },
                            onOpenSongDetail = { onOpenSongDetail(sim.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders a sleek tonal pill button used in [SongDetailScreen]'s action bar.
 */
@Composable
private fun SongActionPill(
    icon: ImageVector,
    label: String,
    accent: Color,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    IvidsTonalPill(
        onClick = onClick,
        baseColor = if (highlighted) accent.copy(alpha = 0.16f) else IvidsColorTokens.GlassSurface,
        hoverColor = if (highlighted) accent.copy(alpha = 0.28f) else IvidsColorTokens.GlassHover,
        baseBorderColor = if (highlighted) accent else IvidsColorTokens.HairlineSubtle,
        hoverBorderColor = if (highlighted) accent else IvidsColorTokens.HoverBorder
    ) {
        Icon(icon, null, tint = if (highlighted) accent else IvidsColorTokens.SlateMuted, modifier = Modifier.size(18.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (highlighted) accent else IvidsColorTokens.ChalkInk
        )
    }
}
