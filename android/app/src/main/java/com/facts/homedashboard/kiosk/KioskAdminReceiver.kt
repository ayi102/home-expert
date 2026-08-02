package com.facts.homedashboard.kiosk

import android.app.admin.DeviceAdminReceiver

/**
 * Marker admin component required for Device Owner + Lock Task. The base class
 * behavior is all we need; provisioning happens once via adb.
 */
class KioskAdminReceiver : DeviceAdminReceiver()
