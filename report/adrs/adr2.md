### 4.2 ADR 2: How Reports Travel from Server to Device

**Context.** When a tablet is online and subscribed to its zones (R6), new reports from those zones should reach it quickly, as in the Connected scenario. Tablets run on battery for a twelve-hour operational period, and the network at a fire is weak and drops often. Whatever we choose has to save battery and recover from frequent drops without missing reports.

**Options.** A normal HTTP request only works one way: the tablet asks and the server answers. The server cannot send anything on its own. The four options are four ways around this.

**Short polling.** The tablet asks the server "anything new?" on a timer, for example every ten seconds. The server answers at once, most of the time with "nothing". This is the simplest option and it works on every network. The problem is the timer. With a short timer, reports arrive fast, but the tablet's radio never rests and the battery does not last twelve hours. With a long timer the battery lasts, but a report about a blocked road can arrive minutes late. No timer gives both.

**Long polling.** The tablet asks "anything new?", but the server does not answer until it has a report. Then the tablet asks again right away. Reports arrive almost at once, and there are no empty answers. But every report still costs a full new request. The network at a fire is weak, so a request that waits a long time is often cut off before the answer comes, and the tablet has to start over. Each restart uses battery and can delay a report.

**Server-Sent Events (SSE).** The tablet sends one request and the server never closes it. Each time there is a new report, the server writes it into this open connection. SSE also handles drops: when the tablet reconnects, it tells the server the last report it received, and the server continues from there. SSE only goes from server to tablet, which is all this direction needs. But the tablet already keeps a NATS connection open to send events (ADR 1), so SSE would be a second open connection. We would also have to build the service that holds these connections and remembers what each tablet has received.

**WebSocket.** A WebSocket is one connection that stays open, and both sides can send on it at any time. On its own it cannot continue after a drop; the app has to build that. In our case NATS does it. The NATS server accepts WebSocket connections [<a href="#16">16</a>], so the tablet can send its events (ADR 1) and receive reports over the same connection, and after a drop NATS continues from the last message the tablet received.

**Decision.** We chose WebSocket, through NATS. The context asks for three things: reports arrive quickly, the battery lasts twelve hours, and no report is missed after a drop. Short polling cannot give the first two together. Long polling gives fast reports, but it spends battery on repeated requests over a weak network.

SSE and WebSocket both give all three. The difference between them is what we have to build. With SSE we add a second connection and write the service behind it. With WebSocket we use the NATS connection the tablet already has (ADR 1), and NATS already continues after a drop. So WebSocket through NATS meets the three conditions with nothing extra to build or run.

**Consequences.** The tablet only talks to NATS and never calls the server. NATS keeps two message logs, one per direction. Tablets write events to the first and the server reads them (ADR 1). The server writes reports to the second and tablets read them. The server needs no extra service to send reports to tablets.

After each merge, the server finds the H3 hexagon of the report and publishes the full report under a name that holds the hexagon and the report id: `report.<hexagon>.<reportId>`. NATS keeps only the newest message per name, so it holds one message per report: its latest version. A tablet finds its own hexagon from its GPS position and subscribes to that hexagon and the ones around it (R6). It first receives the latest version of every report there, then each new version as it is published. After a drop, it continues from the last message it received.

One open connection uses less battery than polling. It still uses some, because both sides send small messages to check that the connection is alive.

The decision has two costs. First, when a report moves to another hexagon, the server also has to tell the old hexagon that the report left. Otherwise tablets there keep an old copy. Second, the server sends a full report and not single events. The message has to hold enough for the tablet to merge it with its own unsent edits and still find conflicts (Section 3.3).

## References

<a id="12">[12]</a> Confluent, "Confluent Platform System Requirements." Docs.confluent.io. https://docs.confluent.io/platform/current/installation/system-requirements.html (accessed Oct. 11, 2026)

<a id="13">[13]</a> Confluent, "Confluent REST Proxy for Apache Kafka." Docs.confluent.io. https://docs.confluent.io/platform/current/kafka-rest/index.html (accessed Oct. 11, 2026)

<a id="14">[14]</a> NATS, "JetStream." Docs.nats.io. https://docs.nats.io/nats-concepts/jetstream (accessed Oct. 11, 2026)

<a id="15">[15]</a> NATS, "What is NATS." Docs.nats.io. https://docs.nats.io/nats-concepts/what-is-nats (accessed Oct. 11, 2026)

<a id="16">[16]</a> NATS, "Connect over WebSocket." Docs.nats.io. https://docs.nats.io/running-a-nats-service/configuration/websocket (accessed Oct. 11, 2026)
