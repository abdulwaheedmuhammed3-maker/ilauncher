package com.nuvia.ios.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NuviaDarkColors = darkColorScheme(
    primary = NuviaPurple,
    secondary = NuviaTeal,
    background = NuviaBackground,
    surface = NuviaSurface,
    onPrimary = NuviaText,
    onSecondary = NuviaBackground,
    onBackground = NuviaText,
    onSurface = NuviaText
)

@Composable
fun NuviaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NuviaDarkColors,
        content = content
    )
}
