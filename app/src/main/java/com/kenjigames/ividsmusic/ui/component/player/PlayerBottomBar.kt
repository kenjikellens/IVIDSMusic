package com.kenjigames.ividsmusic.ui.component.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kenjigames.ividsmusic.domain.model.PlayerState
import com.kenjigames.ividsmusic.ui.theme.DarkSurfaceVariant
import com.kenjigames.ividsmusic.ui.theme.GlassBorder
import com.kenjigames.ividsmusic.ui.theme.PrimaryAccent
import com.kenjigames.ividsmusic.ui.theme.TextMuted
import com.kenjigames.ividsmusic.ui.theme.TextPrimary
import com.kenjigames.ividsmusic.ui.theme.TextSecondary
import com.kenjigames.ividsmusic.ui.theme.Typography

/**
 * Reusable helper to format millisecond duration into "M:SS" string format.
 */
fun formatTimeMs(millis: Long): String {
    if (millis <= 0) return "0:00"
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    return String.format("%d:%02d", minutes, seconds)
}

/**
 * Persistent bottom player bar composable matching the full feature set of the original Web UI player bar.
 *
 * @param playerState Current player state
 * @param modifier Compose modifier
 * @param onClick Action executed when player bar is tapped (opens Song Detail screen)
 * @param onTogglePlayPause Action toggling play/pause
 * @param onNext Action skipping to next track
 * @param onToggleLike Action toggling song like state
 * @param onDownload Action triggering audio download
 */
@Composable
fun PlayerBottomBar(
    playerState: PlayerState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onTogglePlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onToggleLike: () -> Unit = {},
    onDownload: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = playerState.currentSong != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        val song = playerState.currentSong ?: return@AnimatedVisibility

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album Cover Image
                    AsyncImage(
                        model = song.coverUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Track Title & Artist & Status Text
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = Typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artistName,
                            style = Typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (playerState.playbackStatus.isNotEmpty()) {
                            Text(
                                text = "${playerState.playbackStatus} • ${formatTimeMs(playerState.positionMs)} / ${formatTimeMs(playerState.durationMs)}",
                                style = Typography.labelMedium,
                                color = PrimaryAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Download Action Button
                    IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = if (song.isDownloaded) PrimaryAccent else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Like Action Button
                    IconButton(onClick = onToggleLike, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (song.isLiked) PrimaryAccent else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // More Info Button (Navigates to Song Detail)
                    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "More Info",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play/Pause Button
                    IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Next Button
                    IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Progress Indicator Bar
                if (playerState.durationMs > 0) {
                    val progressFraction = (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = PrimaryAccent,
                        trackColor = Color.Transparent
                    )
                }
            }
        }
    }
}
