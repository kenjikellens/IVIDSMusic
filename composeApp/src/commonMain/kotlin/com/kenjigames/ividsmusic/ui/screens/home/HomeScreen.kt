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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
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
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.domain.model.Track
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.platform.currentLocalHour24
import com.kenjigames.ividsmusic.ui.components.CardContextMenuAction
import com.kenjigames.ividsmusic.ui.components.FramedScreenContainer
import com.kenjigames.ividsmusic.ui.components.IvidsPrimaryButton
import com.kenjigames.ividsmusic.ui.components.IvidsVinylLoadingBox
import com.kenjigames.ividsmusic.ui.components.LivingAlbumHero
import com.kenjigames.ividsmusic.ui.components.MusicCard
import com.kenjigames.ividsmusic.ui.components.SpotifyQuickPickTile
import com.kenjigames.ividsmusic.ui.components.ToastController
import com.kenjigames.ividsmusic.ui.components.rememberTrackPlaylistModalState
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.LocalLayoutMetrics

/**
 * Resolves the localized greeting string key from local 24-hour clock [hour24].
 */
internal fun resolveGreetingKey(hour24: Int): String = when (hour24.coerceIn(0, 23)) {
    in 5..11 -> "greeting_morning"
    in 12..17 -> "greeting_afternoon"
    else -> "greeting_evening"
}

/**
 * Renders the Home discovery feed inside a framed viewport (max 1360dp) with a Spotify-style
 * greeting, quick-pick grid, and adaptive carousels.
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
    val greetingKey = remember { resolveGreetingKey(currentLocalHour24()) }
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
                top = if (metrics.isCompactMobile) 16.dp else 22.dp,
                bottom = metrics.contentBottomClearance
            ),
            verticalArrangement = Arrangement.spacedBy(if (metrics.isCompactMobile) 20.dp else 26.dp)
        ) {
            item(key = "home_greeting_and_picks") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = sidePad),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = strings[greetingKey],
                        style = MaterialTheme.typography.headlineMedium,
                        color = IvidsColorTokens.ChalkInk
                    )

                    val quickPickData = buildList {
                        if (likedTracks.isNotEmpty()) {
                            add(Triple("liked", strings["liked_songs"], likedTracks.firstOrNull()?.coverUrl))
                        }
                        playlists.take(3).forEach { pl ->
                            add(Triple("pl_${pl.id}", pl.title, pl.tracks.firstOrNull()?.coverUrl))
                        }
                        val remainingSlots = (6 - size).coerceAtLeast(0)
                        state.continueListening.take(remainingSlots).forEach { tr ->
                            add(Triple("tr_${tr.id}", tr.title, tr.coverUrl))
                        }
                    }.take(6)

                    if (quickPickData.isNotEmpty()) {
                        val columns = if (metrics.isCompactMobile) 2 else 3
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            quickPickData.chunked(columns).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { (key, title, art) ->
                                        when {
                                            key == "liked" -> SpotifyQuickPickTile(
                                                title = title,
                                                artworkUrl = null,
                                                leadingIcon = Icons.Rounded.Favorite,
                                                leadingIconTint = IvidsColorTokens.DangerCoral,
                                                onClick = onOpenLikedSongs,
                                                onPlayClick = { onPlayTrack(likedTracks.first(), likedTracks) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            key.startsWith("pl_") -> {
                                                val plId = key.removePrefix("pl_").toLongOrNull()
                                                val pl = playlists.firstOrNull { it.id == plId }
                                                SpotifyQuickPickTile(
                                                    title = title,
                                                    artworkUrl = art,
                                                    leadingIcon = Icons.Rounded.LibraryMusic,
                                                    onClick = { if (plId != null) onOpenPlaylist(plId) },
                                                    onPlayClick = pl?.tracks?.firstOrNull()?.let { first ->
                                                        { onPlayTrack(first, pl.tracks) }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            else -> {
                                                val trId = key.removePrefix("tr_")
                                                val tr = state.continueListening.firstOrNull { it.id == trId }
                                                SpotifyQuickPickTile(
                                                    title = title,
                                                    subtitle = tr?.artist,
                                                    artworkUrl = art,
                                                    leadingIcon = Icons.Rounded.MusicNote,
                                                    onClick = { if (tr != null) onOpenTrack(tr) },
                                                    onPlayClick = tr?.let {
                                                        { onPlayTrack(it, state.continueListening) }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                    repeat(columns - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
                        onDismiss = viewModel::dismissHero,
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

            if (state.continueListening.isNotEmpty()) {
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
                            items(state.continueListening, key = { "recent_${it.id}" }) { track ->
                                MusicCard(
                                    title = track.title,
                                    subtitle = track.artist,
                                    artworkUrl = track.coverUrl,
                                    onClick = { onOpenTrack(track) },
                                    onPlayClick = { onPlayTrack(track, state.continueListening) },
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
