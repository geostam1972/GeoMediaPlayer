package gr.geo.mediaplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import gr.geo.mediaplayer.data.MediaRepository
import gr.geo.mediaplayer.playback.PlayerConnection
import gr.geo.mediaplayer.ui.AppColors
import gr.geo.mediaplayer.ui.ConfirmDialog
import gr.geo.mediaplayer.ui.EmptyState
import gr.geo.mediaplayer.ui.SongRow
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun QueueScreen(nav: NavHostController) {
    val s by PlayerConnection.state.collectAsState()
    var order by remember { mutableStateOf(s.queue) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(s.queue) { if (!dragging) order = s.queue }
    var dragFrom by remember { mutableIntStateOf(-1) }
    var confirmClear by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        if (s.currentIndex in s.queue.indices) listState.scrollToItem((s.currentIndex - 1).coerceAtLeast(0))
    }
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val a = order.indexOfFirst { it.key == from.key }
        val b = order.indexOfFirst { it.key == to.key }
        if (a >= 0 && b >= 0) order = order.toMutableList().apply { add(b, removeAt(a)) }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω", tint = Color.White)
            }
            Text("Ουρά", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (s.queue.isEmpty()) "0/0" else "${s.currentIndex + 1}/${s.queue.size}",
                style = MaterialTheme.typography.headlineMedium, color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { PlayerConnection.shuffleQueue() }, enabled = s.queue.size > 2) {
                Icon(Icons.Filled.Shuffle, "Ανακάτεμα ουράς", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = { confirmClear = true }, enabled = s.queue.isNotEmpty()) {
                Icon(Icons.Filled.Delete, "Καθαρισμός ουράς", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }

        if (order.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(Icons.AutoMirrored.Filled.QueueMusic, "Η ουρά είναι άδεια", "Διάλεξε ένα τραγούδι για να ξεκινήσεις.")
            }
        } else {
            LazyColumn(Modifier.weight(1f), state = listState) {
                itemsIndexed(order, key = { _, e -> e.key }) { index, entry ->
                    ReorderableItem(reorderState, key = entry.key) { isDragging ->
                        val isCurrent = entry.key == s.queue.getOrNull(s.currentIndex)?.key
                        Box {
                            if (isCurrent) {
                                Box(
                                    Modifier.align(Alignment.CenterStart).width(4.dp).height(64.dp)
                                        .background(AppColors.Accent)
                                )
                            }
                            SongRow(
                                title = entry.title,
                                subtitle = entry.artist,
                                artUri = entry.mediaId.toLongOrNull()?.let { MediaRepository.songById(it)?.uri },
                                isCurrent = isCurrent,
                                modifier = if (isDragging) Modifier.background(AppColors.SurfaceStrong) else Modifier,
                                leading = {
                                    IconButton(
                                        onClick = {},
                                        modifier = Modifier.draggableHandle(
                                            onDragStarted = {
                                                dragging = true
                                                dragFrom = order.indexOfFirst { it.key == entry.key }
                                            },
                                            onDragStopped = {
                                                val to = order.indexOfFirst { it.key == entry.key }
                                                if (dragFrom >= 0 && to >= 0 && dragFrom != to) {
                                                    PlayerConnection.moveQueueItem(dragFrom, to)
                                                }
                                                dragFrom = -1
                                                dragging = false
                                            }
                                        )
                                    ) { Icon(Icons.Filled.DragHandle, "Μετακίνηση", tint = AppColors.TextDim) }
                                },
                                trailing = {
                                    IconButton(onClick = {
                                        val i = order.indexOfFirst { it.key == entry.key }
                                        if (i >= 0) PlayerConnection.removeQueueItem(i)
                                    }) {
                                        Icon(Icons.Filled.Close, "Αφαίρεση", tint = AppColors.TextDim)
                                    }
                                },
                                onClick = { PlayerConnection.playQueueIndex(index) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            "Καθαρισμός ουράς", "Να αφαιρεθούν όλα τα τραγούδια από την ουρά;", "Καθαρισμός",
            { confirmClear = false }
        ) { PlayerConnection.clearQueue(); nav.popBackStack() }
    }
}
