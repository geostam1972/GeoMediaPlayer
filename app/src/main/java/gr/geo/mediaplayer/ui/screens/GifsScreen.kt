package gr.geo.mediaplayer.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GifBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import kotlin.math.abs
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import gr.geo.mediaplayer.data.MediaRepository
import gr.geo.mediaplayer.gif.GifViewerActivity
import gr.geo.mediaplayer.ui.AppColors
import gr.geo.mediaplayer.ui.EmptyState

private const val COLUMNS = 3

@Composable
fun GifsScreen() {
    val context = LocalContext.current
    val gifs by MediaRepository.gifs.collectAsState()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            "${gifs.size} κινούμενες εικόνες",
            style = MaterialTheme.typography.headlineMedium, color = Color.White,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
        )
        if (gifs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Filled.GifBox, "Δεν βρέθηκαν GIF", "Τα αρχεία .gif της συσκευής θα εμφανιστούν εδώ.")
            }
        } else {
            val gridState = rememberLazyGridState()
            // The row nearest the middle of the screen is the only one that animates (max 3 GIFs).
            val activeRow by remember {
                derivedStateOf {
                    val info = gridState.layoutInfo
                    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
                    info.visibleItemsInfo
                        .minByOrNull { abs(it.offset.y + it.size.height / 2 - center) }
                        ?.let { it.index / COLUMNS } ?: -1
                }
            }
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(COLUMNS),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(gifs, key = { _, g -> g.id }) { index, g ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.Surface)
                            .clickable {
                                GifViewerActivity.items = gifs.map { it.uri to it.name }
                                context.startActivity(
                                    Intent(context, GifViewerActivity::class.java)
                                        .putExtra(GifViewerActivity.EXTRA_INDEX, index)
                                )
                            }
                    ) {
                        val animate = index / COLUMNS == activeRow
                        // Only the middle row animates; everything else shows its first frame,
                        // so the phone never runs out of memory.
                        AsyncImage(
                            model = if (animate) {
                                ImageRequest.Builder(context)
                                    .data(g.uri)
                                    .size(300)
                                    .memoryCacheKey("gifanim_${g.id}")
                                    .placeholderMemoryCacheKey("gifthumb_${g.id}")
                                    .crossfade(false)
                                    .build()
                            } else {
                                ImageRequest.Builder(context)
                                    .data(g.uri)
                                    .size(300)
                                    .decoderFactory(BitmapFactoryDecoder.Factory())
                                    .memoryCacheKey("gifthumb_${g.id}")
                                    .placeholderMemoryCacheKey("gifthumb_${g.id}")
                                    .crossfade(false)
                                    .build()
                            },
                            contentDescription = g.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (animate) {
                            Box(
                                Modifier.fillMaxSize()
                                    .border(2.dp, AppColors.Accent, RoundedCornerShape(12.dp))
                            )
                        }
                        Text(
                            "GIF",
                            color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xAA000000))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
