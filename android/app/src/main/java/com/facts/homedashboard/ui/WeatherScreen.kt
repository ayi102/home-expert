package com.facts.homedashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.facts.homedashboard.prayer.SettingsStore
import com.facts.homedashboard.weather.HourEntry
import com.facts.homedashboard.weather.WeatherClient
import com.facts.homedashboard.weather.WeatherData
import com.facts.homedashboard.weather.WeatherDay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val WeatherAccent = Color(0xFF4FC3F7)
private val RainBlue = Color(0xFF29B6F6)
private val Track = Color(0xFF1E2530)

@Composable
fun WeatherScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<WeatherData?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    fun refresh() {
        scope.launch {
            loading = true; error = null
            val s = SettingsStore.load(context)
            runCatching { WeatherClient.fetch(s.latitude, s.longitude) }
                .onSuccess { data = it }
                .onFailure { error = it.message ?: "Couldn't load weather" }
            loading = false
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF14202B), Color(0xFF0B0E13))))
            .padding(28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(8.dp))
            Text("Weather", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { refresh() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
        }
        Spacer(Modifier.height(12.dp))

        val current = data
        when {
            loading && current == null -> Center { CircularProgressIndicator() }
            error != null && current == null -> Center {
                Text(error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            current != null -> {
                val selected = selectedDate ?: current.days.first().date
                CurrentWeather(current)
                Spacer(Modifier.height(20.dp))
                Forecast(current.days, selected) { selectedDate = it }
                Spacer(Modifier.height(20.dp))
                HourlyRain(current.hours, selected)
            }
        }
    }
}

@Composable
private fun CurrentWeather(data: WeatherData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(WeatherAccent.copy(alpha = 0.20f), Color(0xFF161B22))))
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(WeatherClient.emoji(data.now.code), fontSize = 72.sp)
            Spacer(Modifier.width(24.dp))
            Column {
                Text("${data.now.tempF}°", fontSize = 68.sp, fontWeight = FontWeight.Bold)
                Text(
                    WeatherClient.label(data.now.code),
                    style = MaterialTheme.typography.titleLarge,
                    color = WeatherAccent,
                )
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Detail("Feels like", "${data.now.feelsF}°")
                Detail("Humidity", "${data.now.humidity}%")
                Detail("Wind", "${data.now.windMph} mph")
            }
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label  ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Forecast(days: List<WeatherDay>, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        days.forEachIndexed { i, day ->
            val isSelected = day.date == selected
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(day.date) }
                    .then(
                        if (isSelected) Modifier.border(2.dp, WeatherAccent, RoundedCornerShape(18.dp))
                        else Modifier
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) WeatherAccent.copy(alpha = 0.16f) else Color(0xFF161B22)
                ),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        if (i == 0) "Today" else day.date.format(DAY_FMT),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) WeatherAccent else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(WeatherClient.emoji(day.code), fontSize = 30.sp)
                    Text("${day.maxF}°  ${day.minF}°", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun HourlyRain(hours: List<HourEntry>, date: LocalDate) {
    val dayHours = hours.filter { it.time.toLocalDate() == date }
    if (dayHours.isEmpty()) return
    val peak = dayHours.maxByOrNull { it.rainChance }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "Rain by hour · ${if (date == LocalDate.now()) "Today" else date.format(FULL_FMT)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(14.dp))
            if (peak != null && peak.rainChance > 0) {
                Text(
                    "peak ${peak.rainChance}% around ${hourLabel(peak)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RainBlue,
                )
            } else {
                Text("no rain expected", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(dayHours) { h -> HourCell(h) }
        }
    }
}

@Composable
private fun HourCell(h: HourEntry) {
    Column(
        modifier = Modifier.width(50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "${h.rainChance}%",
            style = MaterialTheme.typography.bodySmall,
            color = if (h.rainChance > 0) RainBlue else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (h.rainChance >= 50) FontWeight.Bold else FontWeight.Normal,
        )
        // Rain-chance bar (fills from the bottom).
        Box(
            modifier = Modifier
                .height(90.dp)
                .width(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Track),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(h.rainChance / 100f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RainBlue)
            )
        }
        Text(hourLabel(h), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${h.tempF}°", style = MaterialTheme.typography.bodyMedium)
    }
}

private fun hourLabel(h: HourEntry): String {
    val hour = h.time.hour
    val h12 = ((hour + 11) % 12) + 1
    return "$h12${if (hour < 12) "a" else "p"}"
}

@Composable
private fun Center(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

private val DAY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE")
private val FULL_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
