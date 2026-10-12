# Tablet app

The android app for the tablet, three tabs:

- Reports: create a report and set its severity. saved in the tablet's sqlite db as events
  (schema in `src/main/assets/db/migration/sqlite`, same as the server's)
- Server: send a `SetSeverity` op to NATS, the server merges it. on the emulator the
  docker-compose NATS is at `nats://10.0.2.2:4222`
- P2P: two Android devices find each other over Google Nearby Connections, connect, and
  exchange a `SetSeverity` operation as JSON (same fields as core's `OperationEvent`).
  The transport sends raw bytes, merging what comes in is not done yet.

Unit tests: `./gradlew :device:testDebugUnitTest`, the db ones run on Robolectric.

## Run the two-device P2P test

1. Build the APK: `./gradlew :device:assembleDebug` (from `poc/`). It ends up in
   `device/build/outputs/apk/debug/device-debug.apk`.
2. Install it on both devices (`adb install -r <apk>`, or copy the file over and open it).
3. Turn on Bluetooth and Wi-Fi on both devices. Nearby Connections does not do it for you.
4. Open the app on both, tap **Start P2P**, and allow every permission prompt (nearby
   devices, location).
5. Within about 30 seconds each screen should show `Connected to crew-...` with the other
   device's id. Tap **Send severity op** on one device and check **Last received** on the other.

## Status

- Two emulators (`Tel1`, `Tel2`): connect and exchange operations in both directions.
- A real phone and an emulator do not discover each other, the emulator radios are virtual.
- Two real devices: a Galaxy S26 and a Galaxy Tab S8+ connect and send a severity change. A Galaxy S26 and a Galaxy S24 did not connect when they were not close to each other.
