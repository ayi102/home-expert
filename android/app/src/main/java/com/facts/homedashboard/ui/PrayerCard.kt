package com.facts.homedashboard.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.facts.homedashboard.adhan.AdhanPlayer
import com.facts.homedashboard.prayer.PrayerName
import com.facts.homedashboard.prayer.PrayerSettings
import com.facts.homedashboard.prayer.PrayerTimesEngine
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Compact call-to-prayer bar: next prayer + countdown on the left, the day's
 * times across the middle (next one highlighted), Hijri + Qibla on the right.
 * Kept small so the tiles below are the main navigable area. Tap to preview.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PrayerCard(
    modifier: Modifier = Modifier,
    settings: PrayerSettings = PrayerSettings.PLACEHOLDER,
) {
    val now by produceState(initialValue = Instant.now()) {
        while (true) {
            value = Instant.now()
            delay(1000)
        }
    }
    val zone = remember(settings.timeZoneId) { ZoneId.of(settings.timeZoneId) }
    val today = now.atZone(zone).toLocalDate()
    val daily = remember(settings, today) { PrayerTimesEngine.compute(settings, today) }
    val next = PrayerTimesEngine.nextPrayer(settings, now)
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                // Tap: play/stop the next prayer's adhan. Long-press: standard adhan.
                onClick = { AdhanPlayer.toggle(context, isFajr = next.name == PrayerName.FAJR) },
                onLongClick = { AdhanPlayer.play(context, isFajr = false) },
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Next prayer + countdown
            Column(modifier = Modifier.padding(end = 20.dp)) {
                Text(
                    "NEXT  ·  ${next.name.display}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "in ${formatCountdown(next.remaining(now))}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            // The day's times, spread across, next one highlighted
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                for (entry in daily.entries) {
                    val isNext = entry.name == next.name
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            entry.name.display,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isNext) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            daily.localTime(entry.name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            // Hijri + Qibla
            Column(
                modifier = Modifier.padding(start = 20.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(daily.hijriLabel, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Qibla ${daily.qiblaDegrees.toInt()}° N",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatCountdown(d: Duration): String {
    val total = d.seconds.coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%dh %02dm".format(h, m) else "%dm %02ds".format(m, s)
}
