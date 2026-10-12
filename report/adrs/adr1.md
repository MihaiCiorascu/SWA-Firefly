### 4.1 ADR 1: How Events Travel from Device to Server

**Context.** A tablet creates events offline and sends them to the server at the Incident Command Post (ICP) when it reconnects (R2). The server merges them and saves the result. Three things make this hard. First, a sync can be cut off halfway, and no event may be lost (R4, Reliability). Second, at shift change a whole division reconnects within minutes, and each tablet carries twelve hours of events (Scalability). Third, the server is one machine at the ICP, so what runs on it has to be small and easy to operate.

**Options.** We looked at four options, in two groups. In the first two, the tablet gives its events straight to the server or to its database. In the last two, another program sits in between: it saves the events first, and the server merges them later.

**HTTP request.** The tablet puts its events in one request and sends it. The server merges the events, saves the result in the database, and then answers. The tablet waits for that answer. This is the simplest option: there is no extra program to run, and when something fails the tablet knows at once, because it gets an error. The weak point is shift change. Every tablet waits while the server merges its events, so when a whole division sends at the same time, the server falls behind and answers slowly. A tablet that waits too long gives up and sends again. Each repeat is more work for a server that is already behind, so the surge makes itself worse.

**Direct database write.** Here no server program stands between the tablet and the database. Each tablet logs in to the database at the ICP and adds its events to a table. The merge runs later, inside the database or as a job that starts every few seconds. Saving a row is fast, so tablets do not wait for the merge, and there is one program less to build. This option has two problems. First, every tablet needs a login to the database. A stolen tablet could then read or change any report, which is the risk described in Section 2.9. Second, every tablet has to know exactly what the tables look like. When we change a table, tablets with an older version of the app can no longer write to it. Agencies update on their own schedule, so this breaks Compatibility across versions.

**Kafka.** Kafka is a message log: a program that receives messages, saves them on disk in order, and lets other programs read them later. The tablet sends its events to Kafka and gets a confirmation once they are saved. The server reads the events from Kafka and merges them at its own speed. This solves shift change. A tablet is done as soon as Kafka has saved its events, and the server can take minutes to catch up while no tablet waits. It also solves a sync that is cut off, because the tablet knows which events were confirmed and which were not. But Kafka is normally run on a group of machines. Confluent's guide for production lists three machines with 64 GB of memory each [<a href="#12">12</a>]. Kafka can run on one small machine, but it is not sized for that. Kafka's client is also made for servers, not for phones. The usual way to connect an app is through an extra service in front of Kafka, the REST Proxy [<a href="#13">13</a>]. We would have to run that as well.

**NATS JetStream.** NATS is also a message log, and JetStream is the part of it that saves messages on disk [<a href="#14">14</a>]. For the tablet it works the same way as Kafka: send the events, get a confirmation once they are saved, and the server reads them later. The difference is size. The NATS server is one small program that typically uses about 15 MB of memory [<a href="#15">15</a>]. A tablet can connect to it directly, also over WebSocket [<a href="#16">16</a>].

**Decision.** We chose NATS JetStream. The context sets three conditions: no event is lost, shift change does not slow the tablets down, and everything runs on one machine. NATS JetStream is the only option that meets all three without a new problem. A tablet gets a confirmation only after its events are on disk, so it knows which ones to send again. Events wait in NATS while the server merges, so no tablet waits for the server. And NATS is one small program.

The other options each fail somewhere. With the HTTP request, tablets wait for the merge, so it fails at shift change. The direct database write meets the three conditions, but it gives every tablet a database login and breaks tablets with an older app version. Kafka meets the first two, but it is sized for a group of machines and needs the REST Proxy next to it.

**Consequences.** A tablet marks an event as sent only after NATS confirms that it is saved. If a sync is cut off halfway, the tablet sends the unconfirmed events next time. This meets R4. The same event can arrive twice, for example when a confirmation is lost and the tablet sends the event again. A duplicate changes nothing, because every event has a dot and the merge is idempotent (Section 3.3).

At shift change, events wait in NATS and the server merges them at its own speed. NATS servers can also be linked together, so the regional or cloud layer from Section 2.2 stays possible.

The cost is one more program to install and watch at the ICP, next to our server and database.

## References

<a id="12">[12]</a> Confluent, "Confluent Platform System Requirements." Docs.confluent.io. https://docs.confluent.io/platform/current/installation/system-requirements.html (accessed Oct. 11, 2026)

<a id="13">[13]</a> Confluent, "Confluent REST Proxy for Apache Kafka." Docs.confluent.io. https://docs.confluent.io/platform/current/kafka-rest/index.html (accessed Oct. 11, 2026)

<a id="14">[14]</a> NATS, "JetStream." Docs.nats.io. https://docs.nats.io/nats-concepts/jetstream (accessed Oct. 11, 2026)

<a id="15">[15]</a> NATS, "What is NATS." Docs.nats.io. https://docs.nats.io/nats-concepts/what-is-nats (accessed Oct. 11, 2026)

<a id="16">[16]</a> NATS, "Connect over WebSocket." Docs.nats.io. https://docs.nats.io/running-a-nats-service/configuration/websocket (accessed Oct. 11, 2026)
