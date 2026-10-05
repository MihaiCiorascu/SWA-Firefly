# Android P2P proof of concept

Two Android devices find each other over Google Nearby Connections, connect, and
exchange a `SetSeverity` operation as JSON (same fields as core's `OperationEvent`).
The transport sends raw bytes, merge and persistence are not part of this module.

## Run the two-device test

1. Build the APK: `./gradlew :android:assembleDebug` (from `poc/`). It ends up in
   `android/build/outputs/apk/debug/android-debug.apk`.
2. Install it on both devices (`adb install -r <apk>`, or copy the file over and open it).
3. Turn on Bluetooth and Wi-Fi on both devices. Nearby Connections does not do it for you.
4. Open the app on both, tap **Start P2P**, and allow every permission prompt (nearby
   devices, location).
5. Within about 30 seconds each screen should show `Connected to crew-...` with the other
   device's id. Tap **Send severity op** on one device and check **Last received** on the other.

## Status

- Two emulators (`Tel1`, `Tel2`): connect and exchange operations in both directions.
- A real phone and an emulator do not discover each other, the emulator radios are virtual.
- Two real devices: not tested yet.
