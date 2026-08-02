package com.facts.homedashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.facts.homedashboard.kasa.KasaClient
import com.facts.homedashboard.kasa.KasaDevice
import com.facts.homedashboard.kasa.LightGroups
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val UNASSIGNED = "Unassigned"
private val SECTIONS = listOf(LightGroups.UPSTAIRS, LightGroups.DOWNSTAIRS, UNASSIGNED)

/**
 * Kasa lights grouped into Upstairs / Downstairs / Unassigned. Long-press a
 * light and drag it onto another section to reassign it (persisted by deviceId).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LightsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<KasaDevice>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // deviceId -> group ("Upstairs"/"Downstairs"); absent = Unassigned.
    val assignments = remember { mutableStateMapOf<String, String>() }

    // Drag state.
    var dragged by remember { mutableStateOf<KasaDevice?>(null) }
    var pointer by remember { mutableStateOf(Offset.Zero) }
    var hovered by remember { mutableStateOf<String?>(null) }
    val bounds = remember { mutableStateMapOf<String, Rect>() }

    fun refresh() {
        scope.launch {
            loading = true; error = null
            runCatching { KasaClient.discover(context) }
                .onSuccess { devices = it }
                .onFailure { error = it.message ?: "Discovery failed" }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            kotlinx.coroutines.delay(30_000)
        }
    }
    // Load persisted groups for whatever we discovered.
    LaunchedEffect(devices) {
        devices.forEach { d ->
            val g = LightGroups.groupOf(context, d.deviceId)
            if (g != null) assignments[d.deviceId] = g else assignments.remove(d.deviceId)
        }
    }

    fun keyOf(d: KasaDevice) = assignments[d.deviceId] ?: UNASSIGNED
    fun hitTest(p: Offset) = bounds.entries.firstOrNull { it.value.contains(p) }?.key

    fun toggle(d: KasaDevice, on: Boolean) {
        devices = devices.map { if (it.deviceId == d.deviceId) it.copy(isOn = on) else it }
        scope.launch {
            runCatching { KasaClient.setPower(d.ip, on, d.isBulb) }
                .onFailure { devices = devices.map { if (it.deviceId == d.deviceId) it.copy(isOn = !on) else it } }
        }
    }

    fun drop(d: KasaDevice) {
        val target = hovered
        if (target != null && target != keyOf(d)) {
            if (target == UNASSIGNED) {
                assignments.remove(d.deviceId)
                LightGroups.assign(context, d.deviceId, null)
            } else {
                assignments[d.deviceId] = target
                LightGroups.assign(context, d.deviceId, target)
            }
        }
        dragged = null; hovered = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E13))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(8.dp))
                Text("Lights", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(14.dp))
                Text(
                    "long-press a light and drag it to a section",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                if (devices.isNotEmpty()) {
                    Text(
                        "${devices.count { it.isOn }} on / ${devices.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { refresh() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Rescan")
                }
            }
            Spacer(Modifier.height(8.dp))

            when {
                loading && devices.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                error != null && devices.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Couldn't reach the LAN: $error", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                else -> Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SECTIONS.forEach { section ->
                        val members = devices.filter { keyOf(it) == section }
                        SectionCard(
                            title = section,
                            count = members.size,
                            isHovered = dragged != null && hovered == section,
                            onBounds = { bounds[section] = it },
                        ) {
                            if (members.isEmpty()) {
                                Text(
                                    "Drop lights here",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(6.dp),
                                )
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                members.forEach { device ->
                                    LightTile(
                                        device = device,
                                        isDragged = dragged?.deviceId == device.deviceId,
                                        onToggle = { toggle(device, it) },
                                        onDragStart = { abs -> dragged = device; pointer = abs; hovered = hitTest(abs) },
                                        onDragMove = { abs -> pointer = abs; hovered = hitTest(abs) },
                                        onDragEnd = { drop(device) },
                                        onDragCancel = { dragged = null; hovered = null },
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        // Floating ghost that follows the finger while dragging.
        dragged?.let { d ->
            Box(
                modifier = Modifier
                    .offset { IntOffset((pointer.x - 110).roundToInt(), (pointer.y - 40).roundToInt()) }
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    d.alias,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    count: Int,
    isHovered: Boolean,
    onBounds: (Rect) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { onBounds(it.boundsInRoot()) }
            .clipToBackground(isHovered)
            .padding(16.dp),
    ) {
        Text(
            "$title  ·  $count",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

private fun Modifier.clipToBackground(isHovered: Boolean): Modifier = this
    .background(Color(0xFF141A24), RoundedCornerShape(18.dp))
    .then(
        if (isHovered) Modifier.border(2.dp, Color(0xFF4FC3F7), RoundedCornerShape(18.dp)) else Modifier
    )

@Composable
private fun LightTile(
    device: KasaDevice,
    isDragged: Boolean,
    onToggle: (Boolean) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    var tilePos by remember { mutableStateOf(Offset.Zero) }
    Card(
        modifier = Modifier
            .width(240.dp)
            .onGloballyPositioned { tilePos = it.positionInRoot() }
            .pointerInput(device.deviceId) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { off -> onDragStart(tilePos + off) },
                    onDrag = { change, _ -> change.consume(); onDragMove(tilePos + change.position) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragCancel() },
                )
            }
            .alpha(if (isDragged) 0.35f else 1f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.DragIndicator,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.alias,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    device.model,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = device.isOn, onCheckedChange = onToggle)
        }
    }
}
