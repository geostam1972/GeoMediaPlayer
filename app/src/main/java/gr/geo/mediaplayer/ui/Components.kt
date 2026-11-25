package gr.geo.mediaplayer.ui

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import gr.geo.mediaplayer.playback.PlayerConnection
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Small in-memory cache for album art and video thumbnails. */
object ThumbCache {
    private val cache = object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val missing = HashSet<String>()

    fun get(key: String): Bitmap? = cache.get(key)
    fun put(key: String, bmp: Bitmap) = cache.put(key, bmp)
    fun isMissing(key: String) = synchronized(missing) { missing.contains(key) }
    fun markMissing(key: String) = synchronized(missing) { missing.add(key) }
}

/**
 * Shows the embedded cover of a song (or the frame of a video). Falls back to
 * a music-note tile like the screenshots.
 */
@Composable
fun Artwork(
    uri: Uri?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    corner: Dp = 14.dp,
    playingIndicator: Boolean = false,
    fallbackIcon: ImageVector = Icons.Filled.MusicNote,
    loadSizePx: Int = 256,
) {
    val context = LocalContext.current
    val key = "${uri}_$loadSizePx"
    var bmp by remember(key) { mutableStateOf(uri?.let { ThumbCache.get(key) }) }
    LaunchedEffect(key) {
        if (bmp == null) bmp = loadThumb(context, uri, loadSizePx)
    }
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(if (playingIndicator) Color(0xFF0B0E22) else AppColors.Surface),
        contentAlignment = Alignment.Center
    ) {
        val b = bmp
        if (b != null) {
            Image(
                b.asImageBitmap(), null,
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (!playingIndicator) {
            Icon(fallbackIcon, null, tint = AppColors.Accent, modifier = Modifier.size(size * 0.45f))
        }
        if (playingIndicator) {
            val playing = PlayerConnection.state.collectAsState().value.isPlaying
            Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                PlayingBars(animate = playing, height = size * 0.42f)
            }
        }
    }
}

/** Three little bars that dance while the song plays (static when paused). */
@Composable
fun PlayingBars(animate: Boolean, height: Dp, color: Color = Color.White) {
    val t = rememberInfiniteTransition(label = "bars")
    val specs = listOf(420, 560, 360)
    val rest = listOf(0.45f, 0.85f, 0.6f)
    Row(
        Modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(height * 0.14f),
        verticalAlignment = Alignment.Bottom
    ) {
        specs.forEachIndexed { i, ms ->
            val f by t.animateFloat(
                initialValue = 0.25f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(ms, easing = LinearEasing), RepeatMode.Reverse),
                label = "bar$i"
            )
            Box(
                Modifier
                    .width(height * 0.2f)
                    .fillMaxHeight(if (animate) f else rest[i])
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    title: String,
    subtitle: String,
    artUri: Uri?,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(8.dp))
        }
        Artwork(artUri, size = 56.dp, playingIndicator = isCurrent)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) AppColors.Accent else Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextDim,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(AppColors.SurfaceStrong)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, null, tint = AppColors.TextDim)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = AppColors.TextFaint, fontSize = 16.sp)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                cursorBrush = SolidColor(AppColors.Accent),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, "Καθαρισμός", tint = AppColors.TextDim)
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(AppColors.Surface),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = AppColors.Accent, modifier = Modifier.size(44.dp)) }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextDim)
    }
}

@Composable
fun CircleCheck(checked: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (checked) AppColors.Accent else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
        } else {
            Box(
                Modifier.size(26.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            )
        }
    }
}
