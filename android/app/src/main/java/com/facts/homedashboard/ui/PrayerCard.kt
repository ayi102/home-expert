package com.facts.homedashboard.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.facts.homedashboard.adhan.AdhanPlayer
import com.facts.homedashboard.prayer.PrayerName
import com.facts.homedashboard.prayer.PrayerSettings
import com.facts.homedashboard.prayer.PrayerTimesEngine
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

private val SalahAccent = Color(0xFF2DD4BF) // teal
private val SalahSurface = Color(0xFF161B22)

/**
 * Compact, colorful call-to-prayer bar. Tap = play/stop next prayer's adhan;
 * long-press = standard adhan.
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
                onClick = { AdhanPlayer.toggle(context, isFajr = next.name == PrayerName.FAJR) },
                onLongClick = { AdhanPlayer.play(context, isFajr = false) },
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(SalahAccent.copy(alpha = 0.18f), SalahSurface))
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon chip
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SalahAccent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Mosque, contentDescription = "Prayer", tint = SalahAccent)
            }
            Spacer(Modifier.width(16.dp))

            // Next prayer + countdown
            Column {
                Text(
                    "NEXT  ·  ${next.name.display}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SalahAccent,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "in ${formatCountdown(next.remaining(now))}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            // The day's times, next highlighted in an accent chip
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (entry in daily.entries) {
                    val isNext = entry.name == next.name
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = if (isNext) {
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SalahAccent.copy(alpha = 0.20f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        } else {
                            Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        },
                    ) {
                        Text(
                            entry.name.display,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isNext) SalahAccent else MaterialTheme.colorScheme.onSurfaceVariant,
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
            Column(horizontalAlignment = Alignment.End) {
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
