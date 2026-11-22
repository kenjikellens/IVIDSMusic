package com.kenjigames.ividsmusic.ui.screens.song

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
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
 * Displays detailed metadata for a track alongside queue/playlist/download actions, genre chips,
 * artist albums, and similar tracks.
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (state.isLoading) {
            item(key = "song_loading") { ShimmerSkeletonRow() }
        } else if (state.track == null) {
            item(key = "song_empty") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = IvidsColorTokens.GrooveElevated,
                    border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings["song_no_track"], style = MaterialTheme.typography.titleLarge, color = IvidsColorTokens.ChalkInk)
                            Text(state.errorMessage ?: strings["song_no_track_desc"], color = IvidsColorTokens.SlateMuted)
                        }
                        Button(onClick = viewModel::load) { Text(strings["retry"]) }
                    }
                }
            }
        } else {
            val track = state.track!!
            val isLiked = track.id in likedIds || track.isLiked

            item(key = "song_hero") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        OutlinedButton(
                            onClick = { playerViewModel.enqueueLast(track) },
                            border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder),
                            modifier = Modifier.tvFocusable(shape = RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = accent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings["add_to_queue"], color = IvidsColorTokens.ChalkInk)
                        }
                        OutlinedButton(
                            onClick = { playlistModal.openPicker(track) },
                            border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder),
                            modifier = Modifier.tvFocusable(shape = RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, tint = accent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings["add_to_playlist"], color = IvidsColorTokens.ChalkInk)
                        }
                        OutlinedButton(
                            onClick = { playerViewModel.downloadTrack(track) },
                            border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder),
                            modifier = Modifier.tvFocusable(shape = RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Rounded.Download, null, tint = accent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings["download_track"], color = IvidsColorTokens.ChalkInk)
                        }
                        OutlinedButton(
                            onClick = { playerViewModel.toggleTrackLike(track) },
                            border = BorderStroke(1.dp, if (isLiked) accent else IvidsColorTokens.HairlineBorder),
                            modifier = Modifier.tvFocusable(shape = RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isLiked) accent else IvidsColorTokens.ChalkInk,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings["like"], color = IvidsColorTokens.ChalkInk)
                        }
                        OutlinedButton(
                            onClick = { onOpenArtist(track.artistId, track.artist) },
                            border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder),
                            modifier = Modifier.tvFocusable(shape = RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Rounded.Person, null, tint = accent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings["view_artist"], color = IvidsColorTokens.ChalkInk)
                        }
                        if (track.albumId.isNotBlank()) {
                            TextButton(onClick = { onOpenAlbum(track.albumId) }) {
                                Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, null, tint = accent)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(track.album.ifBlank { strings["albums"] }, color = accent)
                            }
                        }
                    }

                    if (track.genres.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            track.genres.forEach { genre ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(genre, color = accent) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = IvidsColorTokens.GrooveElevated
                                    ),
                                    border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
                                )
                            }
                        }
                    }
                }
            }

            if (state.artistAlbums.isNotEmpty()) {
                item(key = "song_artist_albums") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = strings.format("albums_from_artist", "artist" to track.artist),
                            style = MaterialTheme.typography.headlineSmall,
                            color = IvidsColorTokens.ChalkInk
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
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
