package com.facts.homedashboard.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.facts.homedashboard.MainActivity

/**
 * Central place for the "make this behave like an appliance" behavior:
 * full-screen immersive, keep the screen on, and (when provisioned as Device
 * Owner) pin the app with Lock Task so it can't be swiped away.
 *
 * None of this requires Fully Kiosk or any third-party shell — it's all
 * first-party Android. Lock Task only engages if the app has been made Device
 * Owner via adb (see docs/KIOSK_SETUP.md); otherwise the app still runs full
 * screen and simply skips pinning.
 */
object KioskManager {

    fun keepScreenOn(activity: Activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /** Hide the status and navigation bars; let a swipe reveal them transiently. */
    fun applyImmersive(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    /** Media apps allowed to run inside the kiosk (Lock Task) alongside us. */
    private val LOCK_TASK_ALLOWED = arrayOf(
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
        "com.google.android.apps.youtube.kids",
        "com.netflix.mediaclient",
    )

    /**
     * Enter Lock Task (true kiosk) if we're Device Owner. Allow-lists YouTube/
     * Netflix so they can launch from the panel, enables a Home affordance, and
     * makes this app the default Home so Home returns to the dashboard. Safe to
     * call every onResume — a no-op when already locked or not provisioned.
     */
    fun startLockTaskIfOwner(activity: Activity) {
        if (!isDeviceOwner(activity)) return
        val dpm = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(activity, KioskAdminReceiver::class.java)

        dpm.setLockTaskPackages(admin, arrayOf(activity.packageName) + LOCK_TASK_ALLOWED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Keep a Home button + power menu available while pinned.
            dpm.setLockTaskFeatures(
                admin,
                DevicePolicyManager.LOCK_TASK_FEATURE_HOME or
                    DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS,
            )
        }

        // Make the dashboard the default Home so Home returns here from media apps.
        val homeFilter = IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        dpm.addPersistentPreferredActivity(
            admin, homeFilter, ComponentName(activity, MainActivity::class.java),
        )

        // Keep a powered, mounted tablet awake.
        dpm.setGlobalSetting(admin, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, "7")

        if (!isInLockTask(activity)) {
            activity.startLockTask()
        }
    }

    private fun isInLockTask(activity: Activity): Boolean {
        val am = activity.getSystemService(Context.ACTIVITY_SERVICE)
                as android.app.ActivityManager
        return am.lockTaskModeState != android.app.ActivityManager.LOCK_TASK_MODE_NONE
    }
}
