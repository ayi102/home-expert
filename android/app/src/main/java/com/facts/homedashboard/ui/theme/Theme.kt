package com.facts.homedashboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onBackground = OnBackground,
    onSurface = OnBackground,
    onSurfaceVariant = OnSurfaceVariant,
)

// The panel runs dark by default; a light scheme is provided for completeness.
private val LightColors = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
)

@Composable
fun HomeDashboardTheme(
    darkTheme: Boolean = true, // wall panel is dark by default
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
