package com.facts.homedashboard.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

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

    /**
     * Enter Lock Task (true kiosk) if we're Device Owner. Safe to call every
     * onResume — it's a no-op when already locked or not provisioned.
     */
    fun startLockTaskIfOwner(activity: Activity) {
        if (!isDeviceOwner(activity)) return
        val dpm = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(activity, KioskAdminReceiver::class.java)
        dpm.setLockTaskPackages(admin, arrayOf(activity.packageName))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Keep the screen from locking us out while pinned.
            dpm.setGlobalSetting(admin, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, "7")
        }
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
