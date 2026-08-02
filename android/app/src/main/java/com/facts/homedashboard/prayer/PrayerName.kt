package com.facts.homedashboard.prayer

/** The six daily markers we display. Sunrise isn't a prayer but bounds Fajr. */
enum class PrayerName(val display: String, val isPrayer: Boolean) {
    FAJR("Fajr", true),
    SUNRISE("Sunrise", false),
    DHUHR("Dhuhr", true),
    ASR("Asr", true),
    MAGHRIB("Maghrib", true),
    ISHA("Isha", true),
}
