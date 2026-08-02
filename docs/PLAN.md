# Home Dashboard — Native Android App + Remote Companion for the TECLAST T65

## Context
The user bought a **TECLAST T65** (13.4", Android 16, octa-core 2.2GHz, 20GB RAM / 128GB,
8000mAh, dual-band Wi-Fi + 4G LTE + GPS, IPS LCD) to run as a **wall-mounted, always-on custom
home dashboard**. Target capabilities: **(1) smart-home control** (TP-Link Kasa lights/switches),
**(2) a family dashboard**, **(3) call to prayer** (prayer times + adhan), and
**(4) remote management** — add/edit calendar events, lists, chores, and reminders, and
configure the dashboard, **from the tablet or from a companion web page, anywhere.**

The user is comfortable with Android/Kotlin and wants to own the result. Claude builds the
project; the user compiles/runs on the tablet and we iterate. On-device-only steps (kiosk
provisioning) are documented commands the user runs once.

### Key design decisions
- **Native Kotlin app** on the tablet (cleanest: direct adhan audio, GPS/compass, raw sockets
  for Kasa, self-owned kiosk — no Fully Kiosk, no Termux). Panel is **IPS LCD**, so 24/7 static
  content is low burn-in risk.
- **Alexa is out of scope.** All the user's switches/lights are TP-Link Kasa, controlled
  directly by the app. Alexa (which has no supported API) is not integrated; it can remain a
  parallel voice remote for the same devices if the user wishes.
- **"From anywhere" = a small cloud layer** (hosted service, *not* home hardware; free tier,
  login-protected). Both the tablet and a companion web page read/write it and sync live.
- **Calendar:** the wall *displays* all subscribed calendars (Google Home + any iCloud/Outlook
  ICS feeds), read-only for those. New events created from the tablet or web page are written to
  one shared **Google "Home" calendar** (best write API, cross-platform for the family).
- **Lists / chores / reminders / dashboard config / prayer & light settings:** live in our cloud
  DB (Firebase Firestore or Supabase), fully read-write from both the tablet and the web page,
  realtime-synced.
- **Two tablets (Upstairs + Downstairs), auto-synced.** Both are clients of the same cloud data,
  so they mirror each other for free — no extra architecture. Config splits into **household
  (shared: calendar, lists, chores, reminders, prayer times, lights)** and **per-device
  (its label, brightness/dim schedule, whether that tablet sounds the adhan)**. Each tablet is
  provisioned + named once. Firebase free tier covers both at $0.

## Architecture
**Tablet app (Kotlin, Jetpack Compose, single-Activity):**
- **Kiosk / always-on:** Lock Task Mode (provision app as Device Owner once via adb
  `dpm set-device-owner`, then `startLockTask()`), immersive full-screen, `FLAG_KEEP_SCREEN_ON`;
  `BOOT_COMPLETED` receiver + foreground service for auto-relaunch; watchdog + optional nightly
  self-restart. Fallback: set app as Home launcher if the user prefers no provisioning.
- **Prayer engine:** adhan calculation (methods + Shafi/Hanafi madhab + high-latitude rules)
  from GPS, fully offline; Qibla bearing; Hijri date via `android.icu` `IslamicCalendar`
  (umalqura). `AlarmManager` exact alarms fire the adhan (`MediaPlayer`) even from a slept screen.
- **Kasa control:** in-app client for the Kasa **local LAN protocol** (TCP :9999, XOR autokey) —
  discover + on/off/brightness/state. Add the **KLAP** handshake path if bulbs are newer models.
- **Data:** reads calendars (Google Calendar API + subscribed ICS feeds); reads/writes the cloud
  DB for lists/chores/reminders/config with realtime listeners; local Room cache for offline
  resilience; photo slideshow.

**Cloud layer:**
- **DB + auth + hosting:** Firebase (Firestore + Auth + Hosting) or Supabase — free tier. Holds
  lists, chores, reminders, dashboard config, prayer/light settings; realtime sync to the tablet.
  Login-protected (household account) since it's internet-reachable.
- **Google Calendar:** OAuth to a shared Home calendar for creating/editing events; ICS
  subscription URLs for read-only display of other calendars.

**Companion web page (edit from anywhere):**
- A responsive web app (open on any phone/computer, nothing to install), served from cloud
  Hosting, login-protected. Add/edit calendar events (→ Google Home calendar), lists, chores,
  reminders; configure dashboard tiles, prayer method/madhab/adhan toggles, and light
  labels/scenes. Changes appear on the wall in seconds via realtime sync.

## Initial capability set (Phase 1 — build now)
1. **App skeleton + kiosk foundation:** Compose single-Activity, immersive full-screen, keep
   screen on, Lock Task Mode, auto-launch on boot, watchdog, dark theme.
2. **Cloud layer + auth:** Firebase/Supabase project, household login, data model for
   lists/chores/reminders/config, realtime sync wired into the tablet app.
3. **Companion web page:** login; add/edit events, lists, chores, reminders; edit dashboard &
   prayer/light settings.
4. **Family dashboard tiles:** clock + date, weather + forecast, aggregated calendar agenda
   (read all feeds), chores/shopping list, reminders, rotating photo background — with
   **add/edit directly on the tablet** too.
5. **Call to prayer:** five daily times, next-prayer countdown, Hijri date, live Qibla compass
   (fixed bearing fallback if no magnetometer), adhan audio via exact alarms. Settings editable
   on tablet and web page.
6. **Kasa light control:** discover lights; per-light on/off + brightness with live state.

## Deferred (Phase 2 — candidates, confirm scope)
- Voice/announcements (e.g., trash-day, prayer announcements); **push notifications to phones**
  (e.g., doorbell); per-person family logins/profiles; cameras/doorbell; music control.
- (ecobee thermostat and Alexa intentionally excluded — all switches are Kasa, controlled
  directly; ecobee dropped per user.)

## Things to confirm on the device (non-blocking)
- Magnetometer present? (live Qibla compass vs fixed bearing)
- Exact Kasa bulb model (classic vs KLAP protocol)
- adb/USB debugging access for one-time Device Owner provisioning
- Adhan speaker loudness (3.5mm/Bluetooth speaker if needed); 24/7 charging battery care

## Where the code lives
Two projects in a dedicated folder (proposed `~/AndroidStudioProjects/HomeDashboard/`): the
Android app and the companion web app. Confirm or redirect before scaffolding (current working
folder is unrelated home-warranty files).

## Verification (on-device, run by the user)
- Build & install → app launches full-screen, no status/nav bars, screen stays on; reboot →
  auto-relaunches.
- Add an event/reminder/chore on the **companion web page** from a phone → appears on the wall
  within seconds; add one **on the tablet** → appears on the web page.
- New event created anywhere lands in the Google Home calendar and shows in the aggregated agenda.
- Prayer time reached → adhan plays from a slept screen; countdown advances; Hijri + Qibla render.
- Tap a light tile → Kasa bulb responds in ~1s; change it from the Kasa app → dashboard updates.
- Airplane-mode test → tablet keeps showing cached data and recovers when back online.
