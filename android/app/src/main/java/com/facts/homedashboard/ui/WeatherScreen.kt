package com.facts.homedashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.facts.homedashboard.prayer.SettingsStore
import com.facts.homedashboard.weather.WeatherClient
import com.facts.homedashboard.weather.WeatherData
import com.facts.homedashboard.weather.WeatherDay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private val WeatherAccent = Color(0xFF4FC3F7)

@Composable
fun WeatherScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<WeatherData?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

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
                CurrentWeather(current)
                Spacer(Modifier.height(24.dp))
                Forecast(current.days)
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
                .padding(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(WeatherClient.emoji(data.now.code), fontSize = 84.sp)
            Spacer(Modifier.width(28.dp))
            Column {
                Text(
                    "${data.now.tempF}°",
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Bold,
                )
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
        Text(
            "$label  ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Forecast(days: List<WeatherDay>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        days.forEachIndexed { i, day ->
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        if (i == 0) "Today" else day.date.format(DAY_FMT),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (i == 0) WeatherAccent else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(WeatherClient.emoji(day.code), fontSize = 34.sp)
                    Text(
                        "${day.maxF}°  ${day.minF}°",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun Center(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

private val DAY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE")
