package gr.geo.mediaplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import gr.geo.mediaplayer.playback.PlayerConnection
import gr.geo.mediaplayer.ui.screens.EqualizerScreen
import gr.geo.mediaplayer.ui.screens.GifsScreen
import gr.geo.mediaplayer.ui.screens.NowPlayingScreen
import gr.geo.mediaplayer.ui.screens.PlaylistDetailScreen
import gr.geo.mediaplayer.ui.screens.PlaylistsScreen
import gr.geo.mediaplayer.ui.screens.QueueScreen
import gr.geo.mediaplayer.ui.screens.SettingsScreen
import gr.geo.mediaplayer.ui.screens.SongsScreen
import gr.geo.mediaplayer.ui.screens.VideosScreen
import kotlinx.coroutines.flow.StateFlow

object Routes {
    const val SONGS = "songs"
    const val PLAYLISTS = "playlists"
    const val VIDEOS = "videos"
    const val GIFS = "gifs"
    const val NOW = "now"
    const val QUEUE = "queue"
    const val EQ = "eq"
    const val SETTINGS = "settings"
    const val PLAYLIST = "playlist/{kind}/{id}"
    fun playlist(kind: String, id: Long = 0) = "playlist/$kind/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.SONGS, "Τραγούδια", Icons.Filled.MusicNote),
    Tab(Routes.PLAYLISTS, "Playlists", Icons.AutoMirrored.Filled.QueueMusic),
    Tab(Routes.VIDEOS, "Βίντεο", Icons.Filled.VideoLibrary),
    Tab(Routes.GIFS, "GIF", Icons.Filled.Gif),
)

@Composable
fun AppRoot(
    permissionsGranted: StateFlow<Boolean>,
    onRequestPermissions: () -> Unit,
) {
    val granted by permissionsGranted.collectAsState()
    if (!granted) {
        PermissionScreen(onRequestPermissions)
        return
    }

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val player by PlayerConnection.state.collectAsState()
    val isTab = tabs.any { it.route == route }
    val showMini = player.hasMedia && route != Routes.NOW && route != Routes.EQ

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            NavHost(nav, startDestination = Routes.SONGS, modifier = Modifier.fillMaxSize()) {
                composable(Routes.SONGS) { SongsScreen(nav) }
                composable(Routes.PLAYLISTS) { PlaylistsScreen(nav) }
                composable(Routes.VIDEOS) { VideosScreen() }
                composable(Routes.GIFS) { GifsScreen() }
                composable(
                    Routes.NOW,
                    enterTransition = { slideInVertically { it } },
                    exitTransition = { fadeOut() },
                    popExitTransition = { slideOutVertically { it } },
                ) { NowPlayingScreen(nav) }
                composable(Routes.QUEUE) { QueueScreen(nav) }
                composable(Routes.EQ) { EqualizerScreen(nav) }
                composable(Routes.SETTINGS) { SettingsScreen(nav) }
                composable(
                    Routes.PLAYLIST,
                    arguments = listOf(
                        navArgument("kind") { type = NavType.StringType },
                        navArgument("id") { type = NavType.LongType },
                    )
                ) { entry ->
                    PlaylistDetailScreen(
                        nav,
                        kind = entry.arguments?.getString("kind") ?: "fav",
                        playlistId = entry.arguments?.getLong("id") ?: 0L,
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = showMini,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            MiniPlayer(
                onOpen = { nav.navigate(Routes.NOW) { launchSingleTop = true } },
                onQueue = { nav.navigate(Routes.QUEUE) { launchSingleTop = true } },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    .then(if (!isTab) Modifier.navigationBarsPadding() else Modifier)
            )
        }
        if (isTab) BottomBar(nav, route)
    }
}

@Composable
private fun BottomBar(nav: NavHostController, route: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF070B24))
            .navigationBarsPadding()
            .height(68.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val selected = route == tab.route
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        if (!selected) nav.navigate(tab.route) {
                            popUpTo(Routes.SONGS) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    tab.icon, tab.label,
                    tint = if (selected) Color.White else AppColors.TextFaint,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    tab.label,
                    fontSize = 12.sp,
                    color = if (selected) Color.White else AppColors.TextFaint,
                )
            }
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.LibraryMusic, null, tint = AppColors.Accent, modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(24.dp))
        Text("Καλώς ήρθες!", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(12.dp))
        Text(
            "Για να βρω τη μουσική, τα βίντεο και τα GIF σου, χρειάζομαι πρόσβαση στα αρχεία πολυμέσων του κινητού. " +
                "Η άδεια ειδοποιήσεων χρειάζεται για τα κουμπιά ελέγχου όταν η μουσική παίζει στο παρασκήνιο.",
            color = AppColors.TextDim, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.height(56.dp).fillMaxWidth()
        ) { Text("Δώσε πρόσβαση", fontSize = 17.sp) }
        Spacer(Modifier.height(12.dp))
        Text(
            "Αν είχες πατήσει «Να μην επιτρέπεται», άνοιξε Ρυθμίσεις → Εφαρμογές → Media Player → Άδειες.",
            color = AppColors.TextFaint, fontSize = 13.sp, textAlign = TextAlign.Center
        )
    }
}
