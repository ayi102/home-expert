package com.facts.homedashboard.prayer

import android.content.Context
import java.time.ZoneId

/**
 * Persists the prayer settings (esp. the auto-detected location) so background
 * components — the adhan alarm scheduler and boot receiver — can compute times
 * without the UI being open.
 */
object SettingsStore {
    private const val PREFS = "prayer_settings"

    fun save(context: Context, settings: PrayerSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("lat", settings.latitude.toString())
            .putString("lng", settings.longitude.toString())
            .putString("tz", settings.timeZoneId)
            .putString("method", settings.method.name)
            .putString("madhab", settings.madhab.name)
            .apply()
    }

    fun load(context: Context): PrayerSettings {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val base = PrayerSettings.PLACEHOLDER.copy(timeZoneId = ZoneId.systemDefault().id)
        val lat = p.getString("lat", null)?.toDoubleOrNull() ?: return base
        val lng = p.getString("lng", null)?.toDoubleOrNull() ?: return base
        val method = p.getString("method", null)
            ?.let { runCatching { CalcMethod.valueOf(it) }.getOrNull() } ?: base.method
        val madhab = p.getString("madhab", null)
            ?.let { runCatching { AsrMadhab.valueOf(it) }.getOrNull() } ?: base.madhab
        return base.copy(
            latitude = lat,
            longitude = lng,
            timeZoneId = p.getString("tz", null) ?: base.timeZoneId,
            method = method,
            madhab = madhab,
        )
    }
}
