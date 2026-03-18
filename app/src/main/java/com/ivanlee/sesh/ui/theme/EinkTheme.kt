package com.ivanlee.sesh.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val EinkColorScheme = lightColorScheme(
    primary = EinkColors.OnBackground,
    onPrimary = EinkColors.Background,
    secondary = EinkColors.Focus,
    background = EinkColors.Background,
    onBackground = EinkColors.OnBackground,
    surface = EinkColors.Surface,
    onSurface = EinkColors.OnSurface,
    error = EinkColors.Abandoned,
    outline = EinkColors.ButtonBorder
)

@Composable
fun SeshTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EinkColorScheme,
        typography = EinkTypography,
        content = content
    )
}
