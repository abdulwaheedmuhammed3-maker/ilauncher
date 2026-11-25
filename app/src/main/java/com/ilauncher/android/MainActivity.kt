package com.ilauncher.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.ilauncher.android.ui.LauncherApp
import com.ilauncher.android.ui.theme.ILauncherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = android.graphics.Color.rgb(10, 12, 28)
        window.navigationBarColor = android.graphics.Color.rgb(10, 12, 28)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            ILauncherTheme {
                LauncherApp()
            }
        }
    }
}
