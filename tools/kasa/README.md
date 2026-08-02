# Kasa control (laptop test tool)

Control your TP-Link **Kasa** lights/switches from any machine on the same Wi-Fi — no
account, no cloud, no dependencies (pure Node). This is the same local-LAN protocol the
tablet app will use; running it on your laptop verifies control works before the tablets arrive.

## Use it

From a machine on the **same Wi-Fi as the lights**:

```sh
cd tools/kasa

node kasa.mjs discover              # 1) find devices → prints IPs, names, on/off
node kasa.mjs on   192.168.1.50     # 2) turn one on  (use an IP from discover)
node kasa.mjs off  192.168.1.50     #    turn it off
node kasa.mjs brightness 192.168.1.50 40   # dim to 40% (bulbs)
node kasa.mjs info 192.168.1.50     # full device details
```

`on`/`off` auto-detect bulbs vs plugs/switches.

## Test the protocol (no hardware needed)

```sh
node --test        # 5 cipher/framing tests
```

## If `discover` finds nothing

1. Confirm the laptop is on the **same Wi-Fi/subnet** as the lights (not guest Wi-Fi).
2. If the **Kasa phone app** sees the devices but `discover` doesn't, they're almost
   certainly newer **Tapo / KLAP** hardware, which uses an encrypted AES handshake instead
   of this legacy protocol. Tell me and I'll add the KLAP path (the Kotlin app needs it too).
3. Some networks block UDP broadcast between devices ("AP/client isolation") — check the
   router if the app works on cellular but not Wi-Fi.

## How it works

- Kasa's legacy protocol is JSON over TCP/UDP port 9999, obfuscated with an "autokey" XOR
  cipher (seed `0xAB`). Discovery is a UDP broadcast of `{"system":{"get_sysinfo":{}}}`.
- TCP messages are prefixed with a 4-byte big-endian length; UDP datagrams are not.
- See `kasa.mjs` — the cipher/framing there is what gets ported to Kotlin for the tablet.
