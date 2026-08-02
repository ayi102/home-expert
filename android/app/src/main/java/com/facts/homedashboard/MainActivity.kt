package com.facts.homedashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.facts.homedashboard.adhan.AdhanScheduler
import com.facts.homedashboard.kiosk.DashboardService
import com.facts.homedashboard.kiosk.KioskManager
import com.facts.homedashboard.kiosk.KioskPrefs
import com.facts.homedashboard.ui.DashboardScreen
import com.facts.homedashboard.ui.theme.HomeDashboardTheme

/**
 * Single-Activity entry point. Sets up the appliance behavior (immersive,
 * keep-awake, optional Lock Task) and hosts the Compose dashboard.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        KioskManager.keepScreenOn(this)
        KioskManager.applyImmersive(this)
        if (KioskPrefs.isEnabled(this)) {
            KioskManager.startLockTaskIfOwner(this)
        } else {
            KioskManager.exitKiosk(this)
        }
        DashboardService.start(this)
        AdhanScheduler.scheduleNext(this)

        setContent {
            HomeDashboardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        KioskManager.applyImmersive(this)
        if (KioskPrefs.isEnabled(this)) {
            KioskManager.startLockTaskIfOwner(this)
        }
    }
}
