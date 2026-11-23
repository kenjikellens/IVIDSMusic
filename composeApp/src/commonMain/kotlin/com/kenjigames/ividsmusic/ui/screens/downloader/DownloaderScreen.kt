package com.kenjigames.ividsmusic.ui.screens.downloader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.DownloadItem
import com.kenjigames.ividsmusic.domain.model.DownloadState
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.domain.port.LibraryRepository
import com.kenjigames.ividsmusic.ui.components.BitrateSelector
import com.kenjigames.ividsmusic.ui.components.FramedScreenContainer
import com.kenjigames.ividsmusic.ui.components.IvidsTonalPill
import com.kenjigames.ividsmusic.ui.components.ToastController
import com.kenjigames.ividsmusic.ui.components.TrackRowItem
import com.kenjigames.ividsmusic.ui.components.rememberTrackPlaylistModalState
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.player.PlayerUiState
import com.kenjigames.ividsmusic.ui.player.PlayerViewModel
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.IvidsTextStyles
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.ividsScreenPadding
import com.kenjigames.ividsmusic.ui.theme.tvFocusable

/**
 * Formats [bytes] into a crisp human-readable KB / MB / GB storage label.
 */
internal fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        val gb = ((mb / 1024.0) * 100.0).toLong() / 100.0
        "$gb GB"
    } else {
        val roundedMb = (mb * 10.0).toLong() / 10.0
        "$roundedMb MB"
    }
}

/**
 * Renders the centered Downloader & Saved Offline Media screen (max 900dp) using adaptive
 * mobile gutters, [FlowRow] action wrapping, bitrate selector, and offline file playback.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloaderScreen(
    viewModel: DownloaderViewModel,
    playerState: PlayerUiState,
    playerViewModel: PlayerViewModel,
    playlists: List<Playlist>,
    likedIds: Set<String>,
    libraryRepository: LibraryRepository,
    toastController: ToastController,
    isDesktopPlatform: Boolean,
    onOpenArtist: (String, String) -> Unit,
    onOpenSongDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalStrings.current
    val accent = LocalDynamicAccent.current
    val playlistModal = rememberTrackPlaylistModalState(playlists, libraryRepository, toastController)

    FramedScreenContainer(
        maxWidth = 900.dp,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = ividsScreenPadding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item(key = "dl_header") {
                Text(
                    text = strings["home_header"],
                    style = MaterialTheme.typography.headlineMedium,
                    color = IvidsColorTokens.ChalkInk
                )
            }

            item(key = "dl_config_panel") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = IvidsColorTokens.GlassSurface
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(strings["home_url_label"], style = MaterialTheme.typography.titleMedium, color = IvidsColorTokens.ChalkInk)
                            OutlinedTextField(
                                value = state.urlInput,
                                onValueChange = viewModel::onUrlChanged,
                                placeholder = { Text(strings["home_url_placeholder"], color = IvidsColorTokens.SlateMuted) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = IvidsColorTokens.RecessedTrough,
                                    unfocusedContainerColor = IvidsColorTokens.RecessedTrough,
                                    focusedBorderColor = accent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = IvidsColorTokens.ChalkInk,
                                    unfocusedTextColor = IvidsColorTokens.ChalkInk
                                ),
                                modifier = Modifier.fillMaxWidth().tvFocusable(shape = RoundedCornerShape(14.dp))
                            )
                        }

                        HorizontalDivider(color = IvidsColorTokens.HairlineSubtle)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                Text(strings["home_save_label"], style = MaterialTheme.typography.titleMedium, color = IvidsColorTokens.ChalkInk)
                                Text(
                                    text = state.saveDirectoryPath,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IvidsColorTokens.SlateMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isDesktopPlatform) {
                                IvidsTonalPill(
                                    onClick = viewModel::browseOutputFolder,
                                    baseColor = IvidsColorTokens.RecessedTrough,
                                    hoverColor = IvidsColorTokens.GlassHover,
                                    baseBorderColor = IvidsColorTokens.HairlineSubtle
                                ) {
                                    Icon(Icons.Rounded.FolderOpen, null, tint = IvidsColorTokens.SlateMuted, modifier = Modifier.size(16.dp))
                                    Text(strings["home_browse"], style = MaterialTheme.typography.labelLarge, color = IvidsColorTokens.ChalkInk)
                                }
                            }
                        }

                        HorizontalDivider(color = IvidsColorTokens.HairlineSubtle)

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(strings["home_quality"], style = MaterialTheme.typography.titleMedium, color = IvidsColorTokens.ChalkInk)
                            BitrateSelector(
                                selected = state.selectedQuality,
                                onSelect = viewModel::selectQuality
                            )
                        }

                        if (state.errorMessage != null) {
                            Text(
                                text = state.errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = IvidsColorTokens.DangerCoral
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = viewModel::startDownload,
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent,
                                    contentColor = IvidsColorTokens.OnAccentDark
                                ),
                                modifier = Modifier.tvFocusable(shape = CircleShape)
                            ) {
                                Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${strings["home_start"]} (${state.selectedQuality.label})",
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            IvidsTonalPill(
                                onClick = { if (!state.isLoadingPreview) viewModel.loadPreview() },
                                baseColor = IvidsColorTokens.RecessedTrough,
                                hoverColor = IvidsColorTokens.GlassHover,
                                baseBorderColor = IvidsColorTokens.HairlineSubtle,
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = if (state.isLoadingPreview) strings["loading"] else strings["preview_load"],
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IvidsColorTokens.ChalkInk,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            if (state.previewTracks.isNotEmpty()) {
                item(key = "dl_preview_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${state.previewTracks.size} ${strings["preview_tracks_found"]}",
                            style = MaterialTheme.typography.titleMedium,
                            color = IvidsColorTokens.ChalkInk
                        )
                        Row {
                            TextButton(onClick = { viewModel.selectAllPreview(true) }) {
                                Text(strings["preview_select_all"], color = IvidsColorTokens.ChalkInk)
                            }
                            TextButton(onClick = { viewModel.selectAllPreview(false) }) {
                                Text(strings["preview_deselect_all"], color = IvidsColorTokens.SlateMuted)
                            }
                        }
                    }
                }
                items(state.previewTracks, key = { "prev_${it.id}" }) { track ->
                    val checked = track.id in state.selectedPreviewIds
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.togglePreviewSelection(track.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = IvidsColorTokens.GlassSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { viewModel.togglePreviewSelection(track.id) },
                                colors = CheckboxDefaults.colors(checkedColor = accent)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, style = MaterialTheme.typography.titleSmall, color = IvidsColorTokens.ChalkInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = IvidsColorTokens.SlateMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            if (state.activeDownloads.isNotEmpty()) {
                item(key = "dl_active_header") {
                    Text(strings["status_downloading"], style = MaterialTheme.typography.headlineSmall, color = IvidsColorTokens.ChalkInk)
                }
                items(state.activeDownloads, key = { "dl_${it.id}" }) { item ->
                    ActiveDownloadRow(item = item, onCancel = { viewModel.cancelDownload(item.id) })
                }
            }

            item(key = "dl_storage_bar") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = IvidsColorTokens.GlassSurface
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(strings["storage_used"], style = MaterialTheme.typography.titleMedium, color = IvidsColorTokens.ChalkInk)
                            Text(
                                text = "${formatBytes(state.storageUsedBytes)} • ${state.savedTracks.size} ${strings["saved_tracks"]}",
                                style = IvidsTextStyles.tabularBadge(),
                                color = IvidsColorTokens.SlateMuted
                            )
                        }
                        IvidsTonalPill(
                            onClick = viewModel::rescanSavedDirectory,
                            baseColor = IvidsColorTokens.RecessedTrough,
                            hoverColor = IvidsColorTokens.GlassHover,
                            baseBorderColor = IvidsColorTokens.HairlineSubtle
                        ) {
                            Icon(Icons.Rounded.Refresh, null, tint = IvidsColorTokens.SlateMuted, modifier = Modifier.size(16.dp))
                            Text(
                                text = strings["rescan_saved"],
                                style = MaterialTheme.typography.labelLarge,
                                color = IvidsColorTokens.ChalkInk,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            if (state.savedTracks.isNotEmpty()) {
                item(key = "dl_saved_header") {
                    Text(strings["saved_tracks"], style = MaterialTheme.typography.headlineSmall, color = IvidsColorTokens.ChalkInk)
                }
                itemsIndexed(state.savedTracks, key = { idx, t -> "sav_${idx}_${t.id}" }) { idx, track ->
                    TrackRowItem(
                        index = idx + 1,
                        track = track,
                        isCurrentlyPlaying = playerState.currentTrack?.id == track.id,
                        isLiked = track.id in likedIds || track.isLiked,
                        isDownloaded = true,
                        onPlay = { playerViewModel.playTrack(track, state.savedTracks) },
                        onToggleLike = { playerViewModel.toggleTrackLike(track) },
                        onPlayNext = { playerViewModel.enqueueNext(track) },
                        onAddToQueue = { playerViewModel.enqueueLast(track) },
                        onAddToPlaylist = { playlistModal.openPicker(track) },
                        onDownload = { playerViewModel.downloadTrack(track, state.selectedQuality) },
                        onOpenArtist = { onOpenArtist(track.artistId, track.artist) },
                        onOpenSongDetail = { onOpenSongDetail(track.id) },
                        trailingContent = {
                            IconButton(
                                onClick = { viewModel.deleteOfflineTrack(track.id) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, strings["remove"], tint = IvidsColorTokens.DangerCoral, modifier = Modifier.size(18.dp))
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Renders live download progress, speed, transcoding state, and cancellation control for a [DownloadItem].
 */
@Composable
private fun ActiveDownloadRow(
    item: DownloadItem,
    onCancel: () -> Unit
) {
    val strings = LocalStrings.current
    val accent = LocalDynamicAccent.current
    val (statusLabel, progress, canCancel) = when (val st = item.state) {
        is DownloadState.Idle -> Triple(strings["status_idle"], null, false)
        is DownloadState.Queued -> Triple("${strings["queue"]} (${st.quality.label})", null, true)
        is DownloadState.Resolving -> Triple("${strings["preparing"]} (${st.quality.label})", null, true)
        is DownloadState.Downloading -> {
            val pct = (st.progress * 100).toInt().coerceIn(0, 100)
            val kbPerSec = st.bytesPerSec / 1024L
            Triple("$pct% • $kbPerSec KB/s", st.progress.coerceIn(0f, 1f), true)
        }
        is DownloadState.Transcoding -> Triple("Transcoding -> ${st.targetQuality.label}", null, true)
        is DownloadState.Completed -> Triple("${strings["dl_badge_done"]} (${st.quality.label})", 1f, false)
        is DownloadState.Failed -> Triple("${strings["dl_badge_failed"]}: ${st.error.message}", null, false)
        is DownloadState.Cancelled -> Triple(strings["dl_meta_cancelled"], null, false)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = IvidsColorTokens.GlassSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.track.title, style = MaterialTheme.typography.titleMedium, color = IvidsColorTokens.ChalkInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(statusLabel, style = IvidsTextStyles.tabularBadge(), color = IvidsColorTokens.SlateMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (canCancel) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Rounded.Cancel, strings["home_cancel"], tint = IvidsColorTokens.DangerCoral)
                    }
                }
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = accent,
                    trackColor = IvidsColorTokens.RecessedTrough,
                    modifier = Modifier.fillMaxWidth().clip(CircleShape)
                )
            } else if (canCancel) {
                LinearProgressIndicator(
                    color = accent,
                    trackColor = IvidsColorTokens.RecessedTrough,
                    modifier = Modifier.fillMaxWidth().clip(CircleShape)
                )
            }
        }
    }
}
