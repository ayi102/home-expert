package com.facts.homedashboard.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Contract test for the prayer engine. Expected values were generated and
 * cross-checked with the canonical `adhan` JS library (same author/algorithm)
 * for Raleigh, NC (35.7796, -78.6382) on 2026-07-30. Times asserted in UTC to
 * stay time-zone independent.
 *
 * Reference (UTC):
 *   ISNA/Shafi   fajr 09:00 sunrise 10:21 dhuhr 17:22 asr 21:08 maghrib 00:21 isha 01:41
 *   ISNA/Hanafi  asr 22:17 (later, as expected)
 *   MWL/Shafi    fajr 08:41 isha 01:54
 *   Qibla 55.82°, Hijri "Safar 1448"
 */
class PrayerTimesEngineTest {

    private val raleigh = PrayerSettings(
        latitude = 35.7796,
        longitude = -78.6382,
        timeZoneId = "America/New_York",
    )
    private val date = LocalDate.of(2026, 7, 30)
    private val utc = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC)

    private fun utcOf(t: DailyPrayerTimes, name: PrayerName) = utc.format(t.time(name))

    @Test
    fun isnaShafiMatchesReference() {
        val t = PrayerTimesEngine.compute(raleigh.copy(method = CalcMethod.NORTH_AMERICA), date)
        assertEquals("09:00", utcOf(t, PrayerName.FAJR))
        assertEquals("10:21", utcOf(t, PrayerName.SUNRISE))
        assertEquals("17:22", utcOf(t, PrayerName.DHUHR))
        assertEquals("21:08", utcOf(t, PrayerName.ASR))
        assertEquals("00:21", utcOf(t, PrayerName.MAGHRIB))
        assertEquals("01:41", utcOf(t, PrayerName.ISHA))
    }

    @Test
    fun hanafiAsrIsLaterThanShafi() {
        val shafi = PrayerTimesEngine.compute(raleigh.copy(madhab = AsrMadhab.SHAFI), date)
        val hanafi = PrayerTimesEngine.compute(raleigh.copy(madhab = AsrMadhab.HANAFI), date)
        assertEquals("22:17", utcOf(hanafi, PrayerName.ASR))
        assertTrue(
            "Hanafi Asr must be later than Shafi",
            hanafi.time(PrayerName.ASR).isAfter(shafi.time(PrayerName.ASR)),
        )
    }

    @Test
    fun muslimWorldLeagueShiftsFajrAndIsha() {
        val mwl = PrayerTimesEngine.compute(raleigh.copy(method = CalcMethod.MUSLIM_WORLD_LEAGUE), date)
        assertEquals("08:41", utcOf(mwl, PrayerName.FAJR))
        assertEquals("01:54", utcOf(mwl, PrayerName.ISHA))
    }

    @Test
    fun prayersAreInChronologicalOrder() {
        val t = PrayerTimesEngine.compute(raleigh, date)
        val ordered = listOf(
            PrayerName.FAJR, PrayerName.SUNRISE, PrayerName.DHUHR,
            PrayerName.ASR, PrayerName.MAGHRIB, PrayerName.ISHA,
        )
        for (i in 1 until ordered.size) {
            assertTrue(
                "${ordered[i]} should be after ${ordered[i - 1]}",
                t.time(ordered[i]).isAfter(t.time(ordered[i - 1])),
            )
        }
    }

    @Test
    fun qiblaFromUsEastCoastPointsNortheast() {
        val t = PrayerTimesEngine.compute(raleigh, date)
        assertEquals(55.82, t.qiblaDegrees, 0.5)
    }

    @Test
    fun hijriDateIsUmmAlQura() {
        val t = PrayerTimesEngine.compute(raleigh, date)
        assertTrue("Hijri label was ${t.hijriLabel}", t.hijriLabel.contains("Safar"))
        assertTrue("Hijri label was ${t.hijriLabel}", t.hijriLabel.contains("1448"))
    }

    @Test
    fun nextPrayerRollsToTomorrowAfterIsha() {
        val t = PrayerTimesEngine.compute(raleigh, date)
        // One minute after today's Isha → next actual prayer is tomorrow's Fajr.
        val afterIsha = t.time(PrayerName.ISHA).plusSeconds(60)
        val next = PrayerTimesEngine.nextPrayer(raleigh, afterIsha)
        assertEquals(PrayerName.FAJR, next.name)
        assertTrue("next Fajr must be after Isha", next.time.isAfter(t.time(PrayerName.ISHA)))
    }

    @Test
    fun nextPrayerMidMorningIsDhuhr() {
        val t = PrayerTimesEngine.compute(raleigh, date)
        // Just after sunrise → next actual prayer should be Dhuhr (Sunrise skipped).
        val afterSunrise: Instant = t.time(PrayerName.SUNRISE).plusSeconds(60)
        val next = PrayerTimesEngine.nextPrayer(raleigh, afterSunrise)
        assertEquals(PrayerName.DHUHR, next.name)
    }
}
