package gr.geo.mediaplayer.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Intent
import gr.geo.mediaplayer.data.LibraryStore
import gr.geo.mediaplayer.data.Song
import gr.geo.mediaplayer.playback.PlayerConnection

/** "⋮" menu for a single song. */
@Composable
fun SongMenu(
    song: Song,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit,
    extraItems: @Composable (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val favs by LibraryStore.favorites.collectAsState()
    val isFav = favs.contains(song.id)
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Παίξε επόμενο") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null) },
            onClick = {
                PlayerConnection.playNext(listOf(song)); onDismiss()
                Toast.makeText(context, "Θα παίξει επόμενο", Toast.LENGTH_SHORT).show()
            }
        )
        DropdownMenuItem(
            text = { Text("Προσθήκη στην ουρά") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) },
            onClick = {
                PlayerConnection.addToQueue(listOf(song)); onDismiss()
                Toast.makeText(context, "Προστέθηκε στην ουρά", Toast.LENGTH_SHORT).show()
            }
        )
        DropdownMenuItem(
            text = { Text("Προσθήκη σε playlist") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
            onClick = { onDismiss(); onAddToPlaylist() }
        )
        DropdownMenuItem(
            text = { Text(if (isFav) "Αφαίρεση από αγαπημένα" else "Στα αγαπημένα") },
            leadingIcon = { Icon(if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null) },
            onClick = { LibraryStore.toggleFavorite(song.id); onDismiss() }
        )
        DropdownMenuItem(
            text = { Text("Κοινοποίηση") },
            leadingIcon = { Icon(Icons.Filled.Share, null) },
            onClick = {
                onDismiss()
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(Intent.EXTRA_STREAM, song.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, song.title))
            }
        )
        extraItems?.invoke()
    }
}

/** Pick an existing playlist or create a new one, then add [songIds] to it. */
@Composable
fun AddToPlaylistDialog(songIds: List<Long>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val playlists by LibraryStore.playlists.collectAsState()
    var creating by remember { mutableStateOf(false) }

    if (creating) {
        NameDialog(
            title = "Νέα playlist",
            initial = "",
            onDismiss = { creating = false },
            onConfirm = { name ->
                LibraryStore.createPlaylist(name, songIds)
                Toast.makeText(context, "Δημιουργήθηκε η «$name»", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Προσθήκη σε playlist") },
        text = {
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                item {
                    PlaylistPickRow("Νέα playlist", isNew = true) { creating = true }
                }
                items(playlists, key = { it.id }) { p ->
                    PlaylistPickRow("${p.name}  ·  ${p.songIds.size}") {
                        LibraryStore.addToPlaylist(p.id, songIds)
                        Toast.makeText(context, "Προστέθηκαν στην «${p.name}»", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Άκυρο") } },
    )
}

@Composable
private fun PlaylistPickRow(label: String, isNew: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(AppColors.Surface),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Icon(
                if (isNew) Icons.Filled.Add else Icons.AutoMirrored.Filled.QueueMusic, null,
                tint = AppColors.Accent
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = Color.White)
    }
}

@Composable
fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                singleLine = true, label = { Text("Όνομα") }
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Άκυρο") } },
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Άκυρο") } },
    )
}
