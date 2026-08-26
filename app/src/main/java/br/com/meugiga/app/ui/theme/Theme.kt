package br.com.meugiga.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import br.com.meugiga.app.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = GigaBlue,
    onPrimary = Color.White,
    primaryContainer = GigaBlueLight,
    onPrimaryContainer = GigaNavy,
    secondary = GigaCyan,
    onSecondary = Color.White,
    secondaryContainer = GigaCyanLight,
    onSecondaryContainer = Color(0xFF003737),
    tertiary = GigaOrange,
    error = GigaRed,
    background = GigaSurface,
    surface = Color.White,
    surfaceVariant = Color(0xFFE7E9F2),
    outline = Color(0xFF747785),
    outlineVariant = Color(0xFFD7DAE5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9EB2FF),
    onPrimary = Color(0xFF08245F),
    primaryContainer = Color(0xFF243867),
    onPrimaryContainer = Color(0xFFE0E6FF),
    secondary = Color(0xFF69D3D0),
    onSecondary = Color(0xFF003736),
    secondaryContainer = Color(0xFF123F40),
    onSecondaryContainer = Color(0xFF9DF1EE),
    tertiary = Color(0xFFFFB66D),
    error = Color(0xFFFFB4AB),
    background = Color(0xFF101318),
    surface = Color(0xFF171B22),
    surfaceVariant = Color(0xFF252A33),
    surfaceContainer = Color(0xFF1B1F27),
    surfaceContainerLow = Color(0xFF151920),
    surfaceContainerHigh = Color(0xFF222730),
    surfaceContainerHighest = Color(0xFF29303A),
    outline = Color(0xFF8B929E),
    outlineVariant = Color(0xFF3D4550),
)

@Composable
fun MeugigaTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = MeugigaTypography,
        content = content,
    )
}
