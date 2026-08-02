# Kiosk setup (one-time, on the tablet)

The app runs full-screen and keeps the screen on without any of this. These steps add
**true kiosk lockdown** (Lock Task): the app is pinned, the system bars are gone, and it
can't be swiped away — the wall-panel appliance behavior.

There are two paths. **Device Owner** is the robust one.

---

## Path A — Device Owner + Lock Task (recommended)

Device Owner can only be set on an account with **no other accounts added**. Easiest on a
freshly reset tablet, or before you sign into a Google account.

1. On the tablet: **Settings → About → Build number**, tap 7× to enable Developer options.
   Then **Settings → System → Developer options → USB debugging = ON**.
2. Make sure **no Google/other accounts** are added (Settings → Accounts). Remove them if so.
3. Connect the tablet to your computer (with `adb` from the Android SDK platform-tools):
   ```sh
   adb devices                      # confirm the tablet shows up
   adb install app/build/outputs/apk/debug/app-debug.apk   # or use Android Studio Run
   adb shell dpm set-device-owner com.facts.homedashboard/.kiosk.KioskAdminReceiver
   ```
   Expect: `Success: Device owner set to package com.facts.homedashboard`.
4. Launch the app. It now enters Lock Task automatically (see `KioskManager`).

**To undo** (before uninstalling): `adb shell dpm remove-active-admin com.facts.homedashboard/.kiosk.KioskAdminReceiver`

---

## Path B — Home launcher (no adb, lighter lockdown)

If you don't want Device Owner, make the app the tablet's Home screen instead:

1. In `app/src/main/AndroidManifest.xml`, uncomment the `HOME` / `DEFAULT` intent-filter
   block on `MainActivity` (marked in a comment there) and rebuild.
2. On the tablet, press Home once and choose **Home Dashboard** → **Always**.

This makes the dashboard the default screen and it relaunches on boot, but the user can
still open the recents/notification shade. Good enough for a trusted household; Path A is
stronger.

---

## Notes
- `dpm set-device-owner` fails with "already provisioned" if accounts exist — reset or
  remove accounts first.
- Lock Task + `STAY_ON_WHILE_PLUGGED_IN` (set in `KioskManager`) keeps a mounted, powered
  tablet awake and pinned.
- For battery longevity on 24/7 charging, consider a smart plug to cycle charge later
  (tracked as a Phase-2 hardware note in the plan).
