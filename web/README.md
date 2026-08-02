# Companion Web App

Edit the household's calendar, lists, chores, reminders, and dashboard settings from
any phone or computer. Runs fully offline against `localStorage` today; the same code
swaps to Firebase (realtime cloud sync to the tablet) by changing one backend.

## Run it

```sh
cd web
npm run serve        # → http://localhost:5173  (no dependencies to install)
```

Open the URL on your phone (same network) or computer. Add events, lists, chores,
reminders; change prayer/general settings. Everything persists in the browser.

## Test it

```sh
npm test             # Node's built-in runner — 21 tests, no install needed
```

## Structure

```
web/
├── index.html         # shell + tabs
├── styles.css         # dark, mobile-first
├── server.mjs         # zero-dep static server for local preview
└── src/
    ├── contract.js    # SHARED data shapes + validation (mirror on Android)
    ├── store.js       # data layer; pluggable backend (Memory / Browser / Firestore-later)
    ├── settings.js    # dashboard + prayer config (mirrors Android PrayerSettings)
    └── app.js         # UI (thin layer over the tested store)
```

## What changes when the cloud is added

- Add a `FirestoreBackend` implementing `read/write` (see `store.js`) and swap it in.
- Add login (Firebase Auth) — the "Local preview" badge becomes the signed-in household.
- `app.js`, `contract.js`, and the tests stay as-is.

_The `contract.js` field names are the contract with the Android app — change them in
both places together._
