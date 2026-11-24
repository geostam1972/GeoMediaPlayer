package gr.geo.mediaplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppColors {
    val Accent = Color(0xFF7B8CFF)
    val AccentDeep = Color(0xFF5B6CFF)
    val Violet = Color(0xFF8A4DFF)
    val BgTop = Color(0xFF2A2462)
    val BgMid = Color(0xFF141A4A)
    val BgBottom = Color(0xFF070B24)
    val Surface = Color(0x22FFFFFF)
    val SurfaceStrong = Color(0x33FFFFFF)
    val TextDim = Color(0xB3FFFFFF)
    val TextFaint = Color(0x80FFFFFF)

    val Background = Brush.verticalGradient(listOf(BgTop, BgMid, BgBottom))
    val MiniPlayer = Brush.horizontalGradient(listOf(AccentDeep, Violet))
}

private val scheme = darkColorScheme(
    primary = AppColors.Accent,
    onPrimary = Color.White,
    secondary = AppColors.Violet,
    background = AppColors.BgBottom,
    surface = Color(0xFF151B45),
    surfaceVariant = Color(0xFF1E2559),
    surfaceContainer = Color(0xFF1A2050),
    surfaceContainerHigh = Color(0xFF222a60),
    surfaceContainerHighest = Color(0xFF2A3370),
    onSurface = Color.White,
    onSurfaceVariant = AppColors.TextDim,
    outline = AppColors.TextFaint,
)

private val typography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography) {
        Box(Modifier.fillMaxSize().background(AppColors.Background)) {
            content()
        }
    }
}
