package com.kenjigames.ividsmusic.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.kenjigames.ividsmusic.domain.model.Playlist
import com.kenjigames.ividsmusic.ui.i18n.LocalStrings
import com.kenjigames.ividsmusic.ui.theme.IvidsColorTokens
import com.kenjigames.ividsmusic.ui.theme.LocalDynamicAccent
import com.kenjigames.ividsmusic.ui.theme.tvFocusable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Immutable transient toast message rendered by [ToastHost].
 */
data class ToastMessage(
    val id: Long,
    val text: String,
    val isError: Boolean = false
)

/**
 * Lightweight coordinator emitting non-blocking status toast notifications across all screens.
 */
@Stable
class ToastController {
    private val _current = MutableStateFlow<ToastMessage?>(null)
    val current: StateFlow<ToastMessage?> = _current.asStateFlow()
    private var seq = 0L

    /**
     * Emits a transient toast [text] banner, replacing any currently visible banner.
     */
    fun show(text: String, isError: Boolean = false) {
        seq += 1L
        _current.value = ToastMessage(id = seq, text = text, isError = isError)
    }

    /**
     * Clears the active toast message immediately.
     */
    fun dismiss() {
        _current.value = null
    }
}

/**
 * Renders animated floating toast notifications anchored at the top-center of the viewport and
 * auto-dismisses them after 2.8 seconds.
 */
@Composable
fun ToastHost(
    controller: ToastController,
    modifier: Modifier = Modifier
) {
    val toast by controller.current.collectAsState()
    val accent = LocalDynamicAccent.current

    LaunchedEffect(toast?.id) {
        if (toast != null) {
            delay(2_800L)
            controller.dismiss()
        }
    }

    Box(
        modifier = modifier.fillMaxWidth().padding(top = 18.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            toast?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (msg.isError) IvidsColorTokens.DangerCoral.copy(alpha = 0.24f) else IvidsColorTokens.GrooveElevated,
                    shadowElevation = 10.dp
                ) {
                    Text(
                        text = msg.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = IvidsColorTokens.ChalkInk,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

/**
 * Modal dialog allowing the user to create a new custom playlist with a title and optional description.
 */
/**
 * Modal dialog allowing users to create a new playlist with title and optional description.
 */
@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String) -> Unit
) {
    val strings = LocalStrings.current
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IvidsColorTokens.VinylSurface,
        title = {
            Text(strings["create_playlist"], color = IvidsColorTokens.ChalkInk)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings["playlist_name"]) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IvidsColorTokens.ChalkInk,
                        unfocusedBorderColor = IvidsColorTokens.HairlineBorder,
                        focusedTextColor = IvidsColorTokens.ChalkInk,
                        unfocusedTextColor = IvidsColorTokens.ChalkInk
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings["playlist_desc"]) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IvidsColorTokens.ChalkInk,
                        unfocusedBorderColor = IvidsColorTokens.HairlineBorder,
                        focusedTextColor = IvidsColorTokens.ChalkInk,
                        unfocusedTextColor = IvidsColorTokens.ChalkInk
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim(), description.trim())
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IvidsColorTokens.ChalkInk,
                    contentColor = IvidsColorTokens.ObsidianStage
                ),
                modifier = Modifier.then(if (name.isNotBlank()) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            ) {
                Text(strings["profile_save"])
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text(strings["close"], color = IvidsColorTokens.SlateMuted)
            }
        }
    )
}

/**
 * Modal dialog allowing the user to append a track to an existing playlist using hover-highlighted
 * [IvidsSelectableDialogItem] rows or create a new playlist.
 */
@Composable
fun PlaylistPickerDialog(
    playlists: List<Playlist>,
    onSelectPlaylist: (Playlist) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IvidsColorTokens.VinylSurface,
        title = { Text(strings["add_to_playlist"], color = IvidsColorTokens.ChalkInk) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (playlists.isEmpty()) {
                    Text(
                        text = strings["no_playlists"],
                        style = MaterialTheme.typography.bodyMedium,
                        color = IvidsColorTokens.SlateMuted
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            IvidsSelectableDialogItem(
                                selected = false,
                                onClick = { onSelectPlaylist(playlist) }
                            ) {
                                Text(
                                    text = playlist.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IvidsColorTokens.ChalkInk
                                )
                                Text(
                                    text = "${playlist.tracks.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = IvidsColorTokens.SlateMuted
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                IvidsTonalPill(
                    onClick = onCreateNew,
                    baseColor = IvidsColorTokens.RecessedTrough,
                    hoverColor = IvidsColorTokens.GlassHover,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings["create_playlist"], color = IvidsColorTokens.ChalkInk)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text(strings["close"], color = IvidsColorTokens.SlateMuted)
            }
        }
    )
}

/**
 * Reusable confirmation prompt guarding destructive actions such as playlist deletion or resetting scores.
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IvidsColorTokens.VinylSurface,
        title = { Text(title, color = IvidsColorTokens.ChalkInk) },
        text = { Text(message, color = IvidsColorTokens.SlateMuted) },
        confirmButton = {
            IvidsPrimaryButton(
                onClick = onConfirm,
                isDestructive = isDestructive,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = confirmLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isDestructive) IvidsColorTokens.ChalkInk else IvidsColorTokens.ObsidianStage
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text(strings["cancel"], color = IvidsColorTokens.SlateMuted)
            }
        }
    )
}
