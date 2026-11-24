package gr.geo.mediaplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import gr.geo.mediaplayer.playback.AudioEffects
import gr.geo.mediaplayer.ui.AppColors

@Composable
fun EqualizerScreen(nav: NavHostController) {
    val eq by AudioEffects.state.collectAsState()

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω", tint = Color.White)
            }
            Text(
                "Ισοσταθμιστής", style = MaterialTheme.typography.headlineMedium, color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = eq.enabled,
                onCheckedChange = { AudioEffects.setEnabled(it) },
                enabled = eq.available,
                colors = SwitchDefaults.colors(checkedTrackColor = AppColors.Accent),
                modifier = Modifier.padding(end = 12.dp)
            )
        }

        if (!eq.available) {
            Text(
                "Ο ισοσταθμιστής ενεργοποιείται μόλις ξεκινήσει η αναπαραγωγή ενός τραγουδιού.",
                color = AppColors.TextDim, modifier = Modifier.padding(24.dp)
            )
        } else EqControls()
    }
}

@Composable
private fun EqControls() {
    val eq by AudioEffects.state.collectAsState()
    Column {
        val alpha = if (eq.enabled) 1f else 0.4f

        // presets
        Text("Προεπιλογές", color = AppColors.TextDim, modifier = Modifier.padding(start = 20.dp, top = 8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            eq.presets.forEachIndexed { i, name ->
                val sel = i == eq.currentPreset
                Text(
                    name,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (sel) AppColors.Accent else AppColors.SurfaceStrong)
                        .clickable(enabled = eq.enabled) { AudioEffects.usePreset(i) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        // bands
        eq.bands.forEach { band ->
            val hz = band.centerHz
            val label = if (hz >= 1000) "${hz / 1000} kHz" else "$hz Hz"
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = Color.White.copy(alpha = alpha), modifier = Modifier.width(70.dp))
                Slider(
                    value = band.levelMb.toFloat(),
                    onValueChange = { AudioEffects.setBand(band.index, it.toInt()) },
                    valueRange = eq.minMb.toFloat()..eq.maxMb.toFloat(),
                    enabled = eq.enabled,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = AppColors.Accent),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "%+d dB".format(band.levelMb / 100),
                    color = AppColors.TextDim, fontSize = 13.sp,
                    modifier = Modifier.width(56.dp).padding(start = 8.dp)
                )
            }
        }

        if (eq.bassSupported) {
            Spacer(Modifier.height(12.dp))
            Text("Ενίσχυση μπάσων", color = AppColors.TextDim, modifier = Modifier.padding(start = 20.dp))
            Slider(
                value = eq.bassStrength.toFloat(),
                onValueChange = { AudioEffects.setBass(it.toInt()) },
                valueRange = 0f..1000f,
                enabled = eq.enabled,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = AppColors.Accent),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        TextButton(
            onClick = { AudioEffects.reset() },
            enabled = eq.enabled,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp)
        ) { Text("Επαναφορά") }
    }
}
