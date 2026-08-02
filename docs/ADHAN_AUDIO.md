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

## Fetch the current recordings

```sh
cd android/app/src/main/res/raw
curl -L -o adhan.mp3       "https://cdn.aladhan.com/audio/adhans/a1.mp3"                # Ahmad al-Nafees (aladhan CDN)
curl -L -o adhan_fajr.mp3  "https://archive.org/download/AzanathanAlFajr/Azanfajr.mp3"  # Fajr adhan (Internet Archive)
```

## Use your own reciter

Drop any `.mp3` in as `adhan.mp3` / `adhan_fajr.mp3` and rebuild. Filenames must be
lowercase (Android resource rule). Tap the prayer card on the dashboard to preview.

Sources:
- Standard adhan — aladhan.com adhan downloads: https://aladhan.com/download-adhans
- Fajr adhan — Internet Archive: https://archive.org/details/AzanathanAlFajr
