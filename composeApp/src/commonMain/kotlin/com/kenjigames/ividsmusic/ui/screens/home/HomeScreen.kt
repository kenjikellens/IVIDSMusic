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
import androidx.compose.runtime.Immutable
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
 * Typed quick-pick entry shown in the top Spotify-style Home grid.
 */
@Immutable
sealed interface HomeQuickPick {
    /** Quick-pick tile linking to the user's Liked Songs collection. */
    data class LikedSongs(val tracks: List<Track>) : HomeQuickPick

    /** Quick-pick tile linking to a user-created playlist. */
    data class PlaylistPick(val playlist: Playlist) : HomeQuickPick

    /** Quick-pick tile for a recent or recommended track with its playback queue context. */
    data class TrackPick(val track: Track, val queueContext: List<Track>) : HomeQuickPick
}

/**
 * Deduplicated Home screen feed projection separating quick picks, hero highlight, and remaining history.
 */
@Immutable
data class HomeFeedProjection(
    val quickPicks: List<HomeQuickPick>,
    val heroTrack: Track?,
    val remainingRecent: List<Track>
)

/**
 * Returns a case-folded `(title, artist)` key for deduplicating tracks across history and live charts.
 */
private fun Track.normalizedTrackKey(): Pair<String, String> =
    title.trim().lowercase() to artist.trim().lowercase()

/**
 * Projects [HomeUiState], [playlists], and [likedTracks] into a deduplicated Spotify-style Home feed.
 * Heals stale history cover URLs from live charts and keeps Quick Picks, Hero, and Recently Listened distinct.
 */
fun buildHomeFeedProjection(
    state: HomeUiState,
    playlists: List<Playlist>,
    likedTracks: List<Track>,
    maxQuickPicks: Int = 6
): HomeFeedProjection {
    val chartTracks = state.genreSections.flatMap { it.tracks }
    val chartCoverById = HashMap<String, String>()
    val chartCoverByKey = HashMap<Pair<String, String>, String>()
    chartTracks.forEach { track ->
        if (track.coverUrl.isNotBlank()) {
            chartCoverById.putIfAbsent(track.id, track.coverUrl)
            chartCoverByKey.putIfAbsent(track.normalizedTrackKey(), track.coverUrl)
        }
    }

    val libraryCoverById = HashMap<String, String>()
    val libraryCoverByKey = HashMap<Pair<String, String>, String>()
    (likedTracks + playlists.flatMap { it.tracks }).forEach { track ->
        if (track.coverUrl.isNotBlank()) {
            libraryCoverById.putIfAbsent(track.id, track.coverUrl)
            libraryCoverByKey.putIfAbsent(track.normalizedTrackKey(), track.coverUrl)
        }
    }

    val healedRecent = state.continueListening.map { track ->
        val freshCover = chartCoverById[track.id]
            ?: chartCoverByKey[track.normalizedTrackKey()]
            ?: track.coverUrl.ifBlank { null }
            ?: libraryCoverById[track.id]
            ?: libraryCoverByKey[track.normalizedTrackKey()]
            ?: ""
        if (freshCover != track.coverUrl) track.copy(coverUrl = freshCover) else track
    }

    val quickPicks = mutableListOf<HomeQuickPick>()
    val usedTrackIds = HashSet<String>()
    val usedTrackKeys = HashSet<Pair<String, String>>()

    if (likedTracks.isNotEmpty() && quickPicks.size < maxQuickPicks) {
        quickPicks += HomeQuickPick.LikedSongs(likedTracks)
    }
    playlists.take((maxQuickPicks - quickPicks.size).coerceAtMost(3).coerceAtLeast(0)).forEach { pl ->
        quickPicks += HomeQuickPick.PlaylistPick(pl)
    }

    healedRecent.forEach { track ->
        if (quickPicks.size >= maxQuickPicks) return@forEach
        val key = track.normalizedTrackKey()
        if (track.id !in usedTrackIds && key !in usedTrackKeys) {
            usedTrackIds += track.id
            usedTrackKeys += key
            quickPicks += HomeQuickPick.TrackPick(track, healedRecent)
        }
    }

    if (quickPicks.size < maxQuickPicks && state.genreSections.isNotEmpty()) {
        val maxDepth = state.genreSections.maxOf { it.tracks.size }
        for (depth in 0 until maxDepth) {
            if (quickPicks.size >= maxQuickPicks) break
            for (section in state.genreSections) {
                if (quickPicks.size >= maxQuickPicks) break
                val candidate = section.tracks.getOrNull(depth) ?: continue
                val key = candidate.normalizedTrackKey()
                if (candidate.id !in usedTrackIds && key !in usedTrackKeys) {
                    usedTrackIds += candidate.id
                    usedTrackKeys += key
                    quickPicks += HomeQuickPick.TrackPick(candidate, section.tracks)
                }
            }
        }
    }

    val heroCandidate = chartTracks.firstOrNull { track ->
        track.coverUrl.isNotBlank() &&
            track.id !in usedTrackIds &&
            track.normalizedTrackKey() !in usedTrackKeys
    } ?: chartTracks.firstOrNull { track ->
        track.id !in usedTrackIds && track.normalizedTrackKey() !in usedTrackKeys
    } ?: state.heroTrack

    val remainingRecent = healedRecent.filter { track ->
        track.id !in usedTrackIds && track.normalizedTrackKey() !in usedTrackKeys
    }

    return HomeFeedProjection(
        quickPicks = quickPicks,
        heroTrack = heroCandidate,
        remainingRecent = remainingRecent
    )
}

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
    val projection = remember(state, playlists, likedTracks) {
        buildHomeFeedProjection(state = state, playlists = playlists, likedTracks = likedTracks)
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

                    if (projection.quickPicks.isNotEmpty()) {
                        val columns = if (metrics.isCompactMobile) 2 else 3
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            projection.quickPicks.chunked(columns).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { pick ->
                                        when (pick) {
                                            is HomeQuickPick.LikedSongs -> SpotifyQuickPickTile(
                                                title = strings["liked_songs"],
                                                artworkUrl = null,
                                                leadingIcon = Icons.Rounded.Favorite,
                                                leadingIconTint = IvidsColorTokens.DangerCoral,
                                                onClick = onOpenLikedSongs,
                                                onPlayClick = { onPlayTrack(pick.tracks.first(), pick.tracks) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            is HomeQuickPick.PlaylistPick -> {
                                                val pl = pick.playlist
                                                SpotifyQuickPickTile(
                                                    title = pl.title,
                                                    artworkUrl = pl.tracks.firstOrNull()?.coverUrl,
                                                    leadingIcon = Icons.Rounded.LibraryMusic,
                                                    onClick = { onOpenPlaylist(pl.id) },
                                                    onPlayClick = pl.tracks.firstOrNull()?.let { first ->
                                                        { onPlayTrack(first, pl.tracks) }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            is HomeQuickPick.TrackPick -> SpotifyQuickPickTile(
                                                title = pick.track.title,
                                                subtitle = pick.track.artist,
                                                artworkUrl = pick.track.coverUrl,
                                                leadingIcon = Icons.Rounded.MusicNote,
                                                onClick = { onOpenTrack(pick.track) },
                                                onPlayClick = { onPlayTrack(pick.track, pick.queueContext) },
                                                modifier = Modifier.weight(1f)
                                            )
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
                    val hero = projection.heroTrack
                    val heroQueue = state.genreSections
                        .firstOrNull { sec -> hero != null && sec.tracks.any { it.id == hero.id } }
                        ?.tracks
                        ?: state.genreSections.firstOrNull()?.tracks
                        ?: listOfNotNull(hero)
                    LivingAlbumHero(
                        eyebrow = strings["trending_header"],
                        title = hero?.title ?: strings["welcome_title"],
                        subtitle = if (hero != null) "${hero.artist} • ${strings["welcome_desc"]}" else strings["welcome_desc"],
                        artworkUrl = hero?.coverUrl,
                        primaryActionLabel = strings["start_listening"],
                        onPrimaryAction = {
                            if (hero != null) {
                                onPlayTrack(hero, heroQueue)
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

            if (projection.remainingRecent.isNotEmpty()) {
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
                            items(projection.remainingRecent, key = { "recent_${it.id}" }) { track ->
                                MusicCard(
                                    title = track.title,
                                    subtitle = track.artist,
                                    artworkUrl = track.coverUrl,
                                    onClick = { onOpenTrack(track) },
                                    onPlayClick = { onPlayTrack(track, projection.remainingRecent) },
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
