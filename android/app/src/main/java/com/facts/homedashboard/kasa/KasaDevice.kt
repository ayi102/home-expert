package com.facts.homedashboard.kasa

import org.json.JSONObject

/** A discovered Kasa device and its current state. */
data class KasaDevice(
    val ip: String,
    val deviceId: String,
    val alias: String,
    val model: String,
    val isBulb: Boolean,
    val isOn: Boolean,
) {
    companion object {
        fun from(ip: String, info: JSONObject): KasaDevice {
            val type = info.optString("mic_type", info.optString("type", "")).uppercase()
            val isBulb = type.contains("SMARTBULB") ||
                info.has("light_state") ||
                info.optInt("is_dimmable", 0) == 1
            val on = if (isBulb) {
                info.optJSONObject("light_state")?.optInt("on_off", 0) == 1
            } else {
                info.optInt("relay_state", 0) == 1
            }
            return KasaDevice(
                ip = ip,
                // Stable per-device id (survives DHCP changes); fall back to IP.
                deviceId = info.optString("deviceId", "").ifBlank { ip },
                alias = info.optString("alias", "(unnamed)"),
                model = info.optString("model", "?"),
                isBulb = isBulb,
                isOn = on,
            )
        }
    }
}
