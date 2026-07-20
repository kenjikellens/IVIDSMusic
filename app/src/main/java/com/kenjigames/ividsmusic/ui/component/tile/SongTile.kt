package com.kenjigames.ividsmusic.ui.component.tile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.ui.theme.PrimaryAccent

/**
 * Concrete tile component for rendering [Song] models.
 * Specializes [BaseTile] by adding a play overlay icon over the cover artwork.
 *
 * @param song Domain song model
 * @param modifier Compose modifier
 * @param cardWidth Width of song card
 * @param onClick Click callback when song card is tapped
 */
@Composable
fun SongTile(
    song: Song,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 140.dp,
    onClick: () -> Unit = {}
) {
    BaseTile(
        coverUrl = song.coverUrl,
        title = song.title,
        subtitle = song.artistName,
        modifier = modifier,
        imageShape = RoundedCornerShape(12.dp),
        cardWidth = cardWidth,
        onClick = onClick,
        imageOverlay = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(PrimaryAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    )
}
