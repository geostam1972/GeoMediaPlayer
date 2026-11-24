package gr.geo.mediaplayer.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.geo.mediaplayer.data.MediaRepository
import gr.geo.mediaplayer.playback.PlayerConnection
import kotlinx.coroutines.delay

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
/** The purple bar at the bottom: spinning disc, scrolling title, play/pause, queue. */
@Composable
fun MiniPlayer(onOpen: () -> Unit, onQueue: () -> Unit, modifier: Modifier = Modifier) {
    val s by PlayerConnection.state.collectAsState()
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(s.isPlaying, s.mediaId, s.durationMs) {
        while (true) {
            progress = if (s.durationMs > 0) PlayerConnection.currentPosition.toFloat() / s.durationMs else 0f
            if (!s.isPlaying) break
            delay(500)
        }
    }

    val spin = rememberInfiniteTransition(label = "disc")
    val angle by spin.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )

    var dragX by remember { mutableFloatStateOf(0f) }

    Box(modifier.fillMaxWidth().height(72.dp)) {
        Column(
            Modifier
                .padding(start = 36.dp)
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(32.dp))
                .background(AppColors.MiniPlayer)
                .clickable(onClick = onOpen)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragX < -120) PlayerConnection.next()
                            else if (dragX > 120) PlayerConnection.previous()
                            dragX = 0f
                        }
                    ) { _, amount -> dragX += amount }
                }
        ) {
            Row(
                Modifier.weight(1f).padding(start = 46.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        s.title.ifBlank { "—" },
                        color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    if (s.artist.isNotBlank()) {
                        Text(s.artist, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, maxLines = 1)
                    }
                }
                IconButton(onClick = { PlayerConnection.togglePlay() }) {
                    Icon(
                        if (s.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (s.isPlaying) "Παύση" else "Αναπαραγωγή",
                        tint = Color.White, modifier = Modifier.size(34.dp)
                    )
                }
                IconButton(onClick = { PlayerConnection.next() }) {
                    Icon(Icons.Filled.SkipNext, "Επόμενο", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = onQueue) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, "Ουρά", tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
            // progress line
            Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.2f))) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(3.dp).background(Color.White))
            }
        }

        // Spinning vinyl disc with the cover in the middle
        val songUri = s.mediaId?.toLongOrNull()?.let { MediaRepository.songById(it)?.uri }
        Box(
            Modifier
                .size(72.dp)
                .align(Alignment.CenterStart)
                .rotate(if (s.isPlaying) angle else 0f)
                .clip(CircleShape)
                .background(Color(0xFF111111)),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(62.dp).clip(CircleShape).background(Color(0xFF1C1C1C)))
            Artwork(
                songUri, size = 34.dp, corner = 17.dp,
                modifier = Modifier.clip(CircleShape)
            )
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF111111)))
        }
        Spacer(Modifier.width(0.dp))
    }
}
