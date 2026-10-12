# Tablet app

The android app for the tablet, three tabs:

- Reports: create a report and set its severity. saved in the tablet's sqlite db as events
  (schema in `src/main/assets/db/migration/sqlite`, same as the server's)
- Server: send a severity change (a `SET_FIELD` event) to NATS, the server merges it. the
  report id is a UUID. on the emulator the docker-compose NATS is at `nats://10.0.2.2:4222`
- P2P: two Android devices find each other over Google Nearby Connections, connect, and
  exchange a severity change event as JSON (same fields as core's `EventMessage`).
  The transport sends raw bytes, merging what comes in is not done yet.

Unit tests: `./gradlew :device:testDebugUnitTest`, the db ones run on Robolectric.

## Run the two-device P2P test

1. Build the APK: `./gradlew :device:assembleDebug` (from `poc/`). It ends up in
   `device/build/outputs/apk/debug/device-debug.apk`.
2. Install it on both devices (`adb install -r <apk>`, or copy the file over and open it).
3. Turn on Bluetooth and Wi-Fi on both devices. Nearby Connections does not do it for you.
4. Open the app on both devices, tap **Start P2P** and allow every permission prompt (nearby
   devices, location).
5. After about 30 seconds each screen should show `Connected to crew-...` and the id of the other
   device. Tap **Send severity op** on one device and look at **Last received** on the other.

## Status

- Two emulators (`Tel1`, `Tel2`): connect and exchange operations in both directions.
- A real phone and an emulator do not find each other, because the emulator radios are virtual.
- Two real devices: a Galaxy S25 Ultra and a Galaxy Tab S8+ connect and send a severity change. A Galaxy S25 Ultra and a Galaxy S24 did not connect when they were far apart. Once connected in clear line of sight, the S24 and the S25 Ultra kept exchanging severity changes while the S24 walked away. This went on until the phones were ~50-60m apart. This is an estimate, not a measurement.
