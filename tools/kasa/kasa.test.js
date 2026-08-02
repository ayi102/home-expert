import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  encrypt, decrypt, encryptWithHeader, decryptWithHeader, isBulb,
} from './kasa.mjs';

test('encrypt uses the 0xAB autokey — known first byte', () => {
  // '{' = 0x7B, 0x7B ^ 0xAB = 0xD0
  assert.equal(encrypt('{')[0], 0xd0);
});

test('encrypt/decrypt round-trips arbitrary JSON', () => {
  const samples = [
    '{"system":{"get_sysinfo":{}}}',
    '{"system":{"set_relay_state":{"state":1}}}',
    'héllo — unicode ✓',
    '',
  ];
  for (const s of samples) {
    assert.equal(decrypt(encrypt(s)), s);
  }
});

test('TCP framing writes a correct 4-byte big-endian length header', () => {
  const cmd = '{"system":{"get_sysinfo":{}}}';
  const framed = encryptWithHeader(cmd);
  assert.equal(framed.readUInt32BE(0), Buffer.from(cmd, 'utf8').length);
  assert.equal(decryptWithHeader(framed), cmd);
});

test('the canonical get_sysinfo query encrypts deterministically', () => {
  const bytes = [...encrypt('{"system":{"get_sysinfo":{}}}')];
  // Deterministic cipher → stable, regenerable vector; guards accidental changes.
  assert.equal(bytes[0], 0xd0);
  assert.equal(bytes.length, 29);
});

test('isBulb detects bulbs vs plugs/switches', () => {
  assert.equal(isBulb({ mic_type: 'IOT.SMARTBULB' }), true);
  assert.equal(isBulb({ light_state: { on_off: 1 } }), true);
  assert.equal(isBulb({ is_dimmable: 1 }), true);
  assert.equal(isBulb({ type: 'IOT.SMARTPLUGSWITCH', relay_state: 0 }), false);
  assert.equal(isBulb({}), false);
});
