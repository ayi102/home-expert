package com.facts.homedashboard.kasa

import android.content.Context

/**
 * Persists which floor/group each Kasa device belongs to, keyed by the stable
 * deviceId. Absent = unassigned. Shared by both tablets later via the cloud;
 * local SharedPreferences for now.
 */
object LightGroups {
    const val UPSTAIRS = "Upstairs"
    const val DOWNSTAIRS = "Downstairs"
    val GROUPS = listOf(UPSTAIRS, DOWNSTAIRS)

    private const val PREFS = "light_groups"

    fun groupOf(context: Context, deviceId: String): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(deviceId, null)

    fun assign(context: Context, deviceId: String, group: String?) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (group == null) editor.remove(deviceId) else editor.putString(deviceId, group)
        editor.commit() // synchronous — guarantees it's written before we return
    }
}
