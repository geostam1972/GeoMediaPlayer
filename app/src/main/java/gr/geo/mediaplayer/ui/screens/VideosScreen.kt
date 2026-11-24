package gr.geo.mediaplayer.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.geo.mediaplayer.data.MediaRepository
import gr.geo.mediaplayer.data.formatDuration
import gr.geo.mediaplayer.ui.AppColors
import gr.geo.mediaplayer.ui.Artwork
import gr.geo.mediaplayer.ui.EmptyState
import gr.geo.mediaplayer.ui.SearchField
import gr.geo.mediaplayer.video.VideoPlayerActivity

@Composable
fun VideosScreen() {
    val context = LocalContext.current
    val all by MediaRepository.videos.collectAsState()
    var query by remember { mutableStateOf("") }
    val videos = remember(all, query) {
        if (query.isBlank()) all else all.filter { it.title.contains(query, true) || it.folder.contains(query, true) }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        SearchField(query, { query = it }, "Αναζήτηση βίντεο", Modifier.padding(16.dp))
        Text(
            "${videos.size} βίντεο",
            style = MaterialTheme.typography.headlineMedium, color = Color.White,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
        )
        if (videos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Filled.VideocamOff, "Δεν βρέθηκαν βίντεο", "Τα βίντεο της συσκευής θα εμφανιστούν εδώ.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                itemsIndexed(videos, key = { _, v -> v.id }) { index, v ->
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                VideoPlayerActivity.playlist = videos.map { it.uri to it.title }
                                context.startActivity(
                                    Intent(context, VideoPlayerActivity::class.java)
                                        .putExtra(VideoPlayerActivity.EXTRA_INDEX, index)
                                )
                            }
                    ) {
                        Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f)) {
                            Artwork(
                                v.uri, size = 400.dp, corner = 16.dp, loadSizePx = 480,
                                modifier = Modifier.fillMaxSize(),
                                fallbackIcon = Icons.Filled.PlayArrow
                            )
                            Box(
                                Modifier.align(Alignment.Center).size(42.dp).clip(CircleShape)
                                    .background(Color(0x88000000)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.PlayArrow, null, tint = Color.White) }
                            Text(
                                formatDuration(v.durationMs),
                                color = Color.White, fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                                    .clip(RoundedCornerShape(6.dp)).background(Color(0xAA000000))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            v.title, color = Color.White, fontSize = 14.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                            Text(
                                "${v.folder} · ${"%.1f".format(v.sizeBytes / 1_048_576.0)} MB",
                                color = AppColors.TextFaint, fontSize = 12.sp, maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
