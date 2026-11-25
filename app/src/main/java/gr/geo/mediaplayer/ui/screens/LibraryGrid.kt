package gr.geo.mediaplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import gr.geo.mediaplayer.data.MediaRepository
import gr.geo.mediaplayer.data.Song
import gr.geo.mediaplayer.ui.AppColors
import gr.geo.mediaplayer.ui.Artwork
import gr.geo.mediaplayer.ui.EmptyState
import gr.geo.mediaplayer.ui.Routes

/** One album or artist with its songs. */
data class SongGroup(
    val id: Long,
    val name: String,
    val subtitle: String,
    val songs: List<Song>,
)

/** Stable id for an artist name (used in the navigation route). */
fun artistId(name: String): Long = name.lowercase().hashCode().toLong()

fun albumGroups(songs: List<Song>): List<SongGroup> =
    songs.groupBy { it.albumId }.map { (id, list) ->
        val artist = list.groupingBy { it.artist }.eachCount().maxByOrNull { it.value }?.key ?: ""
        SongGroup(id, list.first().album.ifBlank { "Άγνωστο άλμπουμ" }, artist, list.sortedBy { it.title.lowercase() })
    }.sortedBy { it.name.lowercase() }

fun artistGroups(songs: List<Song>): List<SongGroup> =
    songs.groupBy { artistId(it.artist) }.map { (id, list) ->
        val albums = list.map { it.albumId }.distinct().size
        SongGroup(
            id, list.first().artist,
            if (albums > 1) "$albums άλμπουμ" else "",
            list.sortedBy { it.title.lowercase() }
        )
    }.sortedBy { it.name.lowercase() }

val libraryViews = listOf("Τραγούδια", "Άλμπουμ", "Καλλιτέχνες")

@Composable
fun LibraryViewChips(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        libraryViews.forEachIndexed { i, label ->
            val sel = i == selected
            Text(
                label,
                color = if (sel) Color.White else AppColors.TextDim,
                fontSize = 15.sp,
                fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (sel) AppColors.Accent else AppColors.Surface)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            )
        }
    }
}

/** Grid of album covers (view = 1) or artists (view = 2). */
@Composable
fun LibraryGrid(view: Int, query: String, nav: NavHostController) {
    val songs by MediaRepository.songs.collectAsState()
    val groups = remember(songs, view, query) {
        val all = if (view == 1) albumGroups(songs) else artistGroups(songs)
        if (query.isBlank()) all
        else all.filter { it.name.contains(query, true) || it.subtitle.contains(query, true) }
    }
    val icon: ImageVector = if (view == 1) Icons.Filled.Album else Icons.Filled.Person

    Column(Modifier.fillMaxSize()) {
        Text(
            "${groups.size} ${if (view == 1) "άλμπουμ" else "καλλιτέχνες"}",
            color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
        )
        if (groups.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(icon, "Δεν βρέθηκαν", "Δοκίμασε άλλη αναζήτηση.")
            }
        } else LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(groups, key = { it.id }) { g ->
                Column(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { nav.navigate(Routes.playlist(if (view == 1) "album" else "artist", g.id)) }
                ) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                        // Use the first song that has a cover.
                        Artwork(
                            g.songs.firstOrNull()?.uri,
                            size = 400.dp,
                            corner = if (view == 1) 16.dp else 200.dp,
                            loadSizePx = 400,
                            fallbackIcon = icon,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        g.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Text(
                        listOf(g.subtitle, "${g.songs.size} τραγούδια").filter { it.isNotBlank() }.joinToString(" · "),
                        color = AppColors.TextDim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
