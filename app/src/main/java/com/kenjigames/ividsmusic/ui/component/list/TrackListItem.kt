package com.kenjigames.ividsmusic.ui.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kenjigames.ividsmusic.domain.model.Song
import com.kenjigames.ividsmusic.ui.theme.DarkSurface
import com.kenjigames.ividsmusic.ui.theme.TextMuted
import com.kenjigames.ividsmusic.ui.theme.TextPrimary
import com.kenjigames.ividsmusic.ui.theme.TextSecondary
import com.kenjigames.ividsmusic.ui.theme.Typography

/**
 * Reusable list row composable for rendering track details in lists (search results, playlists, album tracklists).
 *
 * @param song Domain song model
 * @param modifier Compose modifier
 * @param trackNumber Optional index or track number
 * @param onClick Tap callback
 * @param onMoreClick Overflow menu callback
 */
@Composable
fun TrackListItem(
    song: Song,
    modifier: Modifier = Modifier,
    trackNumber: Int? = null,
    onClick: () -> Unit = {},
    onMoreClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track index number
        if (trackNumber != null) {
            Text(
                text = trackNumber.toString(),
                style = Typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.width(28.dp)
            )
        }

        // Cover thumbnail
        AsyncImage(
            model = song.coverUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artist column
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
        }

        // Overflow options button
        if (onMoreClick != null) {
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = TextMuted
                )
            }
        }
    }
}
