// TP-Link Kasa local-LAN client (legacy protocol) — pure Node, zero deps.
//
// Works for classic Kasa plugs/switches/bulbs (e.g. HS200, KP125, KL110/130)
// that speak the port-9999 "autokey XOR" protocol. NEWER Tapo / KLAP-only
// devices use an encrypted AES handshake and will NOT answer discovery here —
// if `discover` finds nothing but the Kasa app sees your devices, they're
// likely KLAP and we'll add that path.
//
// This same protocol is ported to Kotlin for the tablet app; keeping the
// reference here lets us verify it on a laptop before any hardware arrives.

import net from 'node:net';
import dgram from 'node:dgram';

const PORT = 9999;
const INITIAL_KEY = 0xab; // 171

// ---- Autokey XOR cipher ----------------------------------------------------
export function encrypt(text) {
  const buf = Buffer.from(text, 'utf8');
  const out = Buffer.alloc(buf.length);
  let key = INITIAL_KEY;
  for (let i = 0; i < buf.length; i++) {
    out[i] = buf[i] ^ key;
    key = out[i]; // cipher byte feeds the next key
  }
  return out;
}

export function decrypt(buf) {
  const out = Buffer.alloc(buf.length);
  let key = INITIAL_KEY;
  for (let i = 0; i < buf.length; i++) {
    out[i] = buf[i] ^ key;
    key = buf[i]; // ciphertext byte feeds the next key
  }
  return out.toString('utf8');
}

/** TCP framing = 4-byte big-endian length prefix + encrypted payload. */
export function encryptWithHeader(text) {
  const body = encrypt(text);
  const header = Buffer.alloc(4);
  header.writeUInt32BE(body.length, 0);
  return Buffer.concat([header, body]);
}

export function decryptWithHeader(buf) {
  return decrypt(buf.subarray(4));
}

// ---- Transport -------------------------------------------------------------
/** Send one JSON command to a device over TCP and resolve its JSON reply. */
export function send(ip, command, { timeout = 4000 } = {}) {
  const payload = JSON.stringify(command);
  return new Promise((resolve, reject) => {
    const socket = new net.Socket();
    const chunks = [];
    let expected = null;

    const fail = (e) => { socket.destroy(); reject(e); };
    socket.setTimeout(timeout, () => fail(new Error(`Timeout talking to ${ip}`)));
    socket.connect(PORT, ip, () => socket.write(encryptWithHeader(payload)));

    socket.on('data', (d) => {
      chunks.push(d);
      const buf = Buffer.concat(chunks);
      if (expected == null && buf.length >= 4) expected = buf.readUInt32BE(0);
      if (expected != null && buf.length >= expected + 4) {
        socket.end();
        try { resolve(JSON.parse(decryptWithHeader(buf))); }
        catch (e) { reject(e); }
      }
    });
    socket.on('error', fail);
  });
}

/** UDP broadcast discovery. Resolves a list of {ip, alias, model, type, state}. */
export function discover({ timeout = 3000, broadcast = '255.255.255.255' } = {}) {
  return new Promise((resolve) => {
    const socket = dgram.createSocket({ type: 'udp4', reuseAddr: true });
    const found = new Map();
    const query = encrypt(JSON.stringify({ system: { get_sysinfo: {} } }));

    socket.on('message', (msg, rinfo) => {
      try {
        const info = JSON.parse(decrypt(msg))?.system?.get_sysinfo;
        if (info) found.set(rinfo.address, summarize(rinfo.address, info));
      } catch { /* ignore non-Kasa noise */ }
    });
    socket.bind(() => {
      socket.setBroadcast(true);
      socket.send(query, 0, query.length, PORT, broadcast);
    });
    setTimeout(() => { socket.close(); resolve([...found.values()]); }, timeout);
  });
}

// ---- Device helpers --------------------------------------------------------
export async function getSysInfo(ip) {
  const res = await send(ip, { system: { get_sysinfo: {} } });
  return res?.system?.get_sysinfo;
}

export function isBulb(info) {
  const t = (info?.mic_type || info?.type || '').toUpperCase();
  return t.includes('SMARTBULB') || info?.light_state != null || info?.is_dimmable === 1;
}

/** Turn a device on/off, auto-detecting bulb vs plug/switch. */
export async function setPower(ip, on, info = null) {
  const sysinfo = info || (await getSysInfo(ip));
  if (isBulb(sysinfo)) {
    return send(ip, {
      'smartlife.iot.smartbulb.lightingservice': {
        transition_light_state: { on_off: on ? 1 : 0, transition_period: 0 },
      },
    });
  }
  return send(ip, { system: { set_relay_state: { state: on ? 1 : 0 } } });
}

/** Set brightness 0–100 on a dimmable bulb. */
export function setBrightness(ip, pct) {
  const brightness = Math.max(1, Math.min(100, Math.round(pct)));
  return send(ip, {
    'smartlife.iot.smartbulb.lightingservice': {
      transition_light_state: { on_off: 1, brightness, transition_period: 0 },
    },
  });
}

function summarize(ip, info) {
  const state = isBulb(info)
    ? (info.light_state?.on_off ?? '?')
    : (info.relay_state ?? '?');
  return {
    ip,
    alias: info.alias ?? '(unnamed)',
    model: info.model ?? '?',
    type: isBulb(info) ? 'bulb' : 'plug/switch',
    on: state === 1,
  };
}

// ---- CLI -------------------------------------------------------------------
async function main(argv) {
  const [cmd, ip, arg] = argv;
  switch (cmd) {
    case 'discover': {
      console.log('Scanning the LAN for Kasa devices (3s)…');
      const devices = await discover();
      if (!devices.length) {
        console.log('No devices found. Ensure you are on the same Wi-Fi as the lights.');
        console.log('(If the Kasa app sees them but this does not, they may be Tapo/KLAP.)');
        return;
      }
      for (const d of devices) {
        console.log(`  ${d.ip.padEnd(15)} ${d.on ? 'ON ' : 'off'}  ${d.type.padEnd(11)} ${d.model.padEnd(10)} ${d.alias}`);
      }
      return;
    }
    case 'info': {
      requireIp(ip);
      console.log(JSON.stringify(await getSysInfo(ip), null, 2));
      return;
    }
    case 'on':
    case 'off': {
      requireIp(ip);
      await setPower(ip, cmd === 'on');
      console.log(`${ip} → ${cmd}`);
      return;
    }
    case 'brightness': {
      requireIp(ip);
      if (arg == null) throw new Error('Usage: brightness <ip> <0-100>');
      await setBrightness(ip, Number(arg));
      console.log(`${ip} → brightness ${arg}%`);
      return;
    }
    default:
      console.log(`Home Dashboard — Kasa control

Usage:
  node kasa.mjs discover
  node kasa.mjs info <ip>
  node kasa.mjs on <ip>
  node kasa.mjs off <ip>
  node kasa.mjs brightness <ip> <0-100>

Run "discover" first to get device IPs.`);
  }
}

function requireIp(ip) {
  if (!ip) throw new Error('Missing <ip>. Run "node kasa.mjs discover" first.');
}

// Only run the CLI when invoked directly (not when imported by tests).
import { fileURLToPath } from 'node:url';
if (process.argv[1] === fileURLToPath(import.meta.url)) {
  main(process.argv.slice(2)).catch((e) => { console.error('Error:', e.message); process.exit(1); });
}
