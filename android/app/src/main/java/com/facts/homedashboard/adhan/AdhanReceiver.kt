package com.facts.homedashboard.adhan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fires at each prayer time: plays the adhan, then schedules the next one.
 * (Per-prayer on/off toggles from household settings will gate this later.)
 */
class AdhanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isFajr = intent.getStringExtra(AdhanScheduler.EXTRA_PRAYER) == "FAJR"
        AdhanPlayer.play(context, isFajr)
        AdhanScheduler.scheduleNext(context)
    }
}
