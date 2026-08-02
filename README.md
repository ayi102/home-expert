# Home Dashboard

A wall-mounted, always-on home dashboard for the **TECLAST T65** (13.4", Android 16),
plus a companion web page for editing from anywhere.

See the full design in the plan: `~/.claude/plans/ok-i-have-a-vectorized-fog.md`.

## Repo layout

```
HomeDashboard/
├── android/      # the native Kotlin tablet app (open THIS folder in Android Studio)
│   ├── app/
│   ├── docs/KIOSK_SETUP.md
│   └── ...
└── web/          # companion web app (added in a later phase)
```

## Capabilities (Phase 1)

1. **Kiosk foundation** — full-screen, always-on, self-relaunching wall panel. ← _built_
2. Cloud layer + auth (Firebase/Supabase) with realtime sync.
3. Companion web page to edit from any phone/computer.
4. Family dashboard tiles (clock, weather, calendar, chores, lists, reminders, photos).
5. Call to prayer (offline times, countdown, Hijri date, Qibla, adhan audio).
6. Kasa light/switch control (direct over Wi-Fi).

_Smart-home is Kasa-only, controlled directly — no Alexa, no hub. ecobee is out of scope._

## Build & run

1. Install **Android Studio** (latest stable) if you haven't.
2. `File → Open` → select the **`android/`** folder (not the repo root).
3. On first sync, Android Studio downloads Gradle 8.11.1 and regenerates the Gradle
   wrapper automatically. If prompted about a missing wrapper, accept.
   (Alternatively, from `android/` with a system Gradle: `gradle wrapper --gradle-version 8.11.1`.)
4. Enable **Developer options + USB debugging** on the T65, connect it, and press **Run ▶**.
   Or from the terminal in `android/`: `./gradlew installDebug`.

You should see a dark, full-screen dashboard: a live clock/date header and a grid of
"Coming soon" tiles. It stays on and hides the system bars.

## Make it a true kiosk

The app runs full-screen out of the box, but to *lock* it (no swiping out, survives
reboots as an appliance) provision it as **Device Owner** once — see
[`android/docs/KIOSK_SETUP.md`](android/docs/KIOSK_SETUP.md).
