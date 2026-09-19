package com.meteosa.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = SaGreen,
    secondary = SaGold,
    tertiary = SaBlue,
    error = StormRed,
    background = SurfaceLight,
    surface = SurfaceLight
)

private val DarkColors = darkColorScheme(
    primary = SaGold,
    secondary = SaGreen,
    tertiary = SaBlue,
    error = StormRed,
    background = SurfaceDark,
    surface = SurfaceDark
)

@Composable
fun MeteoSaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MeteoSaTypography,
        content = content
    )
}
