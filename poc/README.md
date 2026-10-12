# Firefly proof of concept

Firefly lets crews write incident reports on a tablet without a connection. The reports merge when tablets meet or come back into coverage (see the report). This proof of concept shows three things the architecture needs. A tablet works offline. A tablet sends its events to a server through a message broker. Two tablets find each other and exchange events directly.

## What works and what does not yet

| Part | Status |
|---|---|
| Tablet app (`device`) | The Reports tab saves report changes as events in a local SQLite store. The Server tab sends one severity change to NATS. The P2P tab connects two devices over Nearby Connections. One device sends a severity change to the other. |
| ICP server (`server`) | Reads the `operation-events` stream from NATS. The merge service only logs each event so far. The Postgres schema is ready. |
| Shared library (`core`) | Ids, events, the causal context, the field policies and the first severity merge. The server and the tablet app both use it. |

Not built yet: zone subscription, the report publisher, tablet enrolment, the command web client and merging what arrives over P2P.

## Layout

- `core`: plain Java library shared by the server and the tablet app.
- `server`: Spring Boot service for the Incident Command Post.
- `device`: the Android tablet app.
- `docker-compose.yaml`: NATS with JetStream and Postgres for the server side.

## Requirements

- JDK 21. Gradle downloads it automatically if it is missing.
- Docker, for NATS and Postgres.
- The Android SDK, with `local.properties` in this folder pointing at it (`sdk.dir=...`). Android Studio creates the file when it opens the project.
- An Android device or emulator with Android 14 or newer, because the local store uses SQLite `STRICT` tables.

## Run it

### 1. Server side

From this folder:

```
docker compose up -d
./gradlew :server:bootRun
```

NATS listens on port 4222 (monitoring on 8222). Postgres listens on 5432 with the database `wildfiresync`, user `postgres` and password `postgres`. The server connects to `nats://localhost:4222`. Stop the containers with `docker compose down`.

When the server is ready, its log shows `stream operation-events ready for events.operation.>` and `consuming events.operation.> as server-merge`. When a tablet sends a severity change, the log shows a line like `tablet-a:1 received for report report-1 severity 4`. Sending the same operation twice stores it once, because JetStream drops a message id it has already seen.

If another Postgres already runs on your computer on port 5432, the Postgres container cannot start. Stop that Postgres first, or change the port on the left of `5432:5432` in `docker-compose.yaml` (for example to `5433:5432`) and start the server with the matching address, without editing any file:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/wildfiresync ./gradlew :server:bootRun
```

In PowerShell set the variable first with `$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5433/wildfiresync"`.

### 2. Tablet app

Build the app and install it on a device or emulator:

```
./gradlew :device:assembleDebug
adb install -r device/build/outputs/apk/debug/device-debug.apk
```

You can also open this folder in Android Studio and run `device`. The app has three tabs.

- **Reports**: create a report and set its severity. The app saves the change as an event in the local database.
- **Server**: send a severity change to NATS. On an emulator the address is `nats://10.0.2.2:4222`. On a phone on the same network use `nats://<address of your computer>:4222`. The server log then shows that it received the operation.
- **P2P**: see the next section.

### 3. Two-device P2P test

1. Install the app on both devices.
2. Turn on Bluetooth and Wi-Fi on both. Nearby Connections does not do it for you.
3. Open the app on both, go to the **P2P** tab, tap **Start P2P** and allow every permission prompt (nearby devices and location).
4. Within about 30 seconds each screen should show `Connected to crew-...` with the other device's id.
5. Tap **Send severity op** on one device and check **Last received** on the other.

What we have seen so far:

- A Galaxy S25 Ultra and a Galaxy Tab S8+ connect and send a severity change to each other.
- A Galaxy S25 Ultra and a Galaxy S24 did not connect when they were far apart.
- Once connected in clear line of sight, the S24 and the S25 Ultra kept exchanging severity changes while the S24 walked away. This went on until the phones were ~50-60m apart. This is an estimate, not a measurement.
- On the **Experiment** tab, the S24 sent 10,000 events to the S25 Ultra three times. All events arrived, in ~0.8-1.2 s each time.
- Two emulators connect and exchange operations in both directions. A real phone and an emulator do not find each other, because the emulator radios are virtual.

## Tests

```
./gradlew :core:test :device:testDebugUnitTest
```

The database tests of the tablet app run on Robolectric, so they need no device. `./gradlew :server:test` starts the Spring application, so it needs the Postgres container from step 1 to be running. If you changed the Postgres port, set `SPRING_DATASOURCE_URL` as above before running the tests.
