# Adhan audio

The app plays a bundled adhan at each prayer time. The audio lives in
`android/app/src/main/res/raw/` and is **git-ignored** (recordings aren't
redistributed through this repo) — so fetch it once after cloning.

The player picks the file by prayer:

| Prayer            | File                          |
|-------------------|-------------------------------|
| Fajr (dawn)       | `res/raw/adhan_fajr.mp3`      |
| Dhuhr/Asr/Maghrib/Isha | `res/raw/adhan.mp3`     |

If a file is missing it falls back to the standard adhan, then to the device's
default alarm sound.

## Fetch the current recordings (Mishary Rashid Alafasy)

```sh
cd android/app/src/main/res/raw
# Standard adhan — Mishary Alafasy (aladhan CDN)
curl -L -o adhan.mp3 "https://cdn.aladhan.com/audio/adhans/a4.mp3"
# Fajr adhan — Mishary Alafasy, Maqam Hijaz (Internet Archive)
curl -L -o adhan_fajr.mp3 "https://archive.org/download/adhan-call-to-prayer-mishary-rashid-alafasy-fajr-maqam-hijaz-hd-320-kbps-1/Adhan%20%28Call%20to%20prayer%29%20_%20Mishary%20Rashid%20Alafasy%20_%20Fajr%20_%20Maqam%20Hijaz%20%E1%B4%B4%E1%B4%B0%20%28320%20kbps%29%20%281%29.mp3"
```

## Use your own reciter

Drop any `.mp3` in as `adhan.mp3` / `adhan_fajr.mp3` and rebuild. Filenames must be
lowercase (Android resource rule). Tap the prayer card on the dashboard to preview.

Sources:
- Standard adhan (Mishary Alafasy) — aladhan.com adhan downloads: https://aladhan.com/download-adhans
- Fajr adhan (Mishary Alafasy, Maqam Hijaz) — Internet Archive: https://archive.org/details/adhan-call-to-prayer-mishary-rashid-alafasy-fajr-maqam-hijaz-hd-320-kbps-1
