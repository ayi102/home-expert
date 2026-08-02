import * as adhan from 'adhan';

// Canonical reference check using the batoulapps `adhan` library (the JS twin
// of the Kotlin `adhan2` we'll use in the app — same author, same algorithm).
// We generate test vectors here that the Android JUnit tests will assert.

function fmtUTC(d) {
  return d.toISOString().slice(11, 16); // HH:mm in UTC
}
function fmtLocal(d, tz) {
  return new Intl.DateTimeFormat('en-US', {
    hour: '2-digit', minute: '2-digit', hour12: false, timeZone: tz,
  }).format(d);
}

const coords = new adhan.Coordinates(35.7796, -78.6382); // Raleigh, NC
const tz = 'America/New_York';
const date = new Date(2026, 6, 30); // 2026-07-30 (local wall date)

function times(methodName, madhab) {
  const params = adhan.CalculationMethod[methodName]();
  params.madhab = madhab;
  const pt = new adhan.PrayerTimes(coords, date, params);
  return {
    fajr: pt.fajr, sunrise: pt.sunrise, dhuhr: pt.dhuhr,
    asr: pt.asr, maghrib: pt.maghrib, isha: pt.isha,
  };
}

function show(label, t) {
  console.log(`\n== ${label} ==`);
  for (const [k, v] of Object.entries(t)) {
    console.log(`  ${k.padEnd(8)} UTC ${fmtUTC(v)}   ${tz} ${fmtLocal(v, tz)}`);
  }
}

const isnaShafi = times('NorthAmerica', adhan.Madhab.Shafi);
const isnaHanafi = times('NorthAmerica', adhan.Madhab.Hanafi);
const mwlShafi = times('MuslimWorldLeague', adhan.Madhab.Shafi);

show('ISNA / Shafi', isnaShafi);
show('ISNA / Hanafi (Asr should be later)', isnaHanafi);
show('MuslimWorldLeague / Shafi (Fajr/Isha should differ from ISNA)', mwlShafi);

// Behavioral assertions — prove the engine responds to parameters correctly.
const assert = (cond, msg) => { if (!cond) { console.error('FAIL:', msg); process.exit(1); } };
assert(isnaHanafi.asr.getTime() > isnaShafi.asr.getTime(), 'Hanafi Asr later than Shafi');
assert(mwlShafi.fajr.getTime() !== isnaShafi.fajr.getTime(), 'MWL Fajr differs from ISNA');
const order = ['fajr','sunrise','dhuhr','asr','maghrib','isha'];
for (let i = 1; i < order.length; i++) {
  assert(isnaShafi[order[i]].getTime() > isnaShafi[order[i-1]].getTime(),
    `${order[i]} after ${order[i-1]}`);
}

// Qibla direction from Raleigh (expected ~northeast, roughly 55-60°).
const qibla = adhan.Qibla(coords);
console.log(`\nQibla bearing from Raleigh: ${qibla.toFixed(2)}° from true north`);
assert(qibla > 40 && qibla < 75, 'Qibla bearing in plausible NE range for US East Coast');

// Hijri date via native Intl (no dependency) — what the tile will show.
const hijri = new Intl.DateTimeFormat('en-US-u-ca-islamic-umalqura', {
  day: 'numeric', month: 'long', year: 'numeric',
}).format(date);
console.log(`Hijri (umalqura) for 2026-07-30: ${hijri}`);

console.log('\nAll behavioral assertions passed. Vectors above → Android JUnit test.');
