package com.facts.homedashboard.kasa

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Collections

/**
 * TP-Link Kasa local-LAN client — Kotlin port of the Node reference in
 * tools/kasa (verified against 22 real switches). Legacy port-9999 protocol:
 * JSON obfuscated with an autokey XOR cipher (seed 0xAB); TCP frames carry a
 * 4-byte big-endian length prefix, UDP discovery datagrams do not.
 */
object KasaClient {
    private const val PORT = 9999
    private const val INITIAL_KEY = 0xAB

    fun encrypt(text: String): ByteArray {
        val input = text.toByteArray(Charsets.UTF_8)
        val out = ByteArray(input.size)
        var key = INITIAL_KEY
        for (i in input.indices) {
            val c = (input[i].toInt() xor key) and 0xFF
            out[i] = c.toByte()
            key = c
        }
        return out
    }

    fun decrypt(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): String {
        val out = ByteArray(length)
        var key = INITIAL_KEY
        for (i in 0 until length) {
            val enc = data[offset + i].toInt() and 0xFF
            out[i] = ((enc xor key) and 0xFF).toByte()
            key = enc
        }
        return String(out, Charsets.UTF_8)
    }

    private fun encryptWithHeader(text: String): ByteArray {
        val body = encrypt(text)
        val header = byteArrayOf(
            (body.size ushr 24 and 0xFF).toByte(),
            (body.size ushr 16 and 0xFF).toByte(),
            (body.size ushr 8 and 0xFF).toByte(),
            (body.size and 0xFF).toByte(),
        )
        return header + body
    }

    suspend fun send(ip: String, command: String, timeoutMs: Int = 4000): String =
        withContext(Dispatchers.IO) {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, PORT), timeoutMs)
                socket.soTimeout = timeoutMs
                DataOutputStream(socket.getOutputStream()).apply {
                    write(encryptWithHeader(command)); flush()
                }
                val input = DataInputStream(socket.getInputStream())
                val len = input.readInt()
                val body = ByteArray(len)
                input.readFully(body)
                decrypt(body)
            }
        }

    suspend fun setPower(ip: String, on: Boolean, isBulb: Boolean) {
        val cmd = if (isBulb) {
            """{"smartlife.iot.smartbulb.lightingservice":{"transition_light_state":{"on_off":${if (on) 1 else 0},"transition_period":0}}}"""
        } else {
            """{"system":{"set_relay_state":{"state":${if (on) 1 else 0}}}}"""
        }
        send(ip, cmd)
    }

    suspend fun discover(context: Context, timeoutMs: Int = 3000): List<KasaDevice> =
        withContext(Dispatchers.IO) {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val lock = wifi.createMulticastLock("kasa-discover").apply {
                setReferenceCounted(false); acquire()
            }
            val found = LinkedHashMap<String, KasaDevice>()
            try {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    socket.soTimeout = 300
                    val query = encrypt("""{"system":{"get_sysinfo":{}}}""")
                    for (dest in broadcastTargets()) {
                        runCatching {
                            socket.send(DatagramPacket(query, query.size, InetAddress.getByName(dest), PORT))
                        }
                    }
                    val deadline = System.currentTimeMillis() + timeoutMs
                    val buf = ByteArray(8192)
                    while (System.currentTimeMillis() < deadline) {
                        val packet = DatagramPacket(buf, buf.size)
                        try {
                            socket.receive(packet)
                            val json = decrypt(packet.data, packet.offset, packet.length)
                            val info = JSONObject(json)
                                .getJSONObject("system").getJSONObject("get_sysinfo")
                            val ip = packet.address?.hostAddress ?: continue
                            found[ip] = KasaDevice.from(ip, info)
                        } catch (_: SocketTimeoutException) {
                            // keep looping until the deadline
                        } catch (_: Exception) {
                            // ignore malformed replies
                        }
                    }
                }
            } finally {
                runCatching { lock.release() }
            }
            found.values.sortedBy { it.alias.lowercase() }
        }

    /** Directed broadcast for every up, non-loopback interface + global fallback. */
    private fun broadcastTargets(): List<String> {
        val targets = LinkedHashSet<String>()
        runCatching {
            for (nif in Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nif.isUp || nif.isLoopback) continue
                for (ia in nif.interfaceAddresses) {
                    ia.broadcast?.hostAddress?.let { targets.add(it) }
                }
            }
        }
        targets.add("255.255.255.255")
        return targets.toList()
    }
}
