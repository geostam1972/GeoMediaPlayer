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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import gr.geo.mediaplayer.data.MediaRepository
import gr.geo.mediaplayer.gif.GifViewerActivity
import gr.geo.mediaplayer.ui.AppColors
import gr.geo.mediaplayer.ui.EmptyState

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
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(gifs, key = { _, g -> g.id }) { index, g ->
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(g.uri).size(360).crossfade(true).build(),
                        contentDescription = g.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
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
                    )
                }
            }
        }
    }
}
