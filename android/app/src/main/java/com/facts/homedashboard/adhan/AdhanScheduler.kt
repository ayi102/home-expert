package com.facts.homedashboard.adhan

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.facts.homedashboard.prayer.PrayerTimesEngine
import com.facts.homedashboard.prayer.SettingsStore
import java.time.Instant

/**
 * Schedules an exact alarm for the next prayer. When it fires, [AdhanReceiver]
 * plays the adhan and calls back here to schedule the following one — a
 * self-perpetuating chain that keeps working with the screen off or the app
 * backgrounded. Re-armed on boot and whenever the location updates.
 */
object AdhanScheduler {
    private const val REQUEST = 1001

    fun scheduleNext(context: Context) {
        val settings = SettingsStore.load(context)
        val next = PrayerTimesEngine.nextPrayer(settings, Instant.now())
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AdhanReceiver::class.java)
            .putExtra(EXTRA_PRAYER, next.name.name)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val triggerAt = next.time.toEpochMilli()
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    const val EXTRA_PRAYER = "prayer"
}
