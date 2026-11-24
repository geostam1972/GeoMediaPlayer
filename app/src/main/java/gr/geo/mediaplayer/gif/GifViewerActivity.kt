package gr.geo.mediaplayer.gif

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import gr.geo.mediaplayer.ui.AppTheme

/** Full-screen animated GIF viewer: swipe for next/previous, pinch to zoom, tap to show/hide the bar. */
class GifViewerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_INDEX = "index"
        var items: List<Pair<Uri, String>> = emptyList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent.data
        val list: List<Pair<Uri, String>> = if (data != null) {
            val name = runCatching {
                contentResolver.query(data, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                }
            }.getOrNull() ?: "GIF"
            listOf(data to name)
        } else items
        if (list.isEmpty()) { finish(); return }
        val start = if (data != null) 0 else intent.getIntExtra(EXTRA_INDEX, 0).coerceIn(0, list.size - 1)

        setContent {
            AppTheme {
                GifViewer(list, start, onBack = { finish() }, onShare = { uri ->
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "image/gif"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(send, "Κοινοποίηση GIF"))
                })
            }
        }
    }
}

@Composable
private fun GifViewer(
    list: List<Pair<Uri, String>>,
    start: Int,
    onBack: () -> Unit,
    onShare: (Uri) -> Unit,
) {
    val pager = rememberPagerState(initialPage = start) { list.size }
    var barVisible by remember { mutableStateOf(true) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            ZoomableGif(list[page].first, list[page].second) { barVisible = !barVisible }
        }

        AnimatedVisibility(barVisible, enter = fadeIn(), exit = fadeOut()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω", tint = Color.White)
                }
                Text(
                    list[pager.currentPage].second,
                    color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${pager.currentPage + 1}/${list.size}",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = { onShare(list[pager.currentPage].first) }) {
                    Icon(Icons.Filled.Share, "Κοινοποίηση", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ZoomableGif(uri: Uri, name: String, onTap: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val zoomed = scale > 1.01f

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1.01f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
                    }
                )
            }
            .then(
                // Only grab pinch/pan while zoomed or pinching, so swiping between GIFs still works.
                Modifier.pointerInput(zoomed) {
                    detectTransformGestures(panZoomLock = false) { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 6f)
                        scale = newScale
                        offset = if (newScale <= 1.01f) Offset.Zero else offset + pan
                    }
                }.takeIfZoomable(zoomed)
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = uri,
            contentDescription = name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale; scaleY = scale
                    translationX = offset.x; translationY = offset.y
                }
        )
    }
}

/** When not zoomed we still allow a two-finger pinch to start zooming, handled by double-tap instead. */
private fun Modifier.takeIfZoomable(zoomed: Boolean): Modifier = if (zoomed) this else Modifier
