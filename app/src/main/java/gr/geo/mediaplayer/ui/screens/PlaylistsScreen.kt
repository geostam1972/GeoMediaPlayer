package gr.geo.mediaplayer.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import gr.geo.mediaplayer.data.LibraryStore
import gr.geo.mediaplayer.data.MediaRepository
import gr.geo.mediaplayer.data.Playlist
import gr.geo.mediaplayer.data.Song
import gr.geo.mediaplayer.playback.PlayerConnection
import gr.geo.mediaplayer.ui.AppColors
import gr.geo.mediaplayer.ui.ConfirmDialog
import gr.geo.mediaplayer.ui.NameDialog
import gr.geo.mediaplayer.ui.Routes
import java.time.LocalDate
import kotlin.random.Random

/** Builds the songs of the automatic playlists. */
object SmartLists {
    fun favorites(all: List<Song>, favIds: List<Long>): List<Song> {
        val byId = all.associateBy { it.id }
        return favIds.mapNotNull { byId[it] }
    }

    fun recentlyAdded(all: List<Song>): List<Song> {
        val cutoff = System.currentTimeMillis() / 1000 - 30L * 24 * 3600
        return all.filter { it.dateAddedSec >= cutoff }.sortedByDescending { it.dateAddedSec }
    }

    fun recentlyPlayed(all: List<Song>, recent: List<Long>): List<Song> {
        val byId = all.associateBy { it.id }
        return recent.mapNotNull { byId[it] }
    }

    fun mostPlayed(all: List<Song>, counts: Map<Long, Int>): List<Song> {
        val byId = all.associateBy { it.id }
        return counts.entries.sortedByDescending { it.value }.take(100).mapNotNull { byId[it.key] }
    }

    /** 10 random songs that stay the same for the whole day. */
    fun daily(all: List<Song>): List<Song> {
        if (all.isEmpty()) return emptyList()
        val seed = LocalDate.now().toEpochDay()
        return all.shuffled(Random(seed)).take(10)
    }

    fun user(all: List<Song>, p: Playlist): List<Song> {
        val byId = all.associateBy { it.id }
        return p.songIds.mapNotNull { byId[it] }
    }
}

@Composable
fun PlaylistsScreen(nav: NavHostController) {
    val songs by MediaRepository.songs.collectAsState()
    val favIds by LibraryStore.favorites.collectAsState()
    val recent by LibraryStore.recent.collectAsState()
    val counts by LibraryStore.playCounts.collectAsState()
    val playlists by LibraryStore.playlists.collectAsState()

    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Playlist?>(null) }
    var deleting by remember { mutableStateOf<Playlist?>(null) }
    var menuFor by remember { mutableStateOf<Long?>(null) }

    val favCount = remember(songs, favIds) { SmartLists.favorites(songs, favIds).size }
    val addedCount = remember(songs) { SmartLists.recentlyAdded(songs).size }
    val recentCount = remember(songs, recent) { SmartLists.recentlyPlayed(songs, recent).size }
    val mostCount = remember(songs, counts) { SmartLists.mostPlayed(songs, counts).size }
    val dailyCount = remember(songs) { SmartLists.daily(songs).size }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding()) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${playlists.size + 5} playlists",
                    style = MaterialTheme.typography.headlineMedium, color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { creating = true }) {
                    Icon(Icons.Filled.Add, "Νέα playlist", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
        item {
            // Card grid like screenshot 4
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmartCard(
                    "Ημερήσια\nplaylist", "$dailyCount τραγούδια", Icons.Filled.WbSunny,
                    Brush.linearGradient(listOf(Color(0xFF13A6B8), Color(0xFF2BC4D6), Color(0xFF167C9E))),
                    height = 244.dp, big = true, modifier = Modifier.weight(1f),
                    onPlay = { PlayerConnection.playSongs(SmartLists.daily(songs)) },
                ) { nav.navigate(Routes.playlist("daily")) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmartCard(
                        "Αγαπημένα", "$favCount τραγούδια", Icons.Filled.Favorite,
                        Brush.linearGradient(listOf(Color(0xFF2747C9), Color(0xFF1E6FE0))),
                        height = 116.dp,
                    ) { nav.navigate(Routes.playlist("fav")) }
                    SmartCard(
                        "Πρόσφατα\nπροστέθηκαν", "$addedCount τραγούδια", Icons.Filled.LibraryAdd,
                        Brush.linearGradient(listOf(Color(0xFF4B2BB8), Color(0xFF7440E0))),
                        height = 116.dp,
                    ) { nav.navigate(Routes.playlist("added")) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmartCard(
                    "Πρόσφατα\nπαιγμένα", "$recentCount τραγούδια", Icons.Filled.History,
                    Brush.linearGradient(listOf(Color(0xFF0D4F63), Color(0xFF136D80))),
                    height = 116.dp, modifier = Modifier.weight(1f),
                ) { nav.navigate(Routes.playlist("recent")) }
                SmartCard(
                    "Πιο\nπαιγμένα", "$mostCount τραγούδια", Icons.Filled.LocalFireDepartment,
                    Brush.linearGradient(listOf(Color(0xFF6A1B6E), Color(0xFFA12A8C))),
                    height = 116.dp, modifier = Modifier.weight(1f),
                ) { nav.navigate(Routes.playlist("most")) }
            }
        }
        item {
            Text(
                "Οι playlists μου (${playlists.size})",
                style = MaterialTheme.typography.headlineMedium, color = AppColors.TextFaint,
                modifier = Modifier.padding(start = 20.dp, top = 28.dp, bottom = 8.dp)
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().clickable { creating = true }.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(AppColors.Surface),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(34.dp)) }
                Spacer(Modifier.width(18.dp))
                Text("Δημιουργία νέας playlist", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }
        items(playlists, key = { it.id }) { p ->
            Row(
                Modifier.fillMaxWidth()
                    .clickable { nav.navigate(Routes.playlist("user", p.id)) }
                    .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(AppColors.Surface),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.MusicNote, null, tint = AppColors.Accent, modifier = Modifier.size(34.dp)) }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
                    Text("${p.songIds.size} τραγούδια", color = AppColors.TextDim)
                }
                Box {
                    IconButton(onClick = { menuFor = p.id }) {
                        Icon(Icons.Filled.MoreVert, "Επιλογές", tint = AppColors.TextDim)
                    }
                    DropdownMenu(menuFor == p.id, { menuFor = null }) {
                        DropdownMenuItem(
                            text = { Text("Αναπαραγωγή") },
                            leadingIcon = { Icon(Icons.Filled.PlayArrow, null) },
                            onClick = { menuFor = null; PlayerConnection.playSongs(SmartLists.user(songs, p)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Τυχαία αναπαραγωγή") },
                            leadingIcon = { Icon(Icons.Filled.Shuffle, null) },
                            onClick = { menuFor = null; PlayerConnection.playSongs(SmartLists.user(songs, p), shuffle = true) }
                        )
                        DropdownMenuItem(
                            text = { Text("Μετονομασία") },
                            leadingIcon = { Icon(Icons.Filled.Edit, null) },
                            onClick = { menuFor = null; renaming = p }
                        )
                        DropdownMenuItem(
                            text = { Text("Διαγραφή") },
                            leadingIcon = { Icon(Icons.Filled.Delete, null) },
                            onClick = { menuFor = null; deleting = p }
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (creating) {
        NameDialog("Νέα playlist", "", { creating = false }) { name ->
            val id = LibraryStore.createPlaylist(name)
            creating = false
            nav.navigate(Routes.playlist("user", id))
        }
    }
    renaming?.let { p ->
        NameDialog("Μετονομασία", p.name, { renaming = null }) { name ->
            LibraryStore.renamePlaylist(p.id, name); renaming = null
        }
    }
    deleting?.let { p ->
        ConfirmDialog(
            "Διαγραφή playlist", "Να διαγραφεί η «${p.name}»; Τα τραγούδια δεν θα διαγραφούν από το κινητό.",
            "Διαγραφή", { deleting = null }
        ) { LibraryStore.deletePlaylist(p.id) }
    }
}

@Composable
private fun SmartCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    brush: Brush,
    height: Dp,
    modifier: Modifier = Modifier,
    big: Boolean = false,
    onPlay: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(22.dp))
            .background(brush)
            .clickable(onClick = onClick)
    ) {
        // decorative tilted tile with the icon
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 10.dp)
                .size(if (big) 0.dp else 58.dp)
                .rotate(-12f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            if (!big) Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Column(Modifier.padding(16.dp)) {
            if (big) {
                Icon(icon, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(34.dp))
                Spacer(Modifier.weight(1f))
            }
            Text(
                title, color = Color.White,
                fontSize = if (big) 30.sp else 21.sp, fontWeight = FontWeight.Bold,
                lineHeight = if (big) 34.sp else 24.sp
            )
            Text(subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
        }
        if (onPlay != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.PlayArrow, "Αναπαραγωγή", tint = Color(0xFF167C9E), modifier = Modifier.size(32.dp)) }
        }
    }
}
