package com.facts.homedashboard.kiosk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.facts.homedashboard.R

/**
 * Lightweight always-on foreground service. Its job today is simply to keep the
 * process resident (so Android is less likely to kill the dashboard) and to
 * hold a persistent notification. It is the hook where the watchdog will live:
 * a periodic check that MainActivity is still in the foreground, re-launching
 * it if the page/UI ever dies. Watchdog logic is intentionally a stub for now.
 */
class DashboardService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        // TODO(watchdog): schedule a periodic self-check + relaunch of MainActivity.
        return START_STICKY
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_running_title))
            .setContentText(getString(R.string.service_running_text))
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    companion object {
        private const val CHANNEL_ID = "dashboard_keepalive"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            ensureChannel(context)
            ContextCompat.startForegroundService(
                context, Intent(context, DashboardService::class.java)
            )
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.service_channel_name),
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }
}
