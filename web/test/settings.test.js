import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  defaultHousehold, mergeHousehold, defaultDevice, mergeDevice, CALC_METHODS, MADHABS,
} from '../src/settings.js';

test('defaultHousehold has prayer + general sections', () => {
  const c = defaultHousehold();
  assert.equal(c.prayer.method, 'NORTH_AMERICA');
  assert.equal(c.prayer.madhab, 'SHAFI');
  assert.equal(typeof c.prayer.latitude, 'number');
  assert.equal(c.general.householdName, 'Home');
});

test('mergeHousehold fills missing fields from defaults', () => {
  const merged = mergeHousehold({ prayer: { method: 'DUBAI' } });
  assert.equal(merged.prayer.method, 'DUBAI');        // kept
  assert.equal(merged.prayer.madhab, 'SHAFI');        // filled
  assert.equal(merged.prayer.adhanEnabled.FAJR, true); // nested filled
  assert.equal(merged.general.householdName, 'Home');  // section filled
});

test('mergeHousehold tolerates garbage input', () => {
  assert.deepEqual(mergeHousehold(null), defaultHousehold());
  assert.deepEqual(mergeHousehold('nope'), defaultHousehold());
});

test('defaultDevice is per-tablet and plays adhan by default', () => {
  const d = defaultDevice('Upstairs');
  assert.equal(d.label, 'Upstairs');
  assert.equal(d.playAdhan, true);
  assert.equal(d.dimStart, '22:00');
});

test('mergeDevice keeps overrides and fills the rest', () => {
  const d = mergeDevice({ label: 'Downstairs', playAdhan: false }, 'Tablet');
  assert.equal(d.label, 'Downstairs');   // kept
  assert.equal(d.playAdhan, false);      // kept override (silence this floor)
  assert.equal(d.brightness, 0.85);      // filled from default
});

test('mergeDevice falls back to a label when none stored', () => {
  assert.equal(mergeDevice(null, 'Upstairs').label, 'Upstairs');
});

test('two devices can differ while sharing the household', () => {
  const household = defaultHousehold();
  const up = mergeDevice({ label: 'Upstairs' }, 'Tablet');
  const down = mergeDevice({ label: 'Downstairs', playAdhan: false }, 'Tablet');
  // Shared prayer policy is identical...
  assert.equal(household.prayer.method, 'NORTH_AMERICA');
  // ...but each tablet decides whether it sounds the adhan.
  assert.notEqual(up.playAdhan, down.playAdhan);
});

test('method/madhab option values are unique', () => {
  const methods = CALC_METHODS.map(([v]) => v);
  assert.equal(new Set(methods).size, methods.length);
  assert.equal(MADHABS.length, 2);
});
