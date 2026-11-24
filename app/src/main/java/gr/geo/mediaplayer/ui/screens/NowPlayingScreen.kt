package gr.geo.mediaplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.navigation.NavHostController
import gr.geo.mediaplayer.data.LibraryStore
import gr.geo.mediaplayer.data.MediaRepository
import gr.geo.mediaplayer.data.formatDuration
import gr.geo.mediaplayer.playback.PlayerConnection
import gr.geo.mediaplayer.playback.SleepTimer
import gr.geo.mediaplayer.ui.AddToPlaylistDialog
import gr.geo.mediaplayer.ui.AppColors
import gr.geo.mediaplayer.ui.Artwork
import gr.geo.mediaplayer.ui.Routes
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(nav: NavHostController) {
    val s by PlayerConnection.state.collectAsState()
    val favs by LibraryStore.favorites.collectAsState()
    val sleepEnd by SleepTimer.endsAt.collectAsState()

    val songId = s.mediaId?.toLongOrNull()
    val song = songId?.let { MediaRepository.songById(it) }
    val isFav = songId != null && favs.contains(songId)

    var position by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var showSleep by remember { mutableStateOf(false) }
    var addToPlaylist by remember { mutableStateOf(false) }
    var sleepLeft by remember { mutableLongStateOf(0L) }

    LaunchedEffect(s.isPlaying, s.mediaId) {
        while (true) {
            position = PlayerConnection.currentPosition
            sleepLeft = SleepTimer.remainingMs()
            delay(if (s.isPlaying) 250 else 1000)
        }
    }

    LaunchedEffect(s.hasMedia) { if (!s.hasMedia) nav.popBackStack() }

    var swipeX by remember { mutableFloatStateOf(0f) }
    var swipeY by remember { mutableFloatStateOf(0f) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                // swipe down anywhere to close
                detectVerticalDragGestures(
                    onDragEnd = { if (swipeY > 250) nav.popBackStack(); swipeY = 0f }
                ) { _, dy -> swipeY += dy }
            }
            .padding(horizontal = 24.dp)
    ) {
        // ---------- top bar ----------
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.Filled.KeyboardArrowDown, "Κλείσιμο", tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ΤΩΡΑ ΠΑΙΖΕΙ", color = AppColors.TextDim, fontSize = 12.sp, letterSpacing = 2.sp)
                if (sleepEnd != 0L) {
                    Text("Ύπνος σε ${formatDuration(sleepLeft)}", color = AppColors.Accent, fontSize = 12.sp)
                }
            }
            IconButton(onClick = { nav.navigate(Routes.QUEUE) }) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, "Ουρά", tint = Color.White)
            }
        }

        Spacer(Modifier.weight(0.6f))

        // ---------- artwork (swipe left/right to change song) ----------
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(horizontal = 8.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (swipeX < -150) PlayerConnection.next()
                            else if (swipeX > 150) PlayerConnection.previous()
                            swipeX = 0f
                        }
                    ) { _, dx -> swipeX += dx }
                },
            contentAlignment = Alignment.Center
        ) {
            Artwork(
                song?.uri, size = 340.dp, corner = 32.dp,
                loadSizePx = 900, modifier = Modifier.fillMaxSize(),
                fallbackIcon = Icons.Filled.MusicNote
            )
        }

        Spacer(Modifier.weight(0.6f))

        // ---------- title + heart ----------
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    s.title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 34.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(s.artist, color = AppColors.TextDim, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (songId != null) {
                IconButton(onClick = { LibraryStore.toggleFavorite(songId) }, modifier = Modifier.size(56.dp)) {
                    Icon(
                        if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        if (isFav) "Αφαίρεση από αγαπημένα" else "Στα αγαπημένα",
                        tint = if (isFav) Color(0xFFFF5A7A) else Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---------- seek bar with -10 / +10 ----------
        val duration = s.durationMs.coerceAtLeast(1L)
        val shownPos = if (dragging) (dragValue * duration).toLong() else position
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { PlayerConnection.seekBack() }) {
                Icon(Icons.Filled.Replay10, "Πίσω 10 δευτ.", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Slider(
                value = if (dragging) dragValue else (position.toFloat() / duration).coerceIn(0f, 1f),
                onValueChange = { dragging = true; dragValue = it },
                onValueChangeFinished = {
                    PlayerConnection.seekTo((dragValue * duration).toLong())
                    position = (dragValue * duration).toLong()
                    dragging = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = AppColors.Accent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { PlayerConnection.seekForward() }) {
                Icon(Icons.Filled.Forward10, "Μπροστά 10 δευτ.", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 52.dp)) {
            Text(formatDuration(shownPos), color = AppColors.TextDim, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(formatDuration(s.durationMs), color = AppColors.TextDim, fontSize = 13.sp)
        }

        Spacer(Modifier.height(12.dp))

        // ---------- main controls ----------
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { PlayerConnection.toggleShuffle() }) {
                Icon(
                    Icons.Filled.Shuffle, "Τυχαία σειρά",
                    tint = if (s.shuffle) AppColors.Accent else AppColors.TextFaint,
                    modifier = Modifier.size(30.dp)
                )
            }
            IconButton(onClick = { PlayerConnection.previous() }, modifier = Modifier.size(60.dp)) {
                Icon(Icons.Filled.SkipPrevious, "Προηγούμενο", tint = Color.White, modifier = Modifier.size(46.dp))
            }
            Box(
                Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(AppColors.SurfaceStrong)
                    .clickable { PlayerConnection.togglePlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (s.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (s.isPlaying) "Παύση" else "Αναπαραγωγή",
                    tint = Color.White, modifier = Modifier.size(52.dp)
                )
            }
            IconButton(onClick = { PlayerConnection.next() }, modifier = Modifier.size(60.dp)) {
                Icon(Icons.Filled.SkipNext, "Επόμενο", tint = Color.White, modifier = Modifier.size(46.dp))
            }
            IconButton(onClick = { PlayerConnection.cycleRepeat() }) {
                Icon(
                    if (s.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    when (s.repeatMode) {
                        Player.REPEAT_MODE_ONE -> "Επανάληψη ενός"
                        Player.REPEAT_MODE_ALL -> "Επανάληψη όλων"
                        else -> "Χωρίς επανάληψη"
                    },
                    tint = if (s.repeatMode == Player.REPEAT_MODE_OFF) AppColors.TextFaint else AppColors.Accent,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(Modifier.weight(0.5f))

        // ---------- bottom tools ----------
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ToolButton(Icons.AutoMirrored.Filled.PlaylistAdd, "Σε playlist", enabled = songId != null) { addToPlaylist = true }
            ToolButton(Icons.Filled.Equalizer, "Ισοσταθμιστής") { nav.navigate(Routes.EQ) }
            ToolButton(Icons.Filled.Alarm, "Χρονόμετρο", highlighted = sleepEnd != 0L) { showSleep = true }
            ToolButton(Icons.AutoMirrored.Filled.QueueMusic, "Ουρά") { nav.navigate(Routes.QUEUE) }
        }
    }

    if (showSleep) SleepTimerDialog(active = sleepEnd != 0L) { showSleep = false }
    if (addToPlaylist && songId != null) AddToPlaylistDialog(listOf(songId)) { addToPlaylist = false }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val tint = when {
            !enabled -> AppColors.TextFaint
            highlighted -> AppColors.Accent
            else -> AppColors.TextDim
        }
        Icon(icon, label, tint = tint, modifier = Modifier.size(28.dp))
        Text(label, color = tint, fontSize = 11.sp)
    }
}

@Composable
fun SleepTimerDialog(active: Boolean, onDismiss: () -> Unit) {
    val options = listOf(5, 10, 15, 30, 45, 60, 90, 120)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Χρονόμετρο ύπνου") },
        text = {
            Column {
                Text("Η μουσική θα σταματήσει μετά από:", color = AppColors.TextDim)
                Spacer(Modifier.height(8.dp))
                options.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { m ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppColors.SurfaceStrong)
                                    .clickable { SleepTimer.start(m); onDismiss() }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("$m'", color = Color.White, fontSize = 16.sp) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (active) TextButton(onClick = { SleepTimer.cancel(); onDismiss() }) { Text("Ακύρωση χρονομέτρου") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Κλείσιμο") } },
    )
}

