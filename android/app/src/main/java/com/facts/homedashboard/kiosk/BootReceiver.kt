package com.facts.homedashboard.kiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.facts.homedashboard.MainActivity
import com.facts.homedashboard.adhan.AdhanScheduler

/**
 * Relaunches the dashboard after the tablet reboots. On modern Android,
 * launching an Activity straight from a boot broadcast can be restricted;
 * the most reliable kiosk boots are Device Owner (Lock Task) or making the
 * app the HOME launcher (see docs/KIOSK_SETUP.md). We start the keep-alive
 * service first, then attempt the Activity.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                DashboardService.start(context)
                AdhanScheduler.scheduleNext(context)
                val launch = Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
            }
        }
    }
}
