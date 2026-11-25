package gr.geo.mediaplayer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import gr.geo.mediaplayer.data.LibraryStore
import gr.geo.mediaplayer.data.MediaRepository
import gr.geo.mediaplayer.data.Song
import gr.geo.mediaplayer.data.formatDuration
import gr.geo.mediaplayer.playback.PlayerConnection
import gr.geo.mediaplayer.ui.AddToPlaylistDialog
import gr.geo.mediaplayer.ui.AppColors
import gr.geo.mediaplayer.ui.CircleCheck
import gr.geo.mediaplayer.ui.EmptyState
import gr.geo.mediaplayer.ui.Routes
import gr.geo.mediaplayer.ui.SearchField
import gr.geo.mediaplayer.ui.SongMenu
import gr.geo.mediaplayer.ui.SongRow

val sortLabels = listOf("Τίτλος (Α-Ω)", "Καλλιτέχνης", "Πιο πρόσφατα", "Διάρκεια")

fun sortSongs(songs: List<Song>, sort: Int): List<Song> = when (sort) {
    1 -> songs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist })
    2 -> songs.sortedByDescending { it.dateAddedSec }
    3 -> songs.sortedByDescending { it.durationMs }
    else -> songs
}

@Composable
fun SongsScreen(nav: NavHostController) {
    val allSongs by MediaRepository.songs.collectAsState()
    val loading by MediaRepository.loading.collectAsState()
    val player by PlayerConnection.state.collectAsState()

    var query by remember { mutableStateOf("") }
    var sort by remember { mutableIntStateOf(LibraryStore.getInt("sort", 0)) }
    var sortMenu by remember { mutableStateOf(false) }

    var view by rememberSaveable { mutableIntStateOf(0) } // 0 songs, 1 albums, 2 artists
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var addToPlaylistIds by remember { mutableStateOf<List<Long>?>(null) }
    var menuFor by remember { mutableStateOf<Long?>(null) }

    val songs = remember(allSongs, query, sort) {
        val filtered = if (query.isBlank()) allSongs else allSongs.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
        }
        sortSongs(filtered, sort)
    }

    BackHandler(enabled = selecting) { selecting = false; selected = emptySet() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        if (selecting) {
            // ----- multi-select header (like screenshot 2) -----
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selecting = false; selected = emptySet() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω", tint = Color.White)
                }
                Text(
                    "${selected.size} επιλεγμένα",
                    style = MaterialTheme.typography.headlineMedium, color = Color.White
                )
            }
            SearchField(query, { query = it }, "Αναζήτηση τραγουδιών", Modifier.padding(horizontal = 16.dp))
            val allSelected = songs.isNotEmpty() && songs.all { selected.contains(it.id) }
            Row(
                Modifier.fillMaxWidth()
                    .clickable {
                        selected = if (allSelected) selected - songs.map { it.id }.toSet()
                        else selected + songs.map { it.id }
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleCheck(allSelected) {
                    selected = if (allSelected) selected - songs.map { it.id }.toSet()
                    else selected + songs.map { it.id }
                }
                Spacer(Modifier.width(18.dp))
                Text("Επιλογή όλων", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        } else {
            // ----- normal header (like screenshot 4) -----
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchField(query, { query = it }, "Αναζήτηση", Modifier.weight(1f))
                IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) {
                    Icon(Icons.Filled.Settings, "Ρυθμίσεις", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            LibraryViewChips(view) { view = it }
            if (view == 0) Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${songs.size} τραγούδια",
                    style = MaterialTheme.typography.headlineMedium, color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { sortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, "Ταξινόμηση", tint = Color.White)
                    }
                    DropdownMenu(sortMenu, { sortMenu = false }) {
                        sortLabels.forEachIndexed { i, label ->
                            DropdownMenuItem(
                                text = { Text(if (i == sort) "✓  $label" else "     $label") },
                                onClick = { sort = i; LibraryStore.putInt("sort", i); sortMenu = false }
                            )
                        }
                    }
                }
                IconButton(onClick = { selecting = true }) {
                    Icon(Icons.Filled.Checklist, "Επιλογή", tint = Color.White)
                }
            }
            if (view == 0 && songs.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PillButton("Αναπαραγωγή", Icons.Filled.PlayArrow, filled = true, modifier = Modifier.weight(1f)) {
                        PlayerConnection.playSongs(songs, 0)
                    }
                    PillButton("Τυχαία", Icons.Filled.Shuffle, filled = false, modifier = Modifier.weight(1f)) {
                        PlayerConnection.playSongs(songs, shuffle = true)
                    }
                }
            }
        }

        Box(Modifier.weight(1f)) {
            when {
                !selecting && view != 0 -> LibraryGrid(view, query, nav)
                loading && allSongs.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                songs.isEmpty() -> EmptyState(
                    Icons.Filled.MusicOff,
                    if (query.isBlank()) "Δεν βρέθηκαν τραγούδια" else "Κανένα αποτέλεσμα",
                    if (query.isBlank()) "Βάλε αρχεία MP3 στον φάκελο Music ή Download του κινητού."
                    else "Δοκίμασε άλλη λέξη.",
                    Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                        val isSel = selected.contains(song.id)
                        val toggle: () -> Unit = { selected = if (isSel) selected - song.id else selected + song.id }
                        SongRow(
                            title = song.title,
                            subtitle = "${song.artist}  ·  ${formatDuration(song.durationMs)}",
                            artUri = song.uri,
                            isCurrent = player.mediaId == song.id.toString(),
                            leading = if (selecting) ({ CircleCheck(isSel) { toggle() } }) else null,
                            trailing = if (!selecting) ({
                                Box {
                                    IconButton(onClick = { menuFor = song.id }) {
                                        Icon(Icons.Filled.MoreVert, "Επιλογές", tint = AppColors.TextDim)
                                    }
                                    SongMenu(
                                        song = song,
                                        expanded = menuFor == song.id,
                                        onDismiss = { menuFor = null },
                                        onAddToPlaylist = { addToPlaylistIds = listOf(song.id) },
                                    )
                                }
                            }) else null,
                            onLongClick = {
                                if (!selecting) { selecting = true; selected = setOf(song.id) }
                            },
                            onClick = {
                                if (selecting) toggle() else PlayerConnection.playSongs(songs, index)
                            }
                        )
                    }
                }
            }
        }

        if (selecting) {
            // bottom action bar for selection
            val chosen = songs.filter { selected.contains(it.id) }
            Row(
                Modifier.fillMaxWidth().background(Color(0xFF0E1335)).padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SelectionAction(Icons.Filled.PlayArrow, "Παίξε", chosen.isNotEmpty()) {
                    PlayerConnection.playSongs(chosen); selecting = false; selected = emptySet()
                }
                SelectionAction(Icons.AutoMirrored.Filled.QueueMusic, "Στην ουρά", chosen.isNotEmpty()) {
                    PlayerConnection.addToQueue(chosen); selecting = false; selected = emptySet()
                }
                SelectionAction(Icons.AutoMirrored.Filled.PlaylistAdd, "Σε playlist", chosen.isNotEmpty()) {
                    addToPlaylistIds = chosen.map { it.id }
                }
                SelectionAction(Icons.Filled.FavoriteBorder, "Αγαπημένα", chosen.isNotEmpty()) {
                    val favs = LibraryStore.favorites.value
                    chosen.filter { !favs.contains(it.id) }.forEach { LibraryStore.toggleFavorite(it.id) }
                    selecting = false; selected = emptySet()
                }
            }
        }
    }

    addToPlaylistIds?.let { ids ->
        AddToPlaylistDialog(ids) {
            addToPlaylistIds = null
            selecting = false; selected = emptySet()
        }
    }
}

@Composable
fun PillButton(
    label: String,
    icon: ImageVector,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(if (filled) AppColors.Accent else AppColors.SurfaceStrong)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 15.sp)
    }
}

@Composable
private fun SelectionAction(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val tint = if (enabled) Color.White else AppColors.TextFaint
        Icon(icon, label, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = tint, fontSize = 12.sp)
    }
}
