package com.facts.homedashboard.ui

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
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Placeholder wall layout: a live clock header plus a grid of feature tiles.
 * Each tile is a stub for a Phase-1 capability; we'll fill them in one by one.
 */
@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val now by rememberClock()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp)
    ) {
        ClockHeader(now)
        Spacer(Modifier.height(20.dp))
        PrayerCard()
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 240.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(dashboardTiles) { tile -> FeatureTile(tile) }
        }
    }
}

@Composable
private fun ClockHeader(now: LocalDateTime) {
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
        Text(
            text = "Home",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun FeatureTile(tile: Tile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
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
                    "Coming soon",
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
    Tile("Weather", Icons.Filled.WbSunny),
    Tile("Calendar", Icons.Filled.CalendarMonth),
    Tile("Chores", Icons.Filled.Checklist),
    Tile("Shopping List", Icons.Filled.ShoppingCart),
    Tile("Reminders", Icons.Filled.Notifications),
    Tile("Lights", Icons.Filled.Lightbulb),
)

private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private val DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
