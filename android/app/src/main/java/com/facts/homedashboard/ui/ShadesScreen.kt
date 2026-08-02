package com.facts.homedashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.facts.homedashboard.shade.ShadeClient
import com.facts.homedashboard.shade.ShadeState
import com.facts.homedashboard.shade.ShadeStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ShadeAccent = Color(0xFF8C9EFF)
private val BtnBg = Color(0xFF2A2F3A)

@Composable
fun ShadesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ShadeState?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var rail by remember { mutableFloatStateOf(0f) }
    var flap by remember { mutableFloatStateOf(50f) }
    var moving by remember { mutableStateOf(false) }
    var movingLabel by remember { mutableStateOf("") }

    suspend fun resolveHost(): Pair<String, Int>? {
        ShadeStore.load(context)?.let { return it }
        return ShadeClient.discover(context)?.also { ShadeStore.save(context, it.first, it.second) }
    }

    fun refresh() {
        scope.launch {
            loading = true; error = null
            try {
                val hp = resolveHost() ?: run { error = "Shade not found on Wi-Fi"; loading = false; return@launch }
                val s = try {
                    ShadeClient.status(hp.first, hp.second)
                } catch (e: Exception) {
                    val d = ShadeClient.discover(context) ?: throw e
                    ShadeStore.save(context, d.first, d.second)
                    ShadeClient.status(d.first, d.second)
                }
                state = s; rail = s.bottom.toFloat(); flap = s.middle.toFloat()
            } catch (e: Exception) {
                error = e.message ?: "Couldn't reach the shade"
            }
            loading = false
        }
    }

    // Send a command, show a "moving" indicator, then re-read the real state.
    fun send(label: String, bottom: Int, middle: Int) {
        val s = state ?: return
        rail = bottom.toFloat(); flap = middle.toFloat()
        state = s.copy(bottom = bottom, middle = middle)
        movingLabel = label; moving = true
        scope.launch {
            runCatching { ShadeClient.setPosition(s, bottom, middle) }
            delay(6000)
            runCatching {
                val st = ShadeClient.status(s.host, s.port)
                state = st; rail = st.bottom.toFloat(); flap = st.middle.toFloat()
            }
            moving = false
        }
    }

    fun moveRail(bottom: Int) {
        val s = state ?: return
        send(if (bottom == 0) "Closing…" else if (bottom == 100) "Opening…" else "Moving to $bottom%…", bottom, s.middle)
    }
    fun moveFlap(middle: Int) {
        val s = state ?: return
        send(if (middle == 0) "Closing flaps…" else if (middle == 100) "Opening flaps…" else "Tilting flaps…", s.bottom, middle)
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF171A2B), Color(0xFF0B0E13))))
            .padding(28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(8.dp))
            Text("Shades", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { refresh() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
        }
        Spacer(Modifier.height(16.dp))

        val s = state
        when {
            loading && s == null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null && s == null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            s != null -> Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(ShadeAccent.copy(alpha = 0.22f), Color(0xFF161B22))))
                        .padding(30.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when (rail.toInt()) { 100 -> "Open"; 0 -> "Closed"; else -> "${rail.toInt()}% open" },
                            fontSize = 42.sp, fontWeight = FontWeight.Bold,
                        )
                        if (moving) {
                            Spacer(Modifier.width(18.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 3.dp,
                                color = ShadeAccent,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(movingLabel, style = MaterialTheme.typography.titleMedium, color = ShadeAccent)
                        }
                    }
                    Text("Living room shade", style = MaterialTheme.typography.titleMedium, color = ShadeAccent)

                    ControlBlock(
                        label = "Shade", selected = rail.toInt(),
                        onClose = { moveRail(0) }, onHalf = { moveRail(50) }, onOpen = { moveRail(100) },
                        slider = rail, onSlider = { rail = it }, onSliderDone = { moveRail(rail.toInt()) },
                    )
                    ControlBlock(
                        label = "Flaps", selected = flap.toInt(),
                        onClose = { moveFlap(0) }, onHalf = { moveFlap(50) }, onOpen = { moveFlap(100) },
                        slider = flap, onSlider = { flap = it }, onSliderDone = { moveFlap(flap.toInt()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlBlock(
    label: String,
    selected: Int,
    onClose: () -> Unit,
    onHalf: () -> Unit,
    onOpen: () -> Unit,
    slider: Float,
    onSlider: (Float) -> Unit,
    onSliderDone: () -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Preset("Close", active = selected == 0, onClick = onClose)
        Preset("50%", active = selected == 50, onClick = onHalf)
        Preset("Open", active = selected == 100, onClick = onOpen)
    }
    Slider(
        value = slider, onValueChange = onSlider, onValueChangeFinished = onSliderDone,
        valueRange = 0f..100f,
        colors = SliderDefaults.colors(thumbColor = ShadeAccent, activeTrackColor = ShadeAccent),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Closed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Open", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RowScope.Preset(text: String, active: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(58.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) ShadeAccent else BtnBg,
            contentColor = if (active) Color(0xFF10131A) else Color.White,
        ),
    ) { Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
}
