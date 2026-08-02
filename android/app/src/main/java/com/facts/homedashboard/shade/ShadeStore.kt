package com.facts.homedashboard.shade

import android.content.Context

/** Remembers the shade gateway's last-known address so we skip re-discovery. */
object ShadeStore {
    private const val PREFS = "shade"

    fun save(context: Context, host: String, port: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("host", host).putInt("port", port).apply()
    }

    fun load(context: Context): Pair<String, Int>? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val host = p.getString("host", null) ?: return null
        return host to p.getInt("port", 10123)
    }
}
