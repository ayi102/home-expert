package com.facts.homedashboard.prayer

import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.Qibla
import com.batoulapps.adhan.data.DateComponents
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Offline prayer-time calculation. Wraps the batoulapps `adhan` library (same
 * algorithm as the JS reference we validated) and adds our display model:
 * next-prayer lookup, Qibla, and the Umm al-Qura Hijri date.
 */
object PrayerTimesEngine {

    fun compute(settings: PrayerSettings, date: LocalDate): DailyPrayerTimes {
        val coordinates = Coordinates(settings.latitude, settings.longitude)
        val components = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val params = settings.method.adhan.parameters.apply {
            madhab = settings.madhab.adhan
        }
        val pt = PrayerTimes(coordinates, components, params)

        val entries = listOf(
            PrayerInstant(PrayerName.FAJR, pt.fajr.toInstant()),
            PrayerInstant(PrayerName.SUNRISE, pt.sunrise.toInstant()),
            PrayerInstant(PrayerName.DHUHR, pt.dhuhr.toInstant()),
            PrayerInstant(PrayerName.ASR, pt.asr.toInstant()),
            PrayerInstant(PrayerName.MAGHRIB, pt.maghrib.toInstant()),
            PrayerInstant(PrayerName.ISHA, pt.isha.toInstant()),
        )

        return DailyPrayerTimes(
            date = date,
            zone = ZoneId.of(settings.timeZoneId),
            entries = entries,
            qiblaDegrees = Qibla(coordinates).direction,
            hijriLabel = hijriLabel(date),
        )
    }

    /**
     * The next actual prayer (Sunrise excluded) after [now]. Rolls to
     * tomorrow's Fajr once today's Isha has passed.
     */
    fun nextPrayer(settings: PrayerSettings, now: Instant): NextPrayer {
        val zone = ZoneId.of(settings.timeZoneId)
        val today = compute(settings, now.atZone(zone).toLocalDate())
        val upcoming = today.entries
            .filter { it.name.isPrayer && it.time.isAfter(now) }
            .minByOrNull { it.time }
        if (upcoming != null) return NextPrayer(upcoming.name, upcoming.time)

        val tomorrow = compute(settings, now.atZone(zone).toLocalDate().plusDays(1))
        val fajr = tomorrow.entries.first { it.name == PrayerName.FAJR }
        return NextPrayer(PrayerName.FAJR, fajr.time)
    }

    /** Umm al-Qura Hijri date, e.g. "16 Safar 1448". */
    fun hijriLabel(date: LocalDate): String =
        HijrahDate.from(date).format(HIJRI_FMT)

    private val HIJRI_FMT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
}
