package com.kenjigames.ividsmusic.ui.screens.album

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import com.kenjigames.ividsmusic.ui.components.FramedScreenContainer
import com.kenjigames.ividsmusic.ui.components.IvidsTonalPill
import com.kenjigames.ividsmusic.ui.components.LivingAlbumHero
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
 * Displays the framed Album detail page (max 1360dp) with cover hero, artist link, release year,
 * batch Play/Shuffle/Save pills, and borderless tracklist rows.
 */
@Composable
fun AlbumDetailScreen(
    viewModel: AlbumViewModel,
    playerState: PlayerUiState,
    playerViewModel: PlayerViewModel,
    playlists: List<Playlist>,
    likedIds: Set<String>,
    downloadedIds: Set<String>,
    libraryRepository: LibraryRepository,
    toastController: ToastController,
    onOpenArtist: (String, String) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (state.isLoading) {
                item(key = "album_loading") { ShimmerSkeletonRow() }
            } else if (state.errorMessage != null && state.album == null) {
                item(key = "album_error") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = IvidsColorTokens.DangerCoral.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(state.errorMessage.orEmpty(), color = IvidsColorTokens.ChalkInk)
                            Button(onClick = viewModel::load, shape = CircleShape) { Text(strings["retry"]) }
                        }
                    }
                }
            } else {
                state.album?.let { album ->
                    item(key = "album_hero") {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            LivingAlbumHero(
                                eyebrow = strings["albums"],
                                title = album.title,
                                subtitle = "${album.artist} • ${album.releaseYear ?: "—"} • ${state.tracks.size} ${strings["songs"]}",
                                artworkUrl = album.coverUrl,
                                primaryActionLabel = strings["play_album"],
                                onPrimaryAction = {
                                    state.tracks.firstOrNull()?.let {
                                        playerViewModel.playTrack(it, state.tracks)
                                    }
                                },
                                secondaryActionLabel = strings["shuffle"],
                                onSecondaryAction = {
                                    if (state.tracks.isNotEmpty()) {
                                        playerViewModel.playQueue(state.tracks, shuffled = true)
                                    }
                                }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { onOpenArtist(album.artistId, album.artist) }) {
                                    Text("${strings["view_artist"]}: ${album.artist}", color = IvidsColorTokens.ChalkInk)
                                }
                                IvidsTonalPill(
                                    onClick = viewModel::toggleSaved,
                                    baseColor = if (state.isSaved) accent.copy(alpha = 0.16f) else IvidsColorTokens.GlassSurface,
                                    hoverColor = if (state.isSaved) accent.copy(alpha = 0.28f) else IvidsColorTokens.GlassHover,
                                    baseBorderColor = if (state.isSaved) accent else IvidsColorTokens.HairlineSubtle,
                                    hoverBorderColor = if (state.isSaved) accent else IvidsColorTokens.HoverBorder
                                ) {
                                    Icon(
                                        imageVector = if (state.isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (state.isSaved) accent else IvidsColorTokens.ChalkInk,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (state.isSaved) strings["saved_albums"] else strings["download_track"],
                                        style = IvidsTextStyles.tabularBadge(),
                                        color = IvidsColorTokens.ChalkInk
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(state.tracks, key = { idx, t -> "alt_${idx}_${t.id}" }) { idx, track ->
                        TrackRowItem(
                            index = idx + 1,
                            track = track,
                            isCurrentlyPlaying = playerState.currentTrack?.id == track.id,
                            isLiked = track.id in likedIds || track.isLiked,
                            isDownloaded = track.id in downloadedIds || track.isDownloaded,
                            onPlay = { playerViewModel.playTrack(track, state.tracks) },
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
            }
        }
    }
}
