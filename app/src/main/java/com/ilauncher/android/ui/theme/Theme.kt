package com.ilauncher.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LauncherColors = darkColorScheme(
    primary = LauncherAccent,
    secondary = Color(0xFFB5E6DE),
    background = Color(0xFF0A0C1C),
    surface = Color(0xFF171A2B),
    onPrimary = Color(0xFF171329),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun ILauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LauncherColors, content = content)
}
