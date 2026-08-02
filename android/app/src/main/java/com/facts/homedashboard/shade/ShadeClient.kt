package com.facts.homedashboard.shade

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * SmartWings / Nien Made (Dexatek) motorized shade — local control.
 *
 * The shade runs a tiny plain-HTTP server on port 10123 (no auth, no TLS):
 *   POST /NM/v1/status   {"Timestamp":<unix>}          -> full device state
 *   POST /NM/v1/control  {"BottomRailPosition":0..100, "MiddleRailPosition":..,
 *                         "TaskID":<n>, "GroupID":<g>, "RoomID":<r>, "Timestamp":<unix>}
 *
 * BottomRailPosition: 100 = open, 0 = closed. Protocol decoded from a live capture.
 */
data class ShadeState(
    val host: String,
    val port: Int,
    val roomId: Int,
    val groupId: Int,
    val bottom: Int,   // 0 closed .. 100 open
    val middle: Int,
    val alias: String,
)

object ShadeClient {
    private const val SERVICE_TYPE = "_nien_made._tcp."
    private val taskCounter = AtomicInteger(((System.currentTimeMillis() / 1000) % 100000).toInt())

    /** Find the shade gateway via mDNS. Returns host:port, or null if not found. */
    suspend fun discover(context: Context, timeoutMs: Long = 5000): Pair<String, Int>? =
        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
                var resolved = false
                val resolveListener = object : NsdManager.ResolveListener {
                    override fun onResolveFailed(si: NsdServiceInfo, code: Int) {}
                    override fun onServiceResolved(si: NsdServiceInfo) {
                        if (resolved) return
                        val host = si.host?.hostAddress ?: return
                        resolved = true
                        if (cont.isActive) cont.resume(host to si.port)
                    }
                }
                val discoveryListener = object : NsdManager.DiscoveryListener {
                    override fun onStartDiscoveryFailed(s: String, e: Int) {}
                    override fun onStopDiscoveryFailed(s: String, e: Int) {}
                    override fun onDiscoveryStarted(s: String) {}
                    override fun onDiscoveryStopped(s: String) {}
                    override fun onServiceFound(si: NsdServiceInfo) {
                        @Suppress("DEPRECATION")
                        runCatching { nsd.resolveService(si, resolveListener) }
                    }
                    override fun onServiceLost(si: NsdServiceInfo) {}
                }
                nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
                cont.invokeOnCancellation {
                    runCatching { nsd.stopServiceDiscovery(discoveryListener) }
                }
            }
        }

    private suspend fun post(host: String, port: Int, path: String, body: String): String =
        withContext(Dispatchers.IO) {
            val conn = (URL("http://$host:$port$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4000
                readTimeout = 5000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            conn.inputStream.bufferedReader().use { it.readText() }
        }

    /** Read the shade's current state (first peripheral). */
    suspend fun status(host: String, port: Int): ShadeState {
        val ts = System.currentTimeMillis() / 1000
        val json = JSONObject(post(host, port, "/NM/v1/status", """{"Timestamp":$ts}"""))
        val p = json.getJSONArray("Peripherals").getJSONObject(0)
        return ShadeState(
            host = host,
            port = port,
            roomId = p.getInt("RoomID"),
            groupId = p.getInt("GroupID"),
            bottom = p.getInt("BottomRailPosition"),
            middle = p.getInt("MiddleRailPosition"),
            alias = "Shade",
        )
    }

    /**
     * Move the shade. bottom = rail height (0 closed .. 100 open); middle = flap
     * tilt (0 flaps closed .. 100 flaps open).
     */
    suspend fun setPosition(state: ShadeState, bottom: Int, middle: Int) {
        val ts = System.currentTimeMillis() / 1000
        val task = taskCounter.incrementAndGet()
        val body = """{"BottomRailPosition":${bottom.coerceIn(0, 100)},""" +
            """"MiddleRailPosition":${middle.coerceIn(0, 100)},"TaskID":$task,""" +
            """"GroupID":${state.groupId},"RoomID":${state.roomId},"Timestamp":$ts}"""
        post(state.host, state.port, "/NM/v1/control", body)
    }
}
