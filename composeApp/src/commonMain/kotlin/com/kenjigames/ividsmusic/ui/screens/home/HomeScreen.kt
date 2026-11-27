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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.CategoryRow
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.ui.components.CardContextMenuAction
import com.kenjigames.ividsmusic.ui.components.FramedScreenContainer
import com.kenjigames.ividsmusic.ui.components.IvidsIconButton
import com.kenjigames.ividsmusic.ui.components.IvidsPrimaryButton
import com.kenjigames.ividsmusic.ui.components.IvidsVinylLoadingBox
import com.kenjigames.ividsmusic.ui.components.LivingAlbumHero
import com.kenjigames.ividsmusic.ui.components.MusicCard
import com.kenjigames.ividsmusic.ui.components.ToastController
import com.kenjigames.ividsmusic.ui.components.formatDurationSeconds
import com.kenjigames.ividsmusic.ui.components.rememberTrackPlaylistModalState
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.LocalLayoutMetrics

/**
 * Returns a case-folded `(title, artist)` key for matching tracks across history and live charts.
 */
private fun Track.normalizedTrackKey(): Pair<String, String> =
    title.trim().lowercase() to artist.trim().lowercase()

/**
 * Heals missing or stale `coverUrl` values in [recent] history entries using live [genreSections]
 * charts first and [fallbackTracks] second, returning a fresh display list without mutating state.
 */
fun healRecentTrackCovers(
    recent: List<Track>,
    genreSections: List<CategoryRow>,
    fallbackTracks: List<Track> = emptyList()
): List<Track> {
    if (recent.isEmpty()) return emptyList()
    val chartCoverById = HashMap<String, String>()
    val chartCoverByKey = HashMap<Pair<String, String>, String>()
    genreSections.asSequence().flatMap { it.tracks.asSequence() }.forEach { track ->
        if (track.coverUrl.isNotBlank()) {
            chartCoverById.putIfAbsent(track.id, track.coverUrl)
            chartCoverByKey.putIfAbsent(track.normalizedTrackKey(), track.coverUrl)
        }
    }
    val fallbackCoverById = HashMap<String, String>()
    val fallbackCoverByKey = HashMap<Pair<String, String>, String>()
    fallbackTracks.forEach { track ->
        if (track.coverUrl.isNotBlank()) {
            fallbackCoverById.putIfAbsent(track.id, track.coverUrl)
            fallbackCoverByKey.putIfAbsent(track.normalizedTrackKey(), track.coverUrl)
        }
    }
    return recent.map { track ->
        val freshCover = chartCoverById[track.id]
            ?: chartCoverByKey[track.normalizedTrackKey()]
            ?: track.coverUrl.ifBlank { null }
            ?: fallbackCoverById[track.id]
            ?: fallbackCoverByKey[track.normalizedTrackKey()]
            ?: ""
        if (freshCover != track.coverUrl) track.copy(coverUrl = freshCover) else track
    }
}

/**
 * Formats real metadata for the featured Home [track] hero subtitle (`Artist • Album • Year • Duration`),
 * falling back to the primary genre when album metadata is absent.
 */
fun formatHeroTrackSubtitle(track: Track): String {
    val albumOrGenre = track.album.ifBlank { track.genres.firstOrNull().orEmpty() }
    val yearLabel = track.releaseYear?.takeIf { it > 1900 }?.toString().orEmpty()
    val durationLabel = if (track.durationSeconds > 0) formatDurationSeconds(track.durationSeconds) else ""
    return listOf(track.artist, albumOrGenre, yearLabel, durationLabel)
        .filter { it.isNotBlank() }
        .joinToString(" • ")
}

/**
 * Renders the Home discovery feed inside a framed viewport (max 1360dp) with a featured hero
 * banner and adaptive carousels for recent listening, genre charts, artists, and albums.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onOpenTrack: (Track) -> Unit,
    onOpenArtist: (artistId: String, artistName: String) -> Unit,
    onOpenAlbum: (albumId: String) -> Unit,
    onExploreTrends: () -> Unit,
    modifier: Modifier = Modifier,
    playlists: List<Playlist> = emptyList(),
    likedTracks: List<Track> = emptyList(),
    playerViewModel: PlayerViewModel? = null,
    libraryRepository: LibraryRepository? = null,
    toastController: ToastController? = null,
    onOpenPlaylist: (Long) -> Unit = {},
    onOpenLikedSongs: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalStrings.current
    val metrics = LocalLayoutMetrics.current
    val sidePad = metrics.screenHorizontalPadding
    val likedIds = remember(likedTracks) { likedTracks.map { it.id }.toSet() }
    val recentTracks = remember(state.continueListening, state.genreSections, playlists, likedTracks) {
        healRecentTrackCovers(
            recent = state.continueListening,
            genreSections = state.genreSections,
            fallbackTracks = likedTracks + playlists.flatMap { it.tracks }
        )
    }
    val playlistModal = if (libraryRepository != null && toastController != null) {
        rememberTrackPlaylistModalState(playlists, libraryRepository, toastController)
    } else {
        null
    }

    /** Builds desktop right-click context menu actions for a Home [track] card. */
    fun cardActionsFor(track: Track): List<CardContextMenuAction> {
        val pv = playerViewModel ?: return emptyList()
        return buildList {
            add(CardContextMenuAction(strings["play_next"]) { pv.enqueueNext(track) })
            add(CardContextMenuAction(strings["add_to_queue"]) { pv.enqueueLast(track) })
            if (playlistModal != null) {
                add(CardContextMenuAction(strings["add_to_playlist"]) { playlistModal.openPicker(track) })
            }
            add(CardContextMenuAction(strings["download_track"]) { pv.downloadTrack(track) })
            add(CardContextMenuAction(strings["view_artist"]) { onOpenArtist(track.artistId, track.artist) })
            add(CardContextMenuAction(strings["more_info"]) { onOpenTrack(track) })
        }
    }

    FramedScreenContainer(
        maxWidth = 1360.dp,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = if (metrics.isCompactMobile) 14.dp else 18.dp,
                bottom = metrics.contentBottomClearance
            ),
            verticalArrangement = Arrangement.spacedBy(if (metrics.isCompactMobile) 18.dp else 22.dp)
        ) {
            if (!state.isHeroDismissed) {
                item(key = "hero") {
                    val hero = state.heroTrack
                    val heroSubtitle = if (hero != null) {
                        formatHeroTrackSubtitle(hero).ifBlank { strings["trending_header"] }
                    } else {
                        strings["welcome_desc"]
                    }
                    LivingAlbumHero(
                        eyebrow = strings["trending_header"],
                        title = hero?.title ?: strings["welcome_title"],
                        subtitle = heroSubtitle,
                        artworkUrl = hero?.coverUrl,
                        primaryActionLabel = if (hero != null) strings["play"] else strings["start_listening"],
                        onPrimaryAction = {
                            if (hero != null) {
                                onPlayTrack(hero, state.genreSections.firstOrNull()?.tracks ?: listOf(hero))
                            } else {
                                onExploreTrends()
                            }
                        },
                        secondaryActionLabel = if (hero != null && playerViewModel != null) {
                            strings["add_to_queue"]
                        } else {
                            strings["explore_trends"]
                        },
                        onSecondaryAction = if (hero != null && playerViewModel != null) {
                            { playerViewModel.enqueueLast(hero) }
                        } else {
                            onExploreTrends
                        },
                        onDismiss = viewModel::dismissHero,
                        compact = true,
                        trailingActions = if (hero != null && playerViewModel != null) {
                            {
                                val isLiked = hero.id in likedIds || hero.isLiked
                                IvidsIconButton(
                                    onClick = { playerViewModel.toggleTrackLike(hero) },
                                    size = 38.dp
                                ) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = strings["like"],
                                        tint = if (isLiked) IvidsColorTokens.DangerCoral else IvidsColorTokens.ChalkInk,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else {
                            null
                        },
                        modifier = Modifier.padding(horizontal = sidePad)
                    )
                }
            }

            if (state.errorMessage != null && state.genreSections.isEmpty()) {
                item(key = "error") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = IvidsColorTokens.DangerCoral.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = sidePad)
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
                            IvidsPrimaryButton(onClick = viewModel::refresh) {
                                Text(strings["retry"], color = IvidsColorTokens.ObsidianStage)
                            }
                        }
                    }
                }
            }

            if (recentTracks.isNotEmpty()) {
                item(key = "recent_history") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = strings["recently_listened"],
                            style = MaterialTheme.typography.headlineSmall,
                            color = IvidsColorTokens.ChalkInk,
                            modifier = Modifier.padding(horizontal = sidePad)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = sidePad),
                            horizontalArrangement = Arrangement.spacedBy(if (metrics.isCompactMobile) 12.dp else 16.dp)
                        ) {
                            items(recentTracks, key = { "recent_${it.id}" }) { track ->
                                MusicCard(
                                    title = track.title,
                                    subtitle = track.artist,
                                    artworkUrl = track.coverUrl,
                                    onClick = { onOpenTrack(track) },
                                    onPlayClick = { onPlayTrack(track, recentTracks) },
                                    contextMenuItems = cardActionsFor(track)
                                )
                            }
                        }
                    }
                }
            }

            if (state.isLoading && state.genreSections.isEmpty()) {
                item(key = "home_loading") {
                    IvidsVinylLoadingBox(
                        label = strings["loading"],
                        modifier = Modifier.padding(horizontal = sidePad)
                    )
                }
            } else {
                items(state.genreSections, key = { "genre_${it.id}" }) { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = sidePad),
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
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = sidePad),
                            horizontalArrangement = Arrangement.spacedBy(if (metrics.isCompactMobile) 12.dp else 16.dp)
                        ) {
                            items(section.tracks, key = { "${section.id}_${it.id}" }) { track ->
                                MusicCard(
                                    title = track.title,
                                    subtitle = track.artist,
                                    artworkUrl = track.coverUrl,
                                    onClick = { onOpenTrack(track) },
                                    onPlayClick = { onPlayTrack(track, section.tracks) },
                                    contextMenuItems = cardActionsFor(track)
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
                            color = IvidsColorTokens.ChalkInk,
                            modifier = Modifier.padding(horizontal = sidePad)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = sidePad),
                            horizontalArrangement = Arrangement.spacedBy(if (metrics.isCompactMobile) 12.dp else 16.dp)
                        ) {
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
                            color = IvidsColorTokens.ChalkInk,
                            modifier = Modifier.padding(horizontal = sidePad)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = sidePad),
                            horizontalArrangement = Arrangement.spacedBy(if (metrics.isCompactMobile) 12.dp else 16.dp)
                        ) {
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
