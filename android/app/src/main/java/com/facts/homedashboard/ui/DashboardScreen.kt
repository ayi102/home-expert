package com.facts.homedashboard.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.facts.homedashboard.adhan.AdhanScheduler
import com.facts.homedashboard.kiosk.KioskManager
import com.facts.homedashboard.kiosk.KioskPrefs
import com.facts.homedashboard.location.LocationHelper
import com.facts.homedashboard.prayer.PrayerSettings
import com.facts.homedashboard.prayer.SettingsStore
import com.facts.homedashboard.util.AppLauncher
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Screen { HOME, LIGHTS, WEATHER }

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    when (screen) {
        Screen.LIGHTS -> LightsScreen(onBack = { screen = Screen.HOME }, modifier = modifier)
        Screen.WEATHER -> WeatherScreen(onBack = { screen = Screen.HOME }, modifier = modifier)
        Screen.HOME -> HomeContent(
            modifier = modifier,
            onOpenLights = { screen = Screen.LIGHTS },
            onOpenWeather = { screen = Screen.WEATHER },
        )
    }
}

@Composable
private fun HomeContent(modifier: Modifier, onOpenLights: () -> Unit, onOpenWeather: () -> Unit) {
    val now by rememberClock()
    val settings = rememberPrayerSettings()
    val context = LocalContext.current
    var showMaintenance by remember { mutableStateOf(false) }

    if (showMaintenance) {
        MaintenanceDialog(
            kioskEnabled = KioskPrefs.isEnabled(context),
            onExit = {
                KioskPrefs.setEnabled(context, false)
                context.findActivity()?.let { KioskManager.exitKiosk(it) }
                showMaintenance = false
            },
            onResume = {
                KioskPrefs.setEnabled(context, true)
                context.findActivity()?.let { KioskManager.startLockTaskIfOwner(it) }
                showMaintenance = false
            },
            onDismiss = { showMaintenance = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF141A24), Color(0xFF0B0E13))
                )
            )
            .padding(28.dp)
    ) {
        ClockHeader(now, onMaintenance = { showMaintenance = true })
        Spacer(Modifier.height(18.dp))
        PrayerCard(settings = settings)
        Spacer(Modifier.height(18.dp))
        TileGrid(
            tiles = dashboardTiles,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onClickFor = { tile ->
                when (tile.title) {
                    "Lights" -> onOpenLights
                    "Weather" -> onOpenWeather
                    "YouTube" -> ({ AppLauncher.launchOrNotify(context, "com.google.android.youtube", "YouTube") })
                    "Netflix" -> ({ AppLauncher.launchOrNotify(context, "com.netflix.mediaclient", "Netflix") })
                    else -> null
                }
            },
        )
    }
}

/** Fixed 4-column grid that fills the available height — no scrolling. */
@Composable
private fun TileGrid(
    tiles: List<Tile>,
    modifier: Modifier = Modifier,
    onClickFor: (Tile) -> (() -> Unit)?,
) {
    val columns = 4
    Column(modifier, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        tiles.chunked(columns).forEach { rowTiles ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                rowTiles.forEach { tile ->
                    FeatureTile(
                        tile = tile,
                        onClick = onClickFor(tile),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                repeat(columns - rowTiles.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FeatureTile(tile: Tile, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val enabled = onClick != null
    Card(
        modifier = modifier.let { if (enabled) it.clickable { onClick!!() } else it },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            tile.accent.copy(alpha = if (enabled) 0.26f else 0.12f),
                            Color(0xFF161B22),
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tile.accent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(tile.icon, contentDescription = tile.title, tint = tile.accent)
                }
                Column {
                    Text(
                        tile.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (enabled) "Open" else "Coming soon",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) tile.accent
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClockHeader(now: LocalDateTime, onMaintenance: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = now.format(TIME_FMT).lowercase(Locale.getDefault()),
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = now.format(DATE_FMT),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Brand (long-press for Maintenance).
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.combinedClickable(onClick = {}, onLongClick = onMaintenance),
        ) {
            Text(
                text = "home-expert",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "by Ali Ismail",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MaintenanceDialog(
    kioskEnabled: Boolean,
    onExit: () -> Unit,
    onResume: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Maintenance") },
        text = {
            Text(
                if (kioskEnabled) {
                    "Exit kiosk mode to install apps or change settings. The dashboard " +
                        "stays open; you can switch to Play Store / Settings. Re-open this " +
                        "and tap Resume when you're done."
                } else {
                    "Kiosk is currently OFF. Tap Resume to pin the dashboard back down."
                }
            )
        },
        confirmButton = {
            if (kioskEnabled) {
                TextButton(onClick = onExit) { Text("Exit kiosk") }
            } else {
                TextButton(onClick = onResume) { Text("Resume kiosk") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

/** Builds prayer settings from device time zone + auto-detected GPS location. */
@Composable
private fun rememberPrayerSettings(): PrayerSettings {
    val context = LocalContext.current
    var settings by remember {
        mutableStateOf(PrayerSettings.PLACEHOLDER.copy(timeZoneId = ZoneId.systemDefault().id))
    }
    val onFix: (Double, Double) -> Unit = { lat, lng ->
        val updated = settings.copy(latitude = lat, longitude = lng)
        settings = updated
        SettingsStore.save(context, updated)
        AdhanScheduler.scheduleNext(context)
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) LocationHelper.requestLocation(context, onFix)
    }
    LaunchedEffect(Unit) {
        if (LocationHelper.hasPermission(context)) {
            LocationHelper.requestLocation(context, onFix)
        } else {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    return settings
}

@Composable
private fun rememberClock(): State<LocalDateTime> =
    produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            val current = LocalDateTime.now()
            value = current
            delay(1000L - (current.nano / 1_000_000L))
        }
    }

private data class Tile(val title: String, val icon: ImageVector, val accent: Color)

private val dashboardTiles = listOf(
    Tile("Lights", Icons.Filled.Lightbulb, Color(0xFFFFC24B)),
    Tile("YouTube", Icons.Filled.SmartDisplay, Color(0xFFFF5252)),
    Tile("Netflix", Icons.Filled.Movie, Color(0xFFE50914)),
    Tile("Weather", Icons.Filled.WbSunny, Color(0xFF4FC3F7)),
    Tile("Calendar", Icons.Filled.CalendarMonth, Color(0xFF66BB6A)),
    Tile("Chores", Icons.Filled.Checklist, Color(0xFFBA68C8)),
    Tile("Shopping List", Icons.Filled.ShoppingCart, Color(0xFF4DB6AC)),
    Tile("Reminders", Icons.Filled.Notifications, Color(0xFFFFA726)),
)

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private val DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())

/** Walks the context wrapper chain to the hosting Activity. */
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
