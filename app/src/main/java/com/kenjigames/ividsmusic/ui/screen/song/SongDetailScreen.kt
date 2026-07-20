package com.kenjigames.ividsmusic.ui.screen.song

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kenjigames.ividsmusic.player.PlaybackManager
import com.kenjigames.ividsmusic.ui.component.player.formatTimeMs
import com.kenjigames.ividsmusic.ui.component.shared.EmptyState
import com.kenjigames.ividsmusic.ui.theme.DarkSurface
import com.kenjigames.ividsmusic.ui.theme.DarkSurfaceVariant
import com.kenjigames.ividsmusic.ui.theme.GlassBorder
import com.kenjigames.ividsmusic.ui.theme.PrimaryAccent
import com.kenjigames.ividsmusic.ui.theme.TextMuted
import com.kenjigames.ividsmusic.ui.theme.TextPrimary
import com.kenjigames.ividsmusic.ui.theme.TextSecondary
import com.kenjigames.ividsmusic.ui.theme.Typography

/**
 * Full-screen Track Details & Expanded Player screen composable.
 * Matches all features of the original Web UI More Info / Song view.
 *
 * @param onBackClick Navigation back button handler
 * @param viewModel SongDetailViewModel instance
 */
@Composable
fun SongDetailScreen(
    onBackClick: () -> Unit = {},
    viewModel: SongDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerState by PlaybackManager.instance.playerState.collectAsState()

    val song = uiState.song ?: playerState.currentSong

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Navigation Header Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Track Information",
                style = Typography.headlineMedium,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (song == null) {
            EmptyState(
                message = "No track currently playing",
                actionLabel = "Go Back",
                onAction = onBackClick
            )
        } else {
            // Large Album Cover Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.5.dp, GlassBorder)
            ) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Song Title & Artist
            Text(
                text = song.title,
                style = Typography.headlineLarge,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artistName,
                style = Typography.titleLarge,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Time Scrubber Slider
            val progressFraction = if (playerState.durationMs > 0) {
                (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Slider(
                value = progressFraction,
                onValueChange = { fraction ->
                    val targetMs = (fraction * playerState.durationMs).toLong()
                    PlaybackManager.instance.seekTo(targetMs)
                },
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryAccent,
                    activeTrackColor = PrimaryAccent,
                    inactiveTrackColor = DarkSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTimeMs(playerState.positionMs),
                    style = Typography.bodyMedium,
                    color = TextMuted
                )
                Text(
                    text = formatTimeMs(playerState.durationMs),
                    style = Typography.bodyMedium,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = { PlaybackManager.instance.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playerState.isShuffleEnabled) PrimaryAccent else TextMuted
                    )
                }

                // Previous
                IconButton(onClick = { PlaybackManager.instance.previous() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause Button
                Surface(
                    shape = CircleShape,
                    color = PrimaryAccent,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                ) {
                    IconButton(onClick = { PlaybackManager.instance.togglePlayPause() }) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Next
                IconButton(onClick = { PlaybackManager.instance.next() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat
                IconButton(onClick = { PlaybackManager.instance.toggleRepeatOne() }) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (playerState.isRepeatOne) PrimaryAccent else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Track Information Details Card (Matching Web UI Info Specs)
            Text(text = "Metadata & Info", style = Typography.titleLarge, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow("Title", song.title)
                    InfoRow("Artist", song.artistName)
                    if (song.albumTitle.isNotEmpty()) InfoRow("Album", song.albumTitle)
                    InfoRow("Duration", "${song.durationSeconds}s (${formatTimeMs((song.durationSeconds * 1000).toLong())})")
                    InfoRow("Track ID", song.id)
                    InfoRow("Playback Status", playerState.playbackStatus)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions Bar (Like & Download)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.toggleLike() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isLiked) PrimaryAccent else DarkSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (uiState.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (uiState.isLiked) "Liked" else "Like")
                }

                Button(
                    onClick = { /* Download */ },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Download")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Download")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = Typography.bodyMedium, color = TextMuted)
        Text(
            text = value,
            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
