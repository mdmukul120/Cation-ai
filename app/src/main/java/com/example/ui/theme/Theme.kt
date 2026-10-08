package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF0C0A14),
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = Color(0xFF80F2FF),
    secondary = NeonYellow,
    onSecondary = Color(0xFF0C0A14),
    secondaryContainer = Color(0xFF473C00),
    onSecondaryContainer = Color(0xFFFFEE80),
    tertiary = GrokCoral,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkOutline
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
