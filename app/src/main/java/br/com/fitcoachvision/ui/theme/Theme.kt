package br.com.fitcoachvision.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta do app. Verde para acerto e progresso, ambar para atencao,
// vermelho reservado exclusivamente para erro de sistema — nunca para
// julgamento da execucao do usuario.
val Ink = Color(0xFF0B0F14)
val Surface1 = Color(0xFF141A22)
val Surface2 = Color(0xFF1D2530)
val Accent = Color(0xFF4ADE80)
val AccentDim = Color(0xFF22C55E)
val Info = Color(0xFF38BDF8)
val Warn = Color(0xFFFBBF24)
val Danger = Color(0xFFF87171)
val TextHigh = Color(0xFFF1F5F9)
val TextMid = Color(0xFF94A3B8)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Ink,
    secondary = Info,
    onSecondary = Ink,
    background = Ink,
    onBackground = TextHigh,
    surface = Surface1,
    onSurface = TextHigh,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextMid,
    error = Danger,
    onError = Ink
)

private val LightColors = lightColorScheme(
    primary = AccentDim,
    onPrimary = Color.White,
    secondary = Info,
    background = Color(0xFFF8FAFC),
    surface = Color.White
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun FitCoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
