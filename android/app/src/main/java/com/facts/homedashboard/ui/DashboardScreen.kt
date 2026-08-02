package com.facts.homedashboard.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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

/**
 * The wall layout: live clock header, the call-to-prayer panel (fed by
 * auto-detected location), and a grid of feature tiles. Tapping "Lights" opens
 * the Kasa control screen.
 */
@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    var showLights by remember { mutableStateOf(false) }
    if (showLights) {
        LightsScreen(onBack = { showLights = false }, modifier = modifier)
        return
    }

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
            .padding(28.dp)
    ) {
        ClockHeader(now, onMaintenance = { showMaintenance = true })
        Spacer(Modifier.height(20.dp))
        PrayerCard(settings = settings)
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 260.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(dashboardTiles) { tile ->
                val context = LocalContext.current
                FeatureTile(
                    tile = tile,
                    onClick = when (tile.title) {
                        "Lights" -> ({ showLights = true })
                        "YouTube" -> ({ AppLauncher.launchOrNotify(context, "com.google.android.youtube", "YouTube") })
                        "Netflix" -> ({ AppLauncher.launchOrNotify(context, "com.netflix.mediaclient", "Netflix") })
                        else -> null
                    },
                )
            }
        }
    }
}

/** Builds prayer settings from device time zone + auto-detected GPS location. */
@Composable
private fun rememberPrayerSettings(): PrayerSettings {
    val context = LocalContext.current
    var settings by remember {
        mutableStateOf(PrayerSettings.PLACEHOLDER.copy(timeZoneId = ZoneId.systemDefault().id))
    }
    // On a new fix: update the UI, persist it, and re-arm the adhan alarm.
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
        // Long-press "Home" to open maintenance (exit kiosk to install apps).
        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.combinedClickable(onClick = {}, onLongClick = onMaintenance),
        )
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

/** Walks the context wrapper chain to the hosting Activity. */
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun FeatureTile(tile: Tile, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .let { if (onClick != null) it.clickable { onClick() } else it },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = tile.icon,
                contentDescription = tile.title,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(tile.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (onClick != null) "Tap to open" else "Coming soon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

/** A clock that ticks about once a second, aligned to the top of each second. */
@Composable
private fun rememberClock(): State<LocalDateTime> =
    produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            val current = LocalDateTime.now()
            value = current
            delay(1000L - (current.nano / 1_000_000L))
        }
    }

private data class Tile(val title: String, val icon: ImageVector)

private val dashboardTiles = listOf(
    Tile("Lights", Icons.Filled.Lightbulb),
    Tile("YouTube", Icons.Filled.SmartDisplay),
    Tile("Netflix", Icons.Filled.Movie),
    Tile("Weather", Icons.Filled.WbSunny),
    Tile("Calendar", Icons.Filled.CalendarMonth),
    Tile("Chores", Icons.Filled.Checklist),
    Tile("Shopping List", Icons.Filled.ShoppingCart),
    Tile("Reminders", Icons.Filled.Notifications),
)

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private val DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
