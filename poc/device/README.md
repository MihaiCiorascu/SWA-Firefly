# Tablet app

The android app for the tablet, five tabs:

- Incidents: create an incident, rename it, set its status
- Reports: create a report for an incident and edit every field (types, location, severity,
  description). every change is saved in the tablet's sqlite db as an event (schema in
  `src/main/assets/db/migration/sqlite`, same as the server's), and the reports, incidents and
  conflicts tables are rebuilt from the events with core's `MergeCore`. a field two crews wrote
  without knowing about each other shows both values with who wrote them and when, **Keep this**
  or an agreed wording sends a `RESOLVE_CONFLICT` that settles it on every tablet
- Server: send every event the tablet holds to NATS, the broker drops what it already has.
  on the emulator the docker-compose NATS is at `nats://10.0.2.2:4222`
- P2P: two Android devices find each other over Google Nearby Connections and sync by
  themselves: each sends its causal context, gets back the events it misses, and merges them.
  later changes are passed on right away, also to a third tablet. the tab shows what went in
  and out and how many fields are in conflict
- Experiment: the P2P transfer experiment of the report

Unit tests: `./gradlew :device:testDebugUnitTest`, the db ones run on Robolectric.

## Run the two-device P2P test

1. Build the APK: `./gradlew :device:assembleDebug` (from `poc/`). It ends up in
   `device/build/outputs/apk/debug/device-debug.apk`.
2. Install it on both devices (`adb install -r <apk>`, or copy the file over and open it).
3. Turn on Bluetooth and Wi-Fi on both devices. Nearby Connections does not do it for you.
4. Open the app on both devices, tap **Start P2P** and allow every permission prompt (nearby
   devices, location).
5. After about 30 seconds each screen should show `Connected to crew-...` and the id of the other
   device, and the sync line shows how many events went each way. The Incidents and Reports tabs
   now show the same records on both. A change made on one device shows up on the other.

## Status

- Two emulators (`Tel1`, `Tel2`): connect and exchange operations in both directions.
- A real phone and an emulator do not find each other, because the emulator radios are virtual.
- Two real devices: a Galaxy S25 Ultra and a Galaxy Tab S8+ connect and send a severity change. A Galaxy S25 Ultra and a Galaxy S24 did not connect when they were far apart. Once connected in clear line of sight, the S24 and the S25 Ultra kept exchanging severity changes while the S24 walked away. This went on until the phones were ~50-60m apart. This is an estimate, not a measurement.
