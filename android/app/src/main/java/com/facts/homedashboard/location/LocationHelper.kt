package com.facts.homedashboard.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * Thin wrapper over the platform LocationManager (no Google Play dependency).
 * Returns a quick last-known fix if available, then a fresh single update.
 * City-level accuracy is plenty for prayer-time calculation.
 */
object LocationHelper {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun requestLocation(context: Context, onResult: (Double, Double) -> Unit) {
        if (!hasPermission(context)) return
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

        // Immediate: best last-known fix across providers.
        providers.mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.let { onResult(it.latitude, it.longitude) }

        // Fresh: one live update, then stop listening.
        val provider = providers.firstOrNull() ?: return
        lm.requestLocationUpdates(provider, 0L, 0f, object : LocationListener {
            override fun onLocationChanged(location: Location) {
                onResult(location.latitude, location.longitude)
                lm.removeUpdates(this)
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("Required by interface")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }, Looper.getMainLooper())
    }
}
