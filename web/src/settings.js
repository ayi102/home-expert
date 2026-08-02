// Dashboard configuration, split into two scopes for a multi-tablet home:
//
//   HOUSEHOLD (shared)  — one doc, synced to every tablet + the web page:
//       prayer calc/location, which prayers sound the adhan, general/photos.
//   DEVICE (per-tablet) — one doc per tablet (keyed by a stable deviceId):
//       its label (Upstairs/Downstairs), brightness/dim schedule, and whether
//       THIS tablet plays the adhan out loud.
//
// Both mirror the Android side so the tablets and the companion agree.

export const CALC_METHODS = [
  ['NORTH_AMERICA', 'ISNA (North America)'],
  ['MUSLIM_WORLD_LEAGUE', 'Muslim World League'],
  ['EGYPTIAN', 'Egyptian General Authority'],
  ['UMM_AL_QURA', 'Umm al-Qura (Makkah)'],
  ['KARACHI', 'University of Karachi'],
  ['DUBAI', 'Dubai'],
  ['QATAR', 'Qatar'],
  ['KUWAIT', 'Kuwait'],
  ['SINGAPORE', 'Singapore'],
  ['MOONSIGHTING', 'Moonsighting Committee'],
];

export const MADHABS = [
  ['SHAFI', 'Shafi / Maliki / Hanbali (earlier Asr)'],
  ['HANAFI', 'Hanafi (later Asr)'],
];

// ---- Household (shared across all tablets) --------------------------------
export function defaultHousehold() {
  return {
    prayer: {
      method: 'NORTH_AMERICA',
      madhab: 'SHAFI',
      // PLACEHOLDER location until a tablet's GPS provides the real one.
      latitude: 35.7796,
      longitude: -78.6382,
      timeZoneId: 'America/New_York',
      // Which prayers have an adhan at all (household policy).
      adhanEnabled: { FAJR: true, DHUHR: true, ASR: true, MAGHRIB: true, ISHA: true },
    },
    general: {
      householdName: 'Home',
      photoFolderUrl: '',
    },
  };
}

export function mergeHousehold(stored) {
  const base = defaultHousehold();
  if (!stored || typeof stored !== 'object') return base;
  const p = stored.prayer ?? {};
  return {
    prayer: {
      ...base.prayer, ...p,
      adhanEnabled: { ...base.prayer.adhanEnabled, ...(p.adhanEnabled ?? {}) },
    },
    general: { ...base.general, ...(stored.general ?? {}) },
  };
}

// ---- Device (per-tablet) ---------------------------------------------------
export function defaultDevice(label = 'Tablet') {
  return {
    label,
    playAdhan: true,        // does THIS tablet sound the adhan out loud
    brightness: 0.85,       // 0..1 daytime brightness
    dimStart: '22:00',      // local HH:mm to dim the screen
    dimEnd: '06:00',        // local HH:mm to brighten again
  };
}

export function mergeDevice(stored, fallbackLabel = 'Tablet') {
  const base = defaultDevice(fallbackLabel);
  if (!stored || typeof stored !== 'object') return base;
  return { ...base, ...stored };
}
