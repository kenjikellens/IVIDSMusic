package com.kenjigames.ividsmusic.ui.screens.artist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.ui.components.LivingAlbumHero
import com.kenjigames.ividsmusic.ui.components.MusicCard
import com.kenjigames.ividsmusic.ui.components.ShimmerSkeletonRow
import com.kenjigames.ividsmusic.ui.components.ToastController
import com.kenjigames.ividsmusic.ui.components.TrackRowItem
import com.kenjigames.ividsmusic.ui.components.rememberTrackPlaylistModalState
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.player.PlayerUiState
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Displays the Artist detail page with hero header, fan/album counts, Play Top Track / Shuffle /
 * Like Artist actions, discography albums row, popular songs list, and related artists.
 */
@Composable
fun ArtistDetailScreen(
    viewModel: ArtistViewModel,
    playerState: PlayerUiState,
    playerViewModel: PlayerViewModel,
    playlists: List<Playlist>,
    likedIds: Set<String>,
    downloadedIds: Set<String>,
    libraryRepository: LibraryRepository,
    toastController: ToastController,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String, String) -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        if (state.isLoading) {
            item(key = "artist_loading") { ShimmerSkeletonRow(isCircular = true) }
        } else if (state.errorMessage != null && state.artist == null) {
            item(key = "artist_error") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = IvidsColorTokens.GrooveElevated,
                    border = BorderStroke(1.dp, IvidsColorTokens.DangerCoral.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(state.errorMessage.orEmpty(), color = IvidsColorTokens.ChalkInk)
                        Button(onClick = viewModel::load) { Text(strings["retry"]) }
                    }
                }
            }
        } else {
            state.artist?.let { artist ->
                item(key = "artist_hero") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LivingAlbumHero(
                            eyebrow = strings["view_artist"],
                            title = artist.name,
                            subtitle = "${artist.fanCount} fans • ${state.albums.size} ${strings["albums"]}",
                            artworkUrl = artist.coverUrl,
                            primaryActionLabel = strings["play_top_track"],
                            onPrimaryAction = {
                                state.topTracks.firstOrNull()?.let {
                                    playerViewModel.playTrack(it, state.topTracks)
                                }
                            },
                            secondaryActionLabel = strings["shuffle"],
                            onSecondaryAction = {
                                if (state.topTracks.isNotEmpty()) {
                                    playerViewModel.playQueue(state.topTracks, shuffled = true)
                                }
                            }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (artist.coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = artist.coverUrl,
                                    contentDescription = artist.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                )
                            }
                            Text(
                                text = "${artist.fanCount} • ${state.topTracks.size} ${strings["songs"]}",
                                style = IvidsTextStyles.tabularBadge(),
                                color = IvidsColorTokens.SlateMuted,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = viewModel::likeArtist,
                                border = BorderStroke(1.dp, if (state.isLikedArtist) accent else IvidsColorTokens.HairlineBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = IvidsColorTokens.GrooveElevated,
                                    contentColor = if (state.isLikedArtist) accent else IvidsColorTokens.ChalkInk
                                ),
                                modifier = Modifier.tvFocusable(shape = RoundedCornerShape(12.dp))
                            ) {
                                Icon(
                                    imageVector = if (state.isLikedArtist) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings["like"])
                            }
                            OutlinedButton(
                                onClick = {
                                    if (state.topTracks.isNotEmpty()) {
                                        playerViewModel.playQueue(state.topTracks, shuffled = true)
                                    }
                                },
                                border = BorderStroke(1.dp, IvidsColorTokens.HairlineBorder),
                                modifier = Modifier.tvFocusable(shape = RoundedCornerShape(12.dp))
                            ) {
                                Icon(Icons.Rounded.Shuffle, null, tint = accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings["shuffle"], color = IvidsColorTokens.ChalkInk)
                            }
                        }
                    }
                }

                if (state.albums.isNotEmpty()) {
                    item(key = "artist_albums") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = strings.format("albums_from_artist", "artist" to artist.name),
                                style = MaterialTheme.typography.headlineSmall,
                                color = IvidsColorTokens.ChalkInk
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(state.albums, key = { "aa_${it.id}" }) { album ->
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

                if (state.topTracks.isNotEmpty()) {
                    item(key = "popular_header") {
                        Text(
                            text = strings.format("popular_songs_from_artist", "artist" to artist.name),
                            style = MaterialTheme.typography.headlineSmall,
                            color = IvidsColorTokens.ChalkInk
                        )
                    }
                    itemsIndexed(state.topTracks, key = { idx, t -> "at_${idx}_${t.id}" }) { idx, track ->
                        TrackRowItem(
                            index = idx + 1,
                            track = track,
                            isCurrentlyPlaying = playerState.currentTrack?.id == track.id,
                            isLiked = track.id in likedIds || track.isLiked,
                            isDownloaded = track.id in downloadedIds || track.isDownloaded,
                            onPlay = { playerViewModel.playTrack(track, state.topTracks) },
                            onToggleLike = { playerViewModel.toggleTrackLike(track) },
                            onPlayNext = { playerViewModel.enqueueNext(track) },
                            onAddToQueue = { playerViewModel.enqueueLast(track) },
                            onAddToPlaylist = { playlistModal.openPicker(track) },
                            onDownload = { playerViewModel.downloadTrack(track) },
                            onOpenArtist = { onOpenArtist(track.artistId, track.artist) },
                            onOpenSongDetail = { onOpenSongDetail(track.id) }
                        )
                    }
                }

                if (state.relatedArtists.isNotEmpty()) {
                    item(key = "related_artists") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = strings["top_artists"],
                                style = MaterialTheme.typography.headlineSmall,
                                color = IvidsColorTokens.ChalkInk
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(state.relatedArtists, key = { "ra_${it.id}" }) { peer ->
                                    MusicCard(
                                        title = peer.name,
                                        subtitle = strings["artists"],
                                        artworkUrl = peer.coverUrl,
                                        isCircular = true,
                                        onClick = { onOpenArtist(peer.id, peer.name) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
