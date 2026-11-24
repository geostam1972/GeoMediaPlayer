package gr.geo.mediaplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import gr.geo.mediaplayer.data.LibraryStore
import gr.geo.mediaplayer.data.MediaRepository
import gr.geo.mediaplayer.data.Song
import gr.geo.mediaplayer.data.formatDuration
import gr.geo.mediaplayer.playback.PlayerConnection
import gr.geo.mediaplayer.ui.AddToPlaylistDialog
import gr.geo.mediaplayer.ui.AppColors
import gr.geo.mediaplayer.ui.CircleCheck
import gr.geo.mediaplayer.ui.ConfirmDialog
import gr.geo.mediaplayer.ui.EmptyState
import gr.geo.mediaplayer.ui.SearchField
import gr.geo.mediaplayer.ui.SongMenu
import gr.geo.mediaplayer.ui.SongRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import gr.geo.mediaplayer.ui.Artwork
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun PlaylistDetailScreen(nav: NavHostController, kind: String, playlistId: Long) {
    val all by MediaRepository.songs.collectAsState()
    val favIds by LibraryStore.favorites.collectAsState()
    val recent by LibraryStore.recent.collectAsState()
    val counts by LibraryStore.playCounts.collectAsState()
    val playlists by LibraryStore.playlists.collectAsState()
    val player by PlayerConnection.state.collectAsState()

    val userPlaylist = playlists.firstOrNull { it.id == playlistId }
    val title = when (kind) {
        "fav" -> "Αγαπημένα"
        "added" -> "Πρόσφατα προστέθηκαν"
        "recent" -> "Πρόσφατα παιγμένα"
        "most" -> "Πιο παιγμένα"
        "daily" -> "Ημερήσια playlist"
        else -> userPlaylist?.name ?: "Playlist"
    }
    val songs: List<Song> = remember(kind, all, favIds, recent, counts, userPlaylist) {
        when (kind) {
            "fav" -> SmartLists.favorites(all, favIds)
            "added" -> SmartLists.recentlyAdded(all)
            "recent" -> SmartLists.recentlyPlayed(all, recent)
            "most" -> SmartLists.mostPlayed(all, counts)
            "daily" -> SmartLists.daily(all)
            else -> userPlaylist?.let { SmartLists.user(all, it) } ?: emptyList()
        }
    }
    val isUser = kind == "user" && userPlaylist != null

    var menuFor by remember { mutableStateOf<Long?>(null) }
    var addToPlaylistIds by remember { mutableStateOf<List<Long>?>(null) }
    var picking by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    // Local copy used while dragging to reorder (user playlists only)
    var order by remember { mutableStateOf(songs) }
    LaunchedEffect(songs) { order = songs }
    var dragFrom by remember { mutableStateOf(-1) }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIdx = order.indexOfFirst { it.id == from.key }
        val toIdx = order.indexOfFirst { it.id == to.key }
        if (fromIdx >= 0 && toIdx >= 0) {
            order = order.toMutableList().apply { add(toIdx, removeAt(fromIdx)) }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineMedium, color = Color.White, maxLines = 1)
                Text("${songs.size} τραγούδια", color = AppColors.TextDim)
            }
            if (isUser) {
                IconButton(onClick = { picking = true }) {
                    Icon(Icons.Filled.Add, "Προσθήκη τραγουδιών", tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
            if ((kind == "recent" || kind == "most") && songs.isNotEmpty()) {
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Filled.DeleteSweep, "Καθαρισμός", tint = Color.White)
                }
            }
        }
        if (songs.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PillButton("Αναπαραγωγή", Icons.Filled.PlayArrow, true, Modifier.weight(1f)) {
                    PlayerConnection.playSongs(songs)
                }
                PillButton("Τυχαία", Icons.Filled.Shuffle, false, Modifier.weight(1f)) {
                    PlayerConnection.playSongs(songs, shuffle = true)
                }
            }
        }

        if (songs.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    Icons.Filled.MusicOff, "Άδεια λίστα",
                    when (kind) {
                        "fav" -> "Πάτα την καρδιά σε ένα τραγούδι για να μπει εδώ."
                        "user" -> "Πάτα το + πάνω δεξιά για να προσθέσεις τραγούδια."
                        "added" -> "Τραγούδια που προστέθηκαν τις τελευταίες 30 μέρες."
                        else -> "Παίξε λίγη μουσική και θα εμφανιστεί εδώ."
                    }
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f), state = listState) {
                items(if (isUser) order else songs, key = { it.id }) { song ->
                    ReorderableItem(reorderState, key = song.id, enabled = isUser) { dragging ->
                        val rowIndex = (if (isUser) order else songs).indexOf(song)
                        SongRow(
                            title = song.title,
                            subtitle = "${song.artist}  ·  ${formatDuration(song.durationMs)}",
                            artUri = song.uri,
                            isCurrent = player.mediaId == song.id.toString(),
                            modifier = if (dragging) Modifier.background(AppColors.SurfaceStrong) else Modifier,
                            leading = if (isUser) ({
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.draggableHandle(
                                        onDragStarted = { dragFrom = order.indexOf(song) },
                                        onDragStopped = {
                                            val to = order.indexOf(song)
                                            if (dragFrom >= 0 && to >= 0 && dragFrom != to && userPlaylist != null) {
                                                LibraryStore.movePlaylistSong(userPlaylist.id, dragFrom, to)
                                            }
                                            dragFrom = -1
                                        }
                                    )
                                ) { Icon(Icons.Filled.DragHandle, "Μετακίνηση", tint = AppColors.TextDim) }
                            }) else null,
                            trailing = {
                                Box {
                                    IconButton(onClick = { menuFor = song.id }) {
                                        Icon(Icons.Filled.MoreVert, "Επιλογές", tint = AppColors.TextDim)
                                    }
                                    SongMenu(
                                        song, menuFor == song.id, { menuFor = null },
                                        onAddToPlaylist = { addToPlaylistIds = listOf(song.id) },
                                        extraItems = if (isUser) ({
                                            DropdownMenuItem(
                                                text = { Text("Αφαίρεση από τη playlist") },
                                                leadingIcon = { Icon(Icons.Filled.RemoveCircleOutline, null) },
                                                onClick = {
                                                    menuFor = null
                                                    userPlaylist?.let { LibraryStore.removeFromPlaylist(it.id, song.id) }
                                                }
                                            )
                                        }) else null
                                    )
                                }
                            },
                            onClick = { PlayerConnection.playSongs(if (isUser) order else songs, rowIndex) }
                        )
                    }
                }
            }
        }
    }

    addToPlaylistIds?.let { ids -> AddToPlaylistDialog(ids) { addToPlaylistIds = null } }

    if (picking && userPlaylist != null) {
        SongPickerDialog(
            all = all,
            alreadyIn = userPlaylist.songIds.toSet(),
            onDismiss = { picking = false },
            onConfirm = { ids -> LibraryStore.addToPlaylist(userPlaylist.id, ids); picking = false }
        )
    }

    if (confirmClear) {
        ConfirmDialog(
            "Καθαρισμός", "Να καθαριστεί το ιστορικό αυτής της λίστας;", "Καθαρισμός",
            { confirmClear = false }
        ) { if (kind == "recent") LibraryStore.clearHistory() else LibraryStore.clearCounts() }
    }
}

@Composable
fun SongPickerDialog(
    all: List<Song>,
    alreadyIn: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableIntStateOf(2) } // newest first by default
    var chosen by remember { mutableStateOf(setOf<Long>()) }
    val list = remember(all, query, sort) {
        val filtered = if (query.isBlank()) all
        else all.filter { it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true) }
        sortSongs(filtered, sort)
    }
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .systemBarsPadding()
        ) {
            // ---- top bar ----
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Άκυρο", tint = Color.White)
                }
                Text(
                    "Προσθήκη τραγουδιών",
                    style = MaterialTheme.typography.titleLarge, color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Προσθήκη (${chosen.size})",
                    color = if (chosen.isEmpty()) AppColors.TextFaint else Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (chosen.isEmpty()) AppColors.Surface else AppColors.Accent)
                        .clickable(enabled = chosen.isNotEmpty()) { onConfirm(chosen.toList()) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            SearchField(query, { query = it }, "Αναζήτηση", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))

            // ---- sort chips ----
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(2, 0, 1, 3).forEach { i ->
                    val sel = i == sort
                    Text(
                        sortLabels[i],
                        color = Color.White, fontSize = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (sel) AppColors.Accent else AppColors.SurfaceStrong)
                            .clickable { sort = i }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // ---- select all visible ----
            val selectable = list.filter { !alreadyIn.contains(it.id) }
            val allSel = selectable.isNotEmpty() && selectable.all { chosen.contains(it.id) }
            val toggleAll: () -> Unit = {
                chosen = if (allSel) chosen - selectable.map { it.id }.toSet()
                else chosen + selectable.map { it.id }
            }
            Row(
                Modifier.fillMaxWidth().clickable(onClick = toggleAll).padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleCheck(allSel, toggleAll)
                Spacer(Modifier.width(16.dp))
                Text("Επιλογή όλων (${list.size})", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }

            LazyColumn(Modifier.weight(1f)) {
                items(list, key = { it.id }) { s ->
                    val inList = alreadyIn.contains(s.id)
                    val isSel = chosen.contains(s.id) || inList
                    val toggle: () -> Unit = {
                        if (!inList) chosen = if (chosen.contains(s.id)) chosen - s.id else chosen + s.id
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (isSel && !inList) AppColors.Surface else Color.Transparent)
                            .clickable(onClick = toggle)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircleCheck(isSel, toggle)
                        Spacer(Modifier.width(12.dp))
                        Artwork(s.uri, size = 48.dp, corner = 10.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.title, color = if (inList) AppColors.TextFaint else Color.White,
                                fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                lineHeight = 20.sp
                            )
                            Text(
                                if (inList) "Υπάρχει ήδη στη playlist"
                                else "${s.artist} · ${formatDuration(s.durationMs)} · ${dateFmt.format(Date(s.dateAddedSec * 1000))}",
                                color = AppColors.TextDim, fontSize = 13.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
