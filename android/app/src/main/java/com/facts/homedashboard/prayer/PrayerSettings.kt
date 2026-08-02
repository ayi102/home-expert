package com.facts.homedashboard.prayer

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Madhab

/** Calculation conventions the user can pick in settings (a curated subset). */
enum class CalcMethod(val display: String, val adhan: CalculationMethod) {
    NORTH_AMERICA("ISNA (North America)", CalculationMethod.NORTH_AMERICA),
    MUSLIM_WORLD_LEAGUE("Muslim World League", CalculationMethod.MUSLIM_WORLD_LEAGUE),
    EGYPTIAN("Egyptian General Authority", CalculationMethod.EGYPTIAN),
    UMM_AL_QURA("Umm al-Qura (Makkah)", CalculationMethod.UMM_AL_QURA),
    KARACHI("University of Karachi", CalculationMethod.KARACHI),
    DUBAI("Dubai", CalculationMethod.DUBAI),
    QATAR("Qatar", CalculationMethod.QATAR),
    KUWAIT("Kuwait", CalculationMethod.KUWAIT),
    SINGAPORE("Singapore", CalculationMethod.SINGAPORE),
    MOONSIGHTING("Moonsighting Committee", CalculationMethod.MOON_SIGHTING_COMMITTEE),
}

enum class AsrMadhab(val display: String, val adhan: Madhab) {
    SHAFI("Shafi / Maliki / Hanbali (earlier Asr)", Madhab.SHAFI),
    HANAFI("Hanafi (later Asr)", Madhab.HANAFI),
}

/**
 * All inputs the prayer engine needs. Location defaults to a PLACEHOLDER and is
 * replaced by device GPS (and editable in the companion web page) in a later phase.
 */
data class PrayerSettings(
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val method: CalcMethod = CalcMethod.NORTH_AMERICA,
    val madhab: AsrMadhab = AsrMadhab.SHAFI,
    val adhanEnabled: Map<PrayerName, Boolean> = defaultAdhanToggles(),
) {
    companion object {
        /** PLACEHOLDER location until GPS/settings provide the real one. */
        val PLACEHOLDER = PrayerSettings(
            latitude = 35.7796,
            longitude = -78.6382,
            timeZoneId = "America/New_York",
        )

        fun defaultAdhanToggles(): Map<PrayerName, Boolean> =
            PrayerName.entries.associateWith { it.isPrayer }
    }
}
