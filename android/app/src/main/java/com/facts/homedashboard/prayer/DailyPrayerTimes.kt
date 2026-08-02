package com.facts.homedashboard.prayer

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PrayerInstant(val name: PrayerName, val time: Instant)

/** The next upcoming prayer and how long until it. */
data class NextPrayer(val name: PrayerName, val time: Instant) {
    fun remaining(now: Instant): Duration = Duration.between(now, time)
}

/**
 * A day's computed times plus derived display helpers. Immutable — recompute
 * for a new date rather than mutating.
 */
data class DailyPrayerTimes(
    val date: LocalDate,
    val zone: ZoneId,
    val entries: List<PrayerInstant>,
    val qiblaDegrees: Double,
    val hijriLabel: String,
) {
    fun time(name: PrayerName): Instant =
        entries.first { it.name == name }.time

    /** e.g. "5:00 am" in the configured time zone. */
    fun localTime(name: PrayerName): String =
        TIME_FMT.withZone(zone).format(time(name)).lowercase(Locale.getDefault())

    companion object {
        private val TIME_FMT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    }
}
