package com.facts.homedashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.facts.homedashboard.adhan.AdhanPlayer
import com.facts.homedashboard.prayer.DailyPrayerTimes
import com.facts.homedashboard.prayer.PrayerName
import com.facts.homedashboard.prayer.PrayerSettings
import com.facts.homedashboard.prayer.PrayerTimesEngine
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Live call-to-prayer panel: the next prayer with a countdown, the Hijri date,
 * Qibla bearing, and the day's six times with the next one highlighted.
 * Location comes from [settings] (a placeholder until GPS/settings wire in).
 */
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
    val remaining = next.remaining(now)
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { AdhanPlayer.play(context) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
    ) {
        Row(modifier = Modifier.padding(24.dp)) {
            NextPrayerColumn(
                nextName = next.name.display,
                remaining = remaining,
                hijri = daily.hijriLabel,
                qibla = daily.qiblaDegrees,
                modifier = Modifier.width(260.dp),
            )
            Spacer(Modifier.width(24.dp))
            TimesColumn(daily, highlight = next.name, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun NextPrayerColumn(
    nextName: String,
    remaining: Duration,
    hijri: String,
    qibla: Double,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            "Next  •  $nextName",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            formatCountdown(remaining),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        Text(hijri, style = MaterialTheme.typography.titleLarge)
        Text(
            "Qibla ${qibla.toInt()}° from N",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TimesColumn(
    daily: DailyPrayerTimes,
    highlight: PrayerName,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (entry in daily.entries) {
            val isNext = entry.name == highlight
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isNext) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else androidx.compose.ui.graphics.Color.Transparent
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    entry.name.display,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (entry.name.isPrayer) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    daily.localTime(entry.name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
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
