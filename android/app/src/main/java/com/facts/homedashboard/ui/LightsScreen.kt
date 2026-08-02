package com.facts.homedashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.facts.homedashboard.kasa.KasaClient
import com.facts.homedashboard.kasa.KasaDevice
import kotlinx.coroutines.launch

/**
 * Full-screen Kasa light/switch control: discovers devices on the LAN and shows
 * a toggle per device. Uses the verified [KasaClient] protocol directly.
 */
@Composable
fun LightsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<KasaDevice>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true; error = null
            runCatching { KasaClient.discover(context) }
                .onSuccess { devices = it }
                .onFailure { error = it.message ?: "Discovery failed" }
            loading = false
        }
    }

    // Discover once when the screen opens.
    androidx.compose.runtime.LaunchedEffect(Unit) { refresh() }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(8.dp))
            Text("Lights", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            Text(
                if (devices.isEmpty()) "" else "${devices.count { it.isOn }} on / ${devices.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { refresh() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Rescan")
            }
        }
        Spacer(Modifier.padding(6.dp))

        when {
            loading && devices.isEmpty() -> CenterBox { CircularProgressIndicator() }
            error != null && devices.isEmpty() -> CenterBox {
                Text("Couldn't reach the LAN: $error", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            devices.isEmpty() -> CenterBox {
                Text("No Kasa devices found on this Wi-Fi.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 260.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(devices, key = { it.ip }) { device ->
                    LightTile(device) { newOn ->
                        // Optimistic flip, then send; revert on failure.
                        devices = devices.map { if (it.ip == device.ip) it.copy(isOn = newOn) else it }
                        scope.launch {
                            runCatching { KasaClient.setPower(device.ip, newOn, device.isBulb) }
                                .onFailure {
                                    devices = devices.map { if (it.ip == device.ip) it.copy(isOn = !newOn) else it }
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LightTile(device: KasaDevice, onToggle: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.alias,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    device.model,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = device.isOn, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
