# Firefly proof of concept

Firefly lets crews write incident reports on a tablet without a connection. The reports merge when tablets meet or come back into coverage (see the report). This proof of concept shows three things the architecture needs. A tablet works offline. A tablet sends its events to a server through a message broker. Two tablets find each other and exchange events directly.

## What works and what does not yet

| Part | Status |
|---|---|
| Tablet app (`device`) | The Incidents and Reports tabs save every change as an event in a local SQLite store and rebuild the records from the events with the merge library, conflicts and their resolution included. The Server tab sends the tablet's events to NATS. The P2P tab connects two devices over Nearby Connections, and they exchange the events the other misses and merge them. |
| ICP server (`server`) | Reads the `operation-events` stream from NATS, stores every event in Postgres and rebuilds the incident, report and conflict rows from them with the merge library, the same way the tablet does. Each report it has merged is published back on the subject of its zone. Flyway creates the schema on startup. |
| Shared library (`core`) | Ids, events, the causal context and the merge of every field by its policy, including conflicts and `RESOLVE_CONFLICT`. The server and the tablet app both use it. |

Not built yet: tablet enrolment and the command web client. Zones are a plain latitude/longitude grid
(`core`'s `Zone`), not H3 cells; swapping in real hexagons means replacing that one class.

A report reaches a zone only once its location has settled. Location is a Tier B field, so while two
crews disagree about where a report is it belongs to no zone and is not published. The crew leaders
settling it is what sends it out.

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
docker compose down -v
docker compose up -d
./gradlew :server:bootRun
```

`down -v` drops the Postgres volume, and the first run after this change needs it. Earlier versions let
Hibernate create the tables from the entities, so an existing volume holds an `incidents` table that
does not match the schema and has no Flyway history; Flyway refuses to touch a database like that and
the server stops with `Found non-empty schema(s) "public" but no schema history table`. Dropping the
volume is only needed once, and the tablets lose nothing: they hold their own events and send them
again on the next connect.

NATS listens on port 4222 (monitoring on 8222). Postgres listens on 5432 with the database `wildfiresync`, user `postgres` and password `postgres`. The server connects to `nats://localhost:4222`. Stop the containers with `docker compose down`.

When the server is ready, its log shows `stream operation-events ready for events.operation.>` and `consuming events.operation.> as server-merge`. When a tablet sends its events, the log shows one line per event, like `merged tablet-a:2 SET_FIELD into REPORT <report id>, now 2 event(s)`, and for a report with a location, `published report <report id> with 2 event(s) to zone z2842x3716`. Sending the same operation twice stores it once, because JetStream drops a message id it has already seen.

To see what it merged, look at the tables rather than the log. `events` is the source of truth and the rest is derived from it:

```
docker exec -it wildfiresync-postgres psql -U postgres -d wildfiresync \
  -c "SELECT report_id, severity, description, latitude, longitude FROM reports;" \
  -c "SELECT record_id, field, event_id FROM field_conflicts;" \
  -c "SELECT count(*) FROM events;"
```

A column is NULL while its field is unknown or in conflict, and a field in conflict lists its candidate events in `field_conflicts`. That is the same rule the tablet's tables follow, so the two replicas answer the same way.

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

You can also open this folder in Android Studio and run `device`. The main tabs:

- **Incidents**: create an incident, rename it and set its status.
- **Reports**: create a report for an incident and edit its types, location, severity and description. A field in conflict shows every value with who wrote it and when, and the crew leader settles it with **Keep this** or an agreed wording.
- **Server**: connect to NATS. On an emulator the address is `nats://10.0.2.2:4222`. On a phone on the same network use `nats://<address of your computer>:4222`. **Connect** sends everything the tablet already holds and then keeps sending each change as it is made, so nothing has to be pushed by hand; **Send everything again** is there for after a connection was cut. Once connected the tab also subscribes to the zones around the tablet's position and around the reports it holds, and shows the position, how many events went each way and which zones it is listening to. The position is a fixed point for now (52.12, 5.84 in `DevicePosition`), not a GPS reading, so every tablet listens around the same spot. **Send everything again** and the zone list are the two things to watch when checking the round trip.
- **P2P**: see the next section.

### 3. Two-device P2P test

1. Install the app on both devices.
2. Turn on Bluetooth and Wi-Fi on both. Nearby Connections does not do it for you.
3. Open the app on both, go to the **P2P** tab, tap **Start P2P** and allow every permission prompt (nearby devices and location).
4. Within about 30 seconds each screen should show `Connected to crew-...` with the other device's id, and the sync line shows how many events went each way.
5. The Incidents and Reports tabs now show the same records on both devices. A change made on one shows up on the other.

What we have seen so far:

- A Galaxy S25 Ultra and a Galaxy Tab S8+ connect and send a severity change to each other.
- A Galaxy S25 Ultra and a Galaxy S24 did not connect when they were far apart.
- Once connected in clear line of sight, the S24 and the S25 Ultra kept exchanging severity changes while the S24 walked away. This went on until the phones were ~50-60m apart. This is an estimate, not a measurement.
- On the **Experiment** tab, the S24 sent 10,000 events to the S25 Ultra at ~1m, ~10m and ~20m. All events arrived each time, in ~0.8-1.2 s.
- Two emulators connect and exchange operations in both directions. A real phone and an emulator do not find each other, because the emulator radios are virtual.

## Tests

```
./gradlew :core:test :device:testDebugUnitTest
```

The database tests of the tablet app run on Robolectric, so they need no device.

```
./gradlew :server:test
```

`MergeServiceTests` starts a throwaway Postgres with Testcontainers, runs the real schema into it and
checks that the server stores events, rebuilds the rows from them, lists candidates for a field in
conflict and publishes a report to the right zone. It needs Docker but no running containers of its
own, and it picks its own port, so it does not clash with docker-compose. `ServerApplicationTests`
starts the whole Spring application instead, so that one needs the containers from step 1; if you
changed the Postgres port, set `SPRING_DATASOURCE_URL` as above before running it.

## Seeing the round trip

With the server running and two tablets on the Server tab, both connected:

1. On tablet A, create a report and give it a location near 52.12, 5.84, the position every tablet
   uses for now.
2. The server log shows it merging the events and publishing the report to a zone.
3. Tablet B already listens to the zones around that position. Its Server tab shows the events it has
   received, and the report appears on its Reports tab without the two tablets ever meeting over P2P.
   A report placed further away only reaches a tablet that already holds a report in or next to its
   zone.

The payload the server sends back is the report's events, not its merged row, so tablet B merges them
with whatever it already had rather than being handed an answer it cannot combine. That is why a
tablet that was away comes back into agreement instead of showing two versions.
