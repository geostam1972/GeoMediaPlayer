package gr.geo.mediaplayer.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavHostController
import gr.geo.mediaplayer.data.LibraryStore
import gr.geo.mediaplayer.data.MediaRepository
import gr.geo.mediaplayer.ui.AppColors
import gr.geo.mediaplayer.ui.ConfirmDialog
import gr.geo.mediaplayer.ui.Routes
import kotlinx.coroutines.launch

private fun isBatteryUnrestricted(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

@SuppressLint("BatteryLife")
@Composable
fun SettingsScreen(nav: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var unrestricted by remember { mutableStateOf(isBatteryUnrestricted(context)) }
    var confirmHistory by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        unrestricted = isBatteryUnrestricted(context)
        onPauseOrDispose { }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω", tint = Color.White)
            }
            Text("Ρυθμίσεις", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        }

        SettingRow(
            if (unrestricted) Icons.Filled.CheckCircle else Icons.Filled.BatteryChargingFull,
            "Αναπαραγωγή στο παρασκήνιο",
            if (unrestricted) "Ενεργό: το Samsung δεν θα σταματάει τη μουσική."
            else "Πάτα εδώ ώστε το Samsung να μη σταματάει ποτέ τη μουσική όταν κλειδώνει η οθόνη ή ανοίγεις άλλες εφαρμογές.",
            highlight = !unrestricted,
        ) {
            val intent = if (!unrestricted) {
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            }
            runCatching { context.startActivity(intent) }
        }
        SettingRow(Icons.Filled.Equalizer, "Ισοσταθμιστής", "Ρύθμιση ήχου και μπάσων") {
            nav.navigate(Routes.EQ)
        }
        SettingRow(Icons.Filled.Refresh, "Νέα σάρωση αρχείων", "Ψάξε ξανά για τραγούδια, βίντεο και GIF") {
            scope.launch {
                MediaRepository.refresh(context)
                Toast.makeText(
                    context,
                    "Βρέθηκαν ${MediaRepository.songs.value.size} τραγούδια, ${MediaRepository.videos.value.size} βίντεο, ${MediaRepository.gifs.value.size} GIF",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        SettingRow(Icons.Filled.DeleteSweep, "Καθαρισμός ιστορικού", "Πρόσφατα & πιο παιγμένα") {
            confirmHistory = true
        }
        val version = remember {
            runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
        }
        SettingRow(Icons.Filled.Info, "Έκδοση", version) {}
    }

    if (confirmHistory) {
        ConfirmDialog(
            "Καθαρισμός ιστορικού", "Να διαγραφεί το ιστορικό αναπαραγωγής;", "Καθαρισμός",
            { confirmHistory = false }
        ) { LibraryStore.clearHistory(); LibraryStore.clearCounts() }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    highlight: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (highlight) Color(0xFFFFB74D) else AppColors.Accent, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = AppColors.TextDim, fontSize = 14.sp)
        }
    }
}

