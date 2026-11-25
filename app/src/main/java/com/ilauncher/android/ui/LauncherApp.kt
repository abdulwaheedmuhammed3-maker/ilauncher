package com.ilauncher.android.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: Drawable
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LauncherApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var installedApps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var showLibrary by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showLabels by rememberSaveable { mutableStateOf(true) }
    var wallpaperIndex by rememberSaveable { mutableIntStateOf(0) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var verticalDrag by remember { mutableFloatStateOf(0f) }

    suspend fun loadApps(): List<LaunchableApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(launchIntent, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .mapNotNull { info ->
                runCatching {
                    LaunchableApp(
                        packageName = info.activityInfo.packageName,
                        label = info.loadLabel(pm).toString(),
                        icon = info.loadIcon(pm)
                    )
                }.getOrNull()
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
            .toList()
    }

    LaunchedEffect(Unit) { installedApps = loadApps() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Refresh after returning from app install/uninstall or default-home settings.
                installedApps = installedApps
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val pages = remember(installedApps) {
        installedApps.chunked(16).ifEmpty { listOf(emptyList()) }
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val dockApps = remember(installedApps) {
        val choices = listOf(
            listOf("phone", "dialer"),
            listOf("message", "sms"),
            listOf("chrome", "browser"),
            listOf("camera")
        )
        choices.mapNotNull { words ->
            installedApps.firstOrNull { app ->
                words.any { word -> app.label.contains(word, ignoreCase = true) }
            }
        }.distinctBy { it.packageName }.let { selected ->
            (selected + installedApps.filterNot { app -> selected.any { it.packageName == app.packageName } }).take(4)
        }
    }

    val gradients = listOf(
        listOf(Color(0xFF090E24), Color(0xFF26244C), Color(0xFF514078)),
        listOf(Color(0xFF101827), Color(0xFF173B59), Color(0xFF7A587F)),
        listOf(Color(0xFF16102D), Color(0xFF3D2362), Color(0xFF9C5D76))
    )
    val backgroundBrush = Brush.linearGradient(gradients[wallpaperIndex % gradients.size])

    BackHandler(enabled = showLibrary || showSettings) {
        if (showSettings) showSettings = false else showLibrary = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .pointerInput(showLibrary, showSettings) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, amount -> verticalDrag += amount },
                    onDragEnd = {
                        if (verticalDrag < -70f && !showSettings) showLibrary = true
                        if (verticalDrag > 90f && showLibrary) showLibrary = false
                        verticalDrag = 0f
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showSettings = true }, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Launcher settings", tint = Color.White)
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ClockWidget(Modifier.weight(1f))
                CalendarWidget(Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f, fill = true)
            ) { page ->
                val visibleApps = pages.getOrNull(page).orEmpty()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false,
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleApps, key = { it.packageName }) { app ->
                        HomeAppIcon(app = app, showLabel = showLabels) { launchApp(context, app) }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            if (pages.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(pages.size) { page ->
                        Box(
                            Modifier.size(if (pagerState.currentPage == page) 7.dp else 5.dp)
                                .clip(CircleShape)
                                .background(if (pagerState.currentPage == page) Color.White else Color.White.copy(alpha = 0.48f))
                        )
                    }
                }
            } else {
                Text("●", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
            }

            Spacer(Modifier.height(9.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(27.dp).clickable { showLibrary = true },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Apps, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("App Library", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White.copy(alpha = 0.20f))
                    .padding(horizontal = 9.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dockApps.forEach { app ->
                    DockAppIcon(app) { launchApp(context, app) }
                }
                if (dockApps.isEmpty()) {
                    Text("Your favourite apps will appear here", color = Color.White, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (installedApps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading your apps…", color = Color.White, fontSize = 14.sp)
            }
        }

        if (showLibrary) {
            AppLibrary(
                apps = installedApps,
                query = searchText,
                onQueryChange = { searchText = it },
                onClose = { showLibrary = false },
                onLaunch = { launchApp(context, it) }
            )
        }

        if (showSettings) {
            LauncherSettings(
                showLabels = showLabels,
                onLabelsChange = { showLabels = it },
                wallpaperIndex = wallpaperIndex,
                onWallpaperChange = { wallpaperIndex = it },
                onClose = { showSettings = false }
            )
        }
    }
}

@Composable
private fun ClockWidget(modifier: Modifier = Modifier) {
    val date = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()) }
    val time = remember { SimpleDateFormat("h:mm", Locale.getDefault()).format(Date()) }
    Column(
        modifier = modifier.height(112.dp).clip(RoundedCornerShape(25.dp))
            .background(Color.White.copy(alpha = 0.17f)).padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(date.uppercase(Locale.getDefault()), color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp, maxLines = 1)
        Text(time, color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Light)
        Text("Good day", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
    }
}

@Composable
private fun CalendarWidget(modifier: Modifier = Modifier) {
    val day = remember { SimpleDateFormat("dd", Locale.getDefault()).format(Date()) }
    val month = remember { SimpleDateFormat("MMM", Locale.getDefault()).format(Date()) }
    Column(
        modifier = modifier.height(112.dp).clip(RoundedCornerShape(25.dp))
            .background(Color.White.copy(alpha = 0.17f)).padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("CALENDAR", color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp, letterSpacing = 1.sp)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(day, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light)
            Text(month, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        Text("No upcoming events", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
    }
}

@Composable
private fun HomeAppIcon(app: LaunchableApp, showLabel: Boolean, onClick: () -> Unit) {
    val bitmap = remember(app.packageName) { app.icon.toBitmap().asImageBitmap() }
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Image(bitmap = bitmap, contentDescription = app.label, modifier = Modifier.size(57.dp))
        if (showLabel) {
            Text(
                app.label,
                color = Color.White,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DockAppIcon(app: LaunchableApp, onClick: () -> Unit) {
    val bitmap = remember(app.packageName) { app.icon.toBitmap().asImageBitmap() }
    Image(
        bitmap = bitmap,
        contentDescription = app.label,
        modifier = Modifier.size(55.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
    )
}

@Composable
private fun AppLibrary(
    apps: List<LaunchableApp>,
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onLaunch: (LaunchableApp) -> Unit
) {
    val filtered = remember(apps, query) {
        apps.filter { it.label.contains(query, ignoreCase = true) }
    }
    Column(
        Modifier.fillMaxSize().background(Color(0xF20C1020)).padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("App Library", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Done", color = Color(0xFFC6B7FF)) }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.13f)).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.width(9.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 15.sp),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Search apps", color = Color.White.copy(alpha = 0.5f), fontSize = 15.sp)
                    inner()
                }
            )
        }
        Spacer(Modifier.height(18.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            items(filtered, key = { it.packageName }) { app ->
                HomeAppIcon(app = app, showLabel = true) { onLaunch(app) }
            }
        }
    }
}

@Composable
private fun LauncherSettings(
    showLabels: Boolean,
    onLabelsChange: (Boolean) -> Unit,
    wallpaperIndex: Int,
    onWallpaperChange: (Int) -> Unit,
    onClose: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Color(0xF20C1020)).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color(0xFF20253A)).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("iLauncher Settings", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClose) { Text("Done", color = Color(0xFFC6B7FF)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("App labels", color = Color.White, fontSize = 15.sp)
                    Text("Show names under home icons", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                }
                Switch(checked = showLabels, onCheckedChange = onLabelsChange)
            }
            Text("Wallpaper", color = Color.White, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(Color(0xFF26244C), Color(0xFF173B59), Color(0xFF5A306A)).forEachIndexed { index, color ->
                    Box(
                        Modifier.size(58.dp).clip(RoundedCornerShape(17.dp)).background(color)
                            .clickable { onWallpaperChange(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (wallpaperIndex == index) Text("✓", color = Color.White, fontSize = 22.sp)
                    }
                }
            }
            Text("Swipe up to open App Library. Swipe sideways to move between home pages.", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
        }
    }
}

private fun launchApp(context: Context, app: LaunchableApp) {
    val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
    if (intent == null) {
        Toast.makeText(context, "This app does not expose a launch activity.", Toast.LENGTH_SHORT).show()
        return
    }
    runCatching { context.startActivity(intent) }
        .onFailure { Toast.makeText(context, "Unable to open ${app.label}.", Toast.LENGTH_SHORT).show() }
}

private fun Drawable.toBitmap(): Bitmap {
    val width = intrinsicWidth.coerceAtLeast(1).coerceAtMost(192)
    val height = intrinsicHeight.coerceAtLeast(1).coerceAtMost(192)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val oldBounds = bounds
    setBounds(0, 0, width, height)
    draw(Canvas(bitmap))
    bounds = oldBounds
    return bitmap
}
