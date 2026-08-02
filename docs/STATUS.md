# Status & Next Steps

Snapshot of where the Home Dashboard build stands, so anyone (or any machine) can pick it up.

## What works today

- **`android/`** — native Kotlin/Compose wall-tablet app: locked-down always-on kiosk
  (Lock Task, boot autostart, foreground keep-alive) + a dark dashboard with a live clock and
  a **call-to-prayer** panel (offline times, next-prayer countdown, Qibla, Umm al-Qura Hijri).
  Prayer correctness is pinned by JUnit tests whose values were cross-checked against the
  canonical `adhan` reference (`web/scripts/verify_prayer.mjs`).
  _Builds/runs on a real device via Android Studio — not yet verified on hardware._
- **`web/`** — working companion app (calendar, lists, chores, reminders, settings) over a
  pluggable data layer. **25 passing tests**, zero-dependency preview server.
- Config is split **household (shared)** vs **per-device** to support two synced tablets
  (Upstairs + Downstairs).

## Working on a fresh machine (e.g. laptop)

Clone, then install the toolchains (the repo intentionally does NOT vendor these):

- **Web app** — needs [Node](https://nodejs.org) (18+):
  ```sh
  cd web && npm test          # run tests (no install needed)
  npm run serve               # preview at http://localhost:5173
  npm install && npm run verify:prayer   # optional: re-verify prayer vectors (installs adhan)
  ```
- **Android app** — needs [Android Studio](https://developer.android.com/studio) (latest) + JDK 17:
  - `File → Open` the **`android/`** folder. First sync downloads Gradle and **regenerates the
    Gradle wrapper** (the wrapper jar is intentionally not committed).
  - Run on a device/emulator. Kiosk lockdown steps: `android/docs/KIOSK_SETUP.md`.

## Not in the repo by design

- **No secrets/keys** — `google-services.json`, API keys, `.env` are git-ignored. Add your own.
- **No `node_modules` / Gradle build output** — regenerated locally.

## Next steps

1. **Kasa control client** (Kotlin) — local LAN protocol; crypto verifiable in Node before hardware.
2. **Needs your accounts** (one setup session): Firebase project (Firestore + Auth + Hosting) →
   swap the web `Store` backend + wire the Android app; weather API key; Google Calendar OAuth
   for the shared "Home" calendar.
3. **On the tablets (x2, arriving ~2026-07-30):** install + provision kiosk, name each
   (Upstairs/Downstairs), confirm adhan audio + GPS/compass, and Kasa on the real Wi-Fi.

See `docs/PLAN.md` for the full design.
