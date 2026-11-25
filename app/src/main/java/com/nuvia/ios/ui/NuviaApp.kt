package com.nuvia.ios.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nuvia.ios.notifications.IslandOverlay
import com.nuvia.ios.ui.theme.*

private data class TabItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val tabs = listOf(
    TabItem("Island", Icons.Outlined.Waves),
    TabItem("Alerts", Icons.Outlined.Notifications),
    TabItem("Apps", Icons.Outlined.Apps),
    TabItem("Settings", Icons.Outlined.Settings)
)

@Composable
fun NuviaApp() {
    var selectedTab by rememberSaveable { mutableIntStateOf(3) }
    Scaffold(
        containerColor = NuviaBackground,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0B111D),
                tonalElevation = 0.dp,
                modifier = Modifier.height(76.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(24.dp)) },
                        label = { Text(tab.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NuviaText,
                            selectedTextColor = NuviaTeal,
                            indicatorColor = Color(0xFF343049),
                            unselectedIconColor = NuviaMuted,
                            unselectedTextColor = NuviaMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                0 -> IslandScreen()
                1 -> AlertsScreen()
                2 -> AppsScreen()
                else -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun SettingsScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { context.getSharedPreferences("nuvia_settings", Context.MODE_PRIVATE) }

    var islandEnabled by remember { mutableStateOf(prefs.getBoolean("island_enabled", true)) }
    var duration by remember { mutableFloatStateOf(prefs.getFloat("duration_seconds", 5f)) }
    var showMessagePreview by remember { mutableStateOf(prefs.getBoolean("show_preview", true)) }
    var compactIsland by remember { mutableStateOf(prefs.getBoolean("compact_island", false)) }

    var notificationAccess by remember { mutableStateOf(hasNotificationAccess(context)) }
    var overlayAccess by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    fun refreshPermissions() {
        notificationAccess = hasNotificationAccess(context)
        overlayAccess = Settings.canDrawOverlays(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // LazyColumn owns the vertical scroll range. The bottom navigation belongs to
    // the parent Scaffold, outside this list, so it stays fixed while settings scroll.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { BrandHeader() }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Settings", fontSize = 39.sp, lineHeight = 44.sp, fontWeight = FontWeight.Light, color = NuviaText)
                Text("Fine-tune how notifications appear and behave.", fontSize = 16.sp, color = NuviaMuted)
            }
        }
        item {
            SettingsPanel(title = "PERMISSIONS") {
                PermissionRow("♟", "Notification access", "Read incoming alerts from supported apps.", if (notificationAccess) "Connected" else "Required", notificationAccess) {
                    openNotificationAccess(context)
                }
                PanelDivider()
                PermissionRow("◆", "Display over other apps", "Show the floating island above your current app.", if (overlayAccess) "Allowed" else "Required", overlayAccess) {
                    openOverlayPermission(context)
                }
                PanelDivider()
                PermissionRow("ϟ", "HyperOS notification appearance", "Review Focus notifications and status-bar display in Xiaomi system settings.", "System setting", false) {
                    openAppNotificationSettings(context)
                }
                Button(
                    onClick = {
                        if (!overlayAccess) {
                            openOverlayPermission(context)
                            Toast.makeText(context, "Allow display over other apps, then return to Nuvia.", Toast.LENGTH_SHORT).show()
                        } else {
                            IslandOverlay.show(context, "Nuvia preview", "Your floating island is ready", duration.toLong())
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF171E2C), contentColor = NuviaMuted)
                ) {
                    Icon(Icons.Outlined.Waves, null, tint = NuviaPurpleBright)
                    Spacer(Modifier.width(12.dp))
                    Text("Test with selected app icon", fontWeight = FontWeight.Medium)
                }
            }
        }
        item {
            SettingsPanel(title = "NOTIFICATION BEHAVIOUR") {
                SettingSwitchRow("≋", "Floating island", "Show a compact live alert when supported apps notify you.", islandEnabled) {
                    islandEnabled = it
                    prefs.edit().putBoolean("island_enabled", it).apply()
                    if (it && !overlayAccess) openOverlayPermission(context)
                }
                PanelDivider()
                Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AccessTime, null, tint = NuviaPurple, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Collapse after", color = NuviaText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        Text("${duration.toInt()} seconds", color = NuviaTeal, fontSize = 13.sp)
                    }
                    Slider(
                        value = duration,
                        onValueChange = {
                            duration = it
                            prefs.edit().putFloat("duration_seconds", it).apply()
                        },
                        valueRange = 2f..10f,
                        steps = 7
                    )
                    Text("Choose how long an alert stays visible before it collapses.", color = NuviaMuted, fontSize = 12.sp)
                }
                PanelDivider()
                SettingSwitchRow("◉", "Show message previews", "Display notification text inside the floating island.", showMessagePreview) {
                    showMessagePreview = it
                    prefs.edit().putBoolean("show_preview", it).apply()
                }
                PanelDivider()
                SettingSwitchRow("▰", "Compact island", "Use a smaller pill for short alerts.", compactIsland) {
                    compactIsland = it
                    prefs.edit().putBoolean("compact_island", it).apply()
                }
            }
        }
        item {
            SettingsPanel(title = "MORE SETTINGS") {
                PermissionRow("▦", "Choose notification apps", "Control which apps are allowed to appear in Nuvia.", "Manage", false) {
                    Toast.makeText(context, "Per-app filtering is planned for a later build.", Toast.LENGTH_SHORT).show()
                }
                PanelDivider()
                PermissionRow("◈", "Appearance", "Nuvia dark theme and accent styling.", "Dark", true) {
                    Toast.makeText(context, "More appearance controls are planned for a later build.", Toast.LENGTH_SHORT).show()
                }
                PanelDivider()
                PermissionRow("i", "About Nuvia", "Version 0.1.0 · Notification experience for Android.", "Details", false) {
                    Toast.makeText(context, "Nuvia 0.1.0", Toast.LENGTH_SHORT).show()
                }
            }
        }
        item {
            Text(
                "Nuvia works alongside Android. Secure system screens and Xiaomi's native notification shade remain controlled by HyperOS.",
                color = NuviaMuted, fontSize = 12.sp, lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Box(
            Modifier.size(68.dp).clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(NuviaPurpleBright, Color(0xFF7255D7))),
                contentAlignment = Alignment.Center
        ) {
            Text("N", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Light)
        }
        Spacer(Modifier.width(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("nuvia", fontSize = 28.sp, color = NuviaText, fontWeight = FontWeight.Light)
            Text("C O N T R O L  T H E  E X P E R I E N C E", fontSize = 10.sp, color = NuviaMuted)
        }
    }
}

@Composable
private fun SettingsPanel(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(NuviaSurface)
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Text(title, color = NuviaMuted, fontSize = 11.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 16.dp))
        content()
    }
}

@Composable
private fun PanelDivider() {
    Box(Modifier.fillMaxWidth().padding(vertical = 2.dp).height(1.dp).background(NuviaBorder.copy(alpha = 0.75f)))
}

@Composable
private fun PermissionRow(icon: String, title: String, description: String, status: String, ready: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(NuviaSurfaceRaised), contentAlignment = Alignment.Center) {
            Text(icon, color = NuviaPurpleBright, fontSize = 23.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = NuviaText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(description, color = NuviaMuted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(status, color = if (ready) NuviaTeal else NuviaPurpleBright, fontSize = 12.sp, maxLines = 1)
            Icon(Icons.Outlined.KeyboardArrowRight, null, tint = NuviaMuted, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun SettingSwitchRow(icon: String, title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(NuviaSurfaceRaised), contentAlignment = Alignment.Center) {
            Text(icon, color = NuviaPurpleBright, fontSize = 25.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = NuviaText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(description, color = NuviaMuted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked, onCheckedChange, colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = NuviaPurple,
            uncheckedThumbColor = NuviaMuted, uncheckedTrackColor = NuviaSurfaceRaised
        ))
    }
}

@Composable
private fun IslandScreen() {
    val context = LocalContext.current
    val overlay = Settings.canDrawOverlays(context)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { BrandHeader() }
        item { Text("Floating Island", fontSize = 34.sp, color = NuviaText, fontWeight = FontWeight.Light) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = NuviaSurface), shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LIVE ALERT PREVIEW", color = NuviaMuted, fontSize = 11.sp, letterSpacing = 1.5.sp)
                    Box(Modifier.fillMaxWidth().height(74.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxWidth(0.72f).height(58.dp).clip(CircleShape).background(Color.Black), contentAlignment = Alignment.Center) {
                            Text("●     Nuvia is ready", color = Color.White, fontSize = 13.sp)
                        }
                    }
                    Text("Your notifications can appear in a compact floating pill while you use other apps.", color = NuviaMuted, fontSize = 13.sp)
                    Button(
                        onClick = {
                            if (overlay) IslandOverlay.show(context, "Nuvia preview", "Your floating island is ready", 5)
                            else openOverlayPermission(context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NuviaPurple),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text(if (overlay) "Show test island" else "Enable overlay permission") }
                }
            }
        }
        item { SettingsPanel(title = "QUICK STATUS") {
            StatusLine("Notification access", hasNotificationAccess(context))
            StatusLine("Display over other apps", overlay)
        } }
    }
}

@Composable
private fun AlertsScreen() {
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { BrandHeader() }
        item { Text("Alerts", fontSize = 36.sp, color = NuviaText, fontWeight = FontWeight.Light) }
        item {
            SettingsPanel(title = "NOTIFICATION ACCESS") {
                Text("When notification access is enabled, Nuvia can receive notification events from your selected apps and display a floating alert. Notification content stays on this device.", color = NuviaMuted, fontSize = 13.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { openNotificationAccess(context) }, colors = ButtonDefaults.buttonColors(containerColor = NuviaPurple)) {
                    Text("Manage notification access")
                }
            }
        }
        item { SettingsPanel(title = "PERMISSION STATUS") {
            StatusLine("Notification access", hasNotificationAccess(context))
            StatusLine("Overlay permission", Settings.canDrawOverlays(context))
        } }
    }
}

@Composable
private fun AppsScreen() {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { BrandHeader() }
        item { Text("Apps", fontSize = 36.sp, color = NuviaText, fontWeight = FontWeight.Light) }
        item { SettingsPanel(title = "SUPPORTED NOTIFICATIONS") {
            Text("Nuvia can listen for notifications posted by Android apps after you grant Notification access. Per-app allow/block controls will be added in a later update.", color = NuviaMuted, fontSize = 13.sp, lineHeight = 19.sp)
        } }
        item { SettingsPanel(title = "NEXT UP") {
            Text("App selection · Per-app alert styles · Icon-based notification filters", color = NuviaText, fontSize = 14.sp, lineHeight = 22.sp)
        } }
    }
}

@Composable
private fun StatusLine(label: String, enabled: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.CheckCircle, null, tint = if (enabled) NuviaTeal else NuviaMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = NuviaText, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text(if (enabled) "Ready" else "Not enabled", color = if (enabled) NuviaTeal else NuviaMuted, fontSize = 12.sp)
    }
}

private fun hasNotificationAccess(context: Context): Boolean {
    val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
    return enabled.contains(context.packageName)
}

private fun openNotificationAccess(context: Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        Toast.makeText(context, "Open Android Settings and search for Notification access.", Toast.LENGTH_LONG).show()
    }
}

private fun openOverlayPermission(context: Context) {
    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure {
        Toast.makeText(context, "Open Android Settings and allow Display over other apps for Nuvia.", Toast.LENGTH_LONG).show()
    }
}

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure { openNotificationAccess(context) }
}
