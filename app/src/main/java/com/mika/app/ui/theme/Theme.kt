package com.mika.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PinkPrimary = Color(0xFFFF80AB)
val PinkSecondary = Color(0xFFEA80FC)
val PurpleAccent = Color(0xFFB388FF)
val DarkBackground = Color(0xFF120E18)
val DarkSurface = Color(0xFF1E1728)
val DarkSurfaceVariant = Color(0xFF2D233C)
val TextPrimary = Color(0xFFF3E5F5)
val TextSecondary = Color(0xFFCE93D8)

private val DarkColors = darkColorScheme(
    primary = PinkPrimary,
    secondary = PinkSecondary,
    tertiary = PurpleAccent,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun MikaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography(),
        content = content
    )
}
