package gr.geo.mediaplayer.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads (and caches) the cover of a song / frame of a video. Returns null if there is none. */
suspend fun loadThumb(context: Context, uri: Uri?, sizePx: Int): Bitmap? {
    if (uri == null) return null
    val key = "${uri}_$sizePx"
    ThumbCache.get(key)?.let { return it }
    if (ThumbCache.isMissing(key)) return null
    val bmp = withContext(Dispatchers.IO) {
        runCatching { context.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null) }.getOrNull()
    }
    if (bmp != null) ThumbCache.put(key, bmp) else ThumbCache.markMissing(key)
    return bmp
}

/** Colours taken from the album cover, used to tint the player. */
data class ArtColors(
    val top: Color,
    val bottom: Color,
    val accent: Color,
) {
    /** White or dark icon, whichever reads better on the accent colour. */
    val onAccent: Color get() = if (accent.luminance() > 0.55f) Color(0xFF111111) else Color.White

    companion object {
        val Default = ArtColors(AppColors.BgTop, AppColors.BgBottom, AppColors.Accent)
    }
}

private val colorCache = HashMap<String, ArtColors>()

private fun Int.toColor() = Color(this)

private fun extract(source: Bitmap): ArtColors {
    // Thumbnails can be GPU-only ("hardware") bitmaps; Palette needs to read the pixels.
    val bmp = if (source.config == Bitmap.Config.HARDWARE) source.copy(Bitmap.Config.ARGB_8888, false) else source
    val p = Palette.from(bmp).maximumColorCount(16).generate()
    val darkSwatch = p.darkVibrantSwatch ?: p.darkMutedSwatch ?: p.mutedSwatch ?: p.dominantSwatch
    val accentSwatch = p.vibrantSwatch ?: p.lightVibrantSwatch ?: p.mutedSwatch ?: p.dominantSwatch
    if (darkSwatch == null || accentSwatch == null) return ArtColors.Default

    // Keep the background dark enough for white text.
    var top = darkSwatch.rgb.toColor()
    while (top.luminance() > 0.12f) top = lerpColor(top, Color.Black, 0.25f)
    val bottom = lerpColor(top, Color(0xFF05060F), 0.7f)
    var accent = accentSwatch.rgb.toColor()
    if (accent.luminance() < 0.18f) accent = lerpColor(accent, Color.White, 0.35f)
    return ArtColors(top, bottom, accent)
}

private fun lerpColor(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f,
)

/** Animated colours for the current cover. Falls back to the app's purple theme. */
@Composable
fun rememberArtColors(uri: Uri?): ArtColors {
    val context = LocalContext.current
    var target by remember { mutableStateOf(uri?.toString()?.let { colorCache[it] } ?: ArtColors.Default) }
    LaunchedEffect(uri) {
        val key = uri?.toString()
        if (key == null) { target = ArtColors.Default; return@LaunchedEffect }
        colorCache[key]?.let { target = it; return@LaunchedEffect }
        val bmp = loadThumb(context, uri, 256)
        val colors = if (bmp == null) ArtColors.Default
        else withContext(Dispatchers.Default) { runCatching { extract(bmp) }.getOrDefault(ArtColors.Default) }
        colorCache[key] = colors
        target = colors
    }
    val spec = tween<Color>(700)
    val top by animateColorAsState(target.top, spec, label = "top")
    val bottom by animateColorAsState(target.bottom, spec, label = "bottom")
    val accent by animateColorAsState(target.accent, spec, label = "accent")
    return ArtColors(top, bottom, accent)
}
