package com.facts.homedashboard.util

import android.content.Context
import android.content.Intent
import android.widget.Toast

/** Launches other installed apps (YouTube, Netflix, …) from the dashboard. */
object AppLauncher {

    /** Returns true if launched; false if the app isn't installed. */
    fun launch(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    /** Launch, or toast a friendly message if the app isn't installed yet. */
    fun launchOrNotify(context: Context, packageName: String, label: String) {
        if (!launch(context, packageName)) {
            Toast.makeText(context, "$label isn't installed yet", Toast.LENGTH_SHORT).show()
        }
    }
}
