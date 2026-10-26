package com.ahsen.hava.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HavaColors = darkColorScheme(
    primary = HavaYellow,
    onPrimary = HavaNavy,
    secondary = HavaYellow,
    background = HavaNavy,
    onBackground = TextPrimary,
    surface = HavaNavy,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
)

@Composable
fun HavaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HavaColors,
        typography = Typography,
        content = content,
    )
}