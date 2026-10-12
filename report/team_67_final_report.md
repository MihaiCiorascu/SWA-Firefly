<div align="center">

### CS4505 Software Architecture Heron Report
# Firefly

**Group 67**

Rares Andrei Popa

Stefan Claudiu Avram

Alexandru Cristian Cazacu

Mihai Ciorascu

Radu Zidaru

</div>

## Table of Contents

- [1. Introduction](#1-introduction)
- [2. Problem and Context Analysis](#2-problem-and-context-analysis)
  - [2.1 Problem Description](#21-problem-description)
  - [2.2 Context View](#22-context-view)
  - [2.3 Stakeholders](#23-stakeholders)
  - [2.4 Wardley Map](#24-wardley-map)
  - [2.5 Example Scenarios](#25-example-scenarios)
  - [2.6 Architecturally Significant Requirements](#26-architecturally-significant-requirements)
  - [2.7 Quality Attributes](#27-quality-attributes)
  - [2.8 Roadmap and Pricing](#28-roadmap-and-pricing)
  - [2.9 Ethical and Legal Considerations](#29-ethical-and-legal-considerations)
- [3. Architecture](#3-architecture)
  - [3.1 Components](#31-components)
  - [3.2 Event-Based Domain Model](#32-event-based-domain-model)
  - [3.3 Elaborating Field Reporting](#33-elaborating-field-reporting)
  - [3.4 Container View](#34-container-view)
  - [3.5 Component View](#35-component-view)
  - [3.6 Code View](#36-code-view)
  - [3.7 Runtime Views](#37-runtime-views)
  - [3.8 Deployment View](#38-deployment-view)
  - [3.9 Architectural Decisions](#39-architectural-decisions)
  - [3.10 How This Connects to Chapter 2](#310-how-this-connects-to-chapter-2)
- [4. Architectural Decision Records](#4-architectural-decision-records)
  - [4.1 ADR 1: How Events Travel from Device to Server](#41-adr-1-how-events-travel-from-device-to-server)
  - [4.2 ADR 2: How Reports Travel from Server to Device](#42-adr-2-how-reports-travel-from-server-to-device)
  - [4.3 ADR 3: How events are authenticated](#43-adr-3-how-events-are-authenticated)
- [5. Architectural Experiments](#5-architectural-experiments)
  - [5.1 Experiment 1: Peer-to-peer sync between two real tablets](#51-experiment-1-peer-to-peer-sync-between-two-real-tablets)
  - [5.2 Experiment 2: The cost of a sync as the event log grows](#52-experiment-2-the-cost-of-a-sync-as-the-event-log-grows)
  - [5.3 Validity and next steps](#53-validity-and-next-steps)
- [References](#references)

## 1. Introduction

Climate change has made major natural disasters common. First responders often work in situations that go far beyond normal operations, with unreliable or no connectivity for long stretches. Supervisors face heavy workloads where lives are at stake, made worse because radio communications used to issue orders also have to carry field reports up the command chain. Chapter 2 explores this problem in more detail.

We propose Firefly, an app that lets emergency personnel record and edit incident reports from the field. The system works offline: personnel can create and edit reports with no connection, and a backend merges conflicting edits once connectivity returns. Since more than one person can edit the same report while offline, we designed a conflict-resolution policy that avoids losing safety-critical information. We also added peer-to-peer sync between devices that are near each other, so personnel get the most complete picture even with low connectivity.

Chapter 3 covers the architecture. Chapter 4 records three architectural decisions. Chapter 5 designs two experiments. Section 2.8 covers the roadmap.

## 2. Problem and Context Analysis

### 2.1 Problem Description

During large-scale disaster response, radio channels quickly get overloaded with both field reports and orders, which leads to mismanagement: congested frequencies and a broken command structure during the Pacific Palisades Fire (2025) [<a href="#1">1</a>][<a href="#2">2</a>][<a href="#3">3</a>], and a public dispute over resources during the Gironde wildfires in France (2026). [<a href="#5">5</a>][<a href="#6">6</a>]

This overload also falls on the small group coordinating the response. Overworking them can be disastrous: in the 2002 Überlingen mid-air collision, one air traffic controller managing two positions alone failed to separate two aircraft in time, killing all 71 people aboard. [<a href="#4">4</a>]

Firefly lets field workers share incident reports even when the usual channels are overloaded, merging conflicting reports where possible so effort during a disaster response is not wasted.

### 2.2 Context View

The Context View shows the boundary between the system and its environment. As the lecture says it, "you design the system, the context is a given" [<a href="#11">11</a>].

Firefly's scope is to keep field reports in sync, so every device ends up with the same complete record, connected or not. The boundary is the Incident Command Post: the server is a physical machine at the ICP, not a cloud endpoint, going beyond it is future work. Voice radio and crew assignment stay human decisions, outside our scope. Our job is narrower: make sure command staff act on one complete record, not a partial picture.

Crew leaders use tablets and record offline by default. A device syncs automatically when it reconnects, either to the ICP or to another tablet. Division leaders and command staff see the current zone-scoped record through the ICP server. When two crews meet, their leaders can opt in to peer-to-peer sync, so tablets never exchange data without the crews knowing.

Two existing tools show this gap. ArcGIS Field Maps resolves concurrent edits with last-write-wins, and a real conflict still needs a database administrator to fix it by hand. ATAK with goTenna or Meshtastic sends append-only messages, so a conflict never happens there either. Our problem, merging concurrent offline edits automatically, sits exactly between the two.

We depend on GPS accuracy for zone subscription, covered more in the Wardley Map below and in Section 3.1. A fire-authority record system and a weather feed are future work, named here only so the figure stays accurate.

<figure>
  <img title="" src="../report/figures/context-view.png" alt="context view diagram">
  <figcaption><em>Figure 1: Context view. The system boundary is the ICP local server. Regional/cloud reach and external record systems are dashed, future work only.</em></figcaption>
</figure>

### 2.3 Stakeholders

Stakeholders fall into five categories. Field workers (firefighters, crew leads) create and edit reports and have the most direct contact with the system, but no formal power over its design. Command and decision-making staff, like incident commanders or EOC personnel, consume the merged view and are the main reason the system exists, together with the owning agency, such as a regional fire service, which controls procurement, deployment, and data governance. Above them sit government and regulatory bodies, such as national ministries or data protection regulators. They hold high power, set policy and funding, and are not always aligned with the owning agency. Platform developers design and improve the system. Indirectly affected parties, such as civilians named in reports or any oversight body, have a stake without using the system directly.

Plotting each stakeholder on a Power/Interest grid shows which of them deserve the most focus, and highlights possible conflicts.

<figure>
  <img title="" src="../report/figures/stakeholder_power_interest_grid.png"  alt="power-interest grid diagram">
  <figcaption><em>Figure 2: Power-Interest Grid for Stakeholders</em> </figcaption>
</figure>

### 2.4 Wardley Map

A Wardley map puts the user and the key system parts on two axes: value visible to the user, and evolution stage, from Genesis, "uncertainty about everything, including functionality," through Custom built and Product, to Commodity, "as standard as water and electricity" [<a href="#11">11</a>]. The map itself is not the point, what it shows is where our effort should go.

Field Reporting is the merge logic that solves concurrent offline edits (Chapter 3). It sits alone in the genesis, high-value quadrant. No existing tool merges concurrent edits correctly for this problem (Section 2.2), and this decides whether Firefly's core promise, no edit ever disappears, actually holds. Zone Subscription is still fairly custom, we chose its radius-based H3 subscription on purpose to avoid an edge-of-hexagon blind spot, but it gives less direct value than the merge logic. Synchronization sits closer to the middle, custom-built for us but kept free of wildfire-specific logic, so it stays reusable. Device Identity, the tablet hardware, and the local WiFi or Bluetooth stack all sit near commodity, well-understood pieces we can just treat as given.

The map can change. If H3 tools become something everybody uses, Zone Subscription could move toward product. Field Reporting stays in genesis only as long as no one else solves this merge problem, a real competitor would force us to redraw the map.

<figure>
  <img title="" src="../report/figures/wardley-map.png" alt="wardley map diagram">
  <figcaption><em>Figure 3: Wardley map of the system's main components. Field Reporting is the one genesis-stage, high-value component, matching its identification as the core domain in Chapter 3.</em></figcaption>
</figure>

### 2.5 Example Scenarios

To show how the system works in practice, this section presents four scenarios at user-goal level [<a href="#8">8</a>]: working with a connection, logging without one, two crews meeting, and coming back into coverage. These scenarios cover the requirements that shape the architecture. The remaining requirements are in Appendix A, as user stories.

- **Working with a connection**: Alice leads a crew on the eastern flank. She marks a blocked access road on her tablet, and it shows up on the command staff's map at the Incident Command Post. Reports from nearby crews appear on her map too, each one naming the crew and time. When command staff assign her crew for the next shift, the assignment arrives the same way, though anything urgent still comes over the radio. Reports from crews on the far side of the fire never reach her tablet, since they are of no use to her.

- **Logging without a connection**: Alice's crew moves below a ridge and loses coverage. She sees a spot fire across the containment line, calls it in on the radio, then marks it on her map with its size and direction. Her tablet keeps the report with her name and the time, and she keeps logging through the afternoon from the map she had when the signal went.

- **Two crews meeting**: Alice's crew runs into Bob's crew later that day. Neither has had a connection since morning, so they switch on P2P mode. Each tablet shows the other crew, and once the sync finishes, each of them sees a summary of what came in and out. Almost everything lands on both maps, except conflicting logs: Alice had marked one road blocked, Bob had marked it open. Both tablets show the road with both reports, who made them and when, and Alice and Bob settle it themselves before splitting up again, so neither report is thrown away.

- **Coming back into coverage**: Bob drives back to the staging area and his tablet picks up a signal, syncing with the central server. Everything his crew logged that day, along with what he took from Alice, reaches command staff. In return, his tablet receives the reports it missed for the zones around him.

### 2.6 Architecturally Significant Requirements

Not every requirement shapes the architecture. We treat one as architecturally significant if it needs dedicated components, is expensive to change later, or affects future functions [<a href="#7">7</a>]. Six requirements meet at least one of these.

- **R1, working without a connection.** A tablet can store new reports, update existing ones, and sync in P2P mode while offline. Each tablet keeps its own copy of the record instead of reading one from a server. This is also the hardest requirement to add later, assuming a live connection first would mean designing the system twice.

- **R2, syncing with the central server.** When a tablet has a connection, it syncs on its own, sending its reports and receiving updates and assignments for its zones. Unlike P2P mode, this happens without anyone asking, so a component has to notice the connection and act on it.

- **R3, single sync behaviour.** After two peers sync, both hold the same record, and the central server counts as a peer too. One behaviour covers every pair, and a tablet passes on what it received as readily as what its own crew recorded, so a report can reach a crew that never met its author.

- **R4, no silent loss.** No report is lost. This shapes how reports are stored and exchanged, and is expensive to change once tablets are in the field. Section 3's architectural decisions cover how.

- **R5, P2P mode under the users' control.** Two tablets only sync once both crew leaders switch P2P mode on. Tablets must find and trust each other on request, which rules out exchanging data in the background whenever two tablets pass each other.

- **R6, zones.** A tablet subscribes to the zones within a radius of its position, or to zones assigned to it, and receives only reports from those zones. Zones do not limit P2P mode, where two tablets exchange everything they hold.

### 2.7 Quality Attributes

Several quality attributes were prioritised while designing the system. Each is a property of how the system behaves, not a function it offers, and each has a target the proof of concept can measure.

- **Availability under partition.** Nothing is withdrawn when the connection goes. A tablet offers what it offers when connected for a full twelve-hour operational period, even offline. A tablet that needs the server for its map is useless below a ridge, which is what R1 pays for.

- **Reliability.** A report the tablet has accepted survives a crash, a flat battery, and a sync cut off halfway, the system never discards it. The target is zero loss across the interruptions the experiments inject, since nobody can ask for a report they never knew was made.

- **Scalability.** The volume a tablet receives stays roughly constant as an incident grows from one crew to dozens, and the central server absorbs the shift-change surge, when a whole division comes back into coverage within minutes carrying twelve hours of reports each. These are the targets the beta phase measures (Section 2.8).

- **Compatibility across versions.** Tablets and servers one version apart still sync. Agencies upgrade on their own schedule, so mutual aid puts different-vintage devices on the same fire, and sync cannot assume both sides are current.

These attributes fight each other sometimes. Compatibility across versions holds back Scalability, because the server cannot always pick the fastest sync path, it also has to work with an older tablet still in the field. Reliability and Availability under partition do not fight each other though. They come from the same idea: a tablet keeps its own full copy, so it never needs the server to stay useful.

### 2.8 Roadmap and Pricing

To make the future concrete, we laid it out as a roadmap of four phases over a horizon of one to two years. The roadmap carries no dates, since development time is impossible to estimate without knowing team size, so it names what each phase delivers rather than when it ships.

The **proof of concept** is the phase we build during this course. It is deliberately narrow: the offline map, syncing with the central server, and P2P mode between two tablets. It is not deployed to anyone, its purpose is to show that a tablet stays useful with no connection and that two tablets can reconcile what they did apart.

The **minimum viable product** puts the system in the hands of one fire service, on real incidents alongside the radio. It adds the first working interface: a map for logging and reading reports, and zones, so a crew receives what belongs to the ground it works on. A decision point sits before this phase, shown in Figure 4: field trials have to show a crew leader can log a report quickly with gloves on and a fire in front of them, otherwise the interface gets reworked first.

The **beta** widens the same fire service from one crew to a whole incident: every crew, their division leaders, and command staff reading the incident picture. Where the MVP shows the system works for the crew that carries it, the beta shows it holds under a full operational period of reports at once, the first point zones and sync face real load.

**Release 1.0** adds mutual aid across agencies and a reworked interface on both fronts: the tablet map crews use, and a web client for command staff at a desk. Two fire services on the same fire each run their own server, and reports from one agency's tablets must reach the other's crews, whose devices may be on a different version. This is where the system starts serving an incident rather than one organisation, and where licensing and support begin.

<figure>
  <img title="" src="../report/figures/roadmap.png" alt="delivery roadmap diagram">
  <figcaption><em>Figure 4: Delivery roadmap. Only the proof of concept is built during this course.</em></figcaption>
</figure>

Fire services will not hand live crew positions to a server we run, so the system is sold as a licence deployed on the customer's own infrastructure. Of the models in [<a href="#7">7</a>] this is a subscription, but an annual one for self-hosted software, matching how self-hosted enterprise products have moved from perpetual licences to yearly terms [<a href="#10">10</a>]. It is priced per agency, not per tablet, since tablet count swings with the season and with mutual aid. The MVP is free, so a fire service can run it for one season without procurement, and the licence begins at release 1.0. Annual support is priced as a percentage of the licence fee, in line with common industry practice [<a href="#9">9</a>]. Deployment and integration are charged separately.

Every agency hosts its own server and upgrades on its own schedule, so release 1.0's mutual aid has to work between different versions, which falls on R3. Crews, command staff, and fire services all gain from the system.

### 2.9 Ethical and Legal Considerations

- Automation bias. A commander who sees a report marked "merged successfully" may stop treating it as provisional. Since the system's whole premise is reconciling conflicting edits without asking a human every time, the UI must keep showing that a report was auto-merged, not just that it was saved. Presenting a merge as more certain than it is would undermine the trust the project depends on, and the solution must never lose information that might be safety critical.
- False or spoofed reports. A system built to keep working through network chaos is also an attractive target for someone who wants to inject false data during that same chaos. Access must be safeguarded by modern user/device authentication.
- Privacy of people named in reports. Reports tied to a location may include personal data such as injuries, missing persons, or property loss. This is sensitive personal data under regulations like the GDPR, which constrains retention, who can access merge history, and how long pre-merge versions should be kept.
- Equity across device capability. Not every field worker or agency has the newest hardware. The implementation must be lean and performance-optimised so it runs on a wide range of devices.

## 3. Architecture

### 3.1 Components

A bounded context is where one specific domain model and its own language apply [<a href="#7">7</a>]. We split the system into four bounded contexts, all ours to build, plus external systems we do not control.

**Field Reporting** is our core domain, it owns what a report and an incident are, and how concurrent edits get merged. This is the one genesis-stage piece in Wardley terms (the Wardley Map, Section 2.4). **Synchronization** moves events between devices and the server, generic enough for both the server path and the peer-to-peer path. **Zone Subscription** maps report locations to H3 hexagons to route server-path deliveries, it has no role in peer-to-peer mode, by design. **Device Identity** gives each tablet a stable id, used in every event's causal stamp.

*External, not built in this proof of concept*: a future fire-authority record system, a weather feed, and the regional or cloud layer above the ICP server (Section 2.2).

| Upstream | Downstream | Relationship |
|---|---|---|
| Field Reporting | Synchronization | Customer/Supplier, Field Reporting defines what needs merging. |
| Field Reporting | Zone Subscription | Customer/Supplier, only report locations are read. |
| Zone Subscription | Synchronization | Customer/Supplier, server path only. |
| Device Identity | Field Reporting, Synchronization | Customer/Supplier, both consume a stable device id. |
| Field Reporting | External systems | Anti-corruption layer, once built, to shield our model from theirs. |

*Table 3.1: Bounded context dependencies.*

<figure>
  <img title="" src="../report/figures/context-map.png" alt="bounded context diagram">
  <figcaption><em>Figure 5: Bounded contexts and their dependencies.</em></figcaption>
</figure>

### 3.2 Event-Based Domain Model

Table 3.2 lists the events an event-storming pass over the four scenarios in Section 2.5 turns up. For brevity: Offline (logging without a connection), Connected (working with a connection), Meeting (two crews meeting), Reconnect (coming back into coverage).

| Event | Scenario | Operation |
|---|---|---|
| Report created | Offline | `CREATE` |
| Report field updated | Offline, Meeting | `SET_FIELD` |
| Incident created | Connected | `CREATE` |
| Incident status changed | Connected | `SET_FIELD` |
| Field marked conflicted | Meeting | derived |
| Conflict resolved by a crew leader | Meeting | `RESOLVE_CONFLICT` |
| Tablet reconnected to server | Reconnect | triggers sync |
| Tablet synced with server | Reconnect, R2 | sync completes |
| Two tablets synced in P2P mode | Meeting | sync completes |
| Position mapped to zones | R6 | derived |
| Report routed to subscribed tablets | Connected, R6 | delivery |

*Table 3.2: Domain events.*

### 3.3 Elaborating Field Reporting

Field Reporting carries our hardest problem: correctly merging concurrent offline edits.

**Ubiquitous language.** We built our own team terminology for the domain concepts [<a href="#7">7</a>]. An **Incident** is one wildfire response effort. A **Report** is one reportable thing inside an incident. An **Event** is one immutable write to one field, by one device. A **dot** pairs a device id with that device's own local sequence number, and the **causal context** stores what a device already has seen, as a map from device id to counter, together they form the Dotted Version Vector (DVV) our merge relies on. A **Tier A field** merges by itself, automatic, no person needed. A **Tier B field** has no safe automatic merge and stays for a crew leader to resolve. A **conflict** happens when two events touch the same Tier B field and neither device knew about the other.

**Tactical DDD.** The entity **Incident** has an immutable `incidentId`. Its `name` is Tier A with a fixed tiebreak rule. Its `status` is Tier B, since a status change is a judgement call, not something automatic. The entity **Report** has immutable `reportId` and `incidentId`. Its `reportType` is Tier A, merged by set union. Its `severity` is Tier A too, merged by taking the maximum. Its `location` and `description` stay Tier B for manual resolution. **Event** is its own category, with `eventId`, `operation` (`CREATE`, `SET_FIELD`, or `RESOLVE_CONFLICT`), `field`, `value`, `dot`, `causalContext`, and an informational `supersedes` field. **Dot** and **causalContext** are value objects, together the DVV. **MergeService** applies the Tier A/B policy. Every Tier A policy is deterministic, commutative, associative, and idempotent, the CRDT convergence properties, so every device converges regardless of arrival order. On the tablet, the Report and Event repositories are SQLite repositories over the shared schema (`ReportRepository`, `EventRepository`), the Incident one is not built yet. We use no Factory, an Incident or Report is simply created by its first `CREATE` event.

Concurrent resolutions of the same Tier B conflict become a new conflict under the same mechanism, no special case needed.

<figure>
  <img title="" src="../report/figures/field-reporting-classes.png" alt="field reporting class diagram">
  <figcaption><em>Figure 6: Field Reporting's entities, Event record, and MergeService.</em></figcaption>
</figure>

### 3.4 Container View

The container view opens up the Firefly box from the context view (Figure 1). Firefly runs as five parts, plus the merge code they share.

- **Tablet app.** One per crew, held by the crew leader. It is a native Android app (Kotlin, Jetpack Compose), because it has to work for a whole shift with no connection and needs Bluetooth and WiFi for P2P mode. It keeps a full copy of the record in a local SQLite store and never needs the server to stay useful (R1).
- **Message broker.** NATS JetStream carries everything between the tablets and the server. Tablets publish their events on the `operation-events` stream, one subject per report (`events.operation.<reportId>`), and the server publishes merged reports on a second stream, under `report.<hexagon>.<reportId>`. JetStream keeps messages until they are read and ignores a message whose id it has already seen. That id is the event's dot, which means a tablet can resend after a sync was cut off without creating duplicates. Tablets reach it over cellular.
- **ICP server.** A Spring Boot service on the machine at the Incident Command Post. It merges incoming events, works out which hexagon each report is in, assigns hexagons to tablets, and enrols new tablets.
- **Server store.** PostgreSQL, with the same logical schema as the tablet store: every event, plus the reports and incidents derived from them.
- **Command web client.** A browser app for command staff at the ICP. It shows the whole incident instead of a few hexagons. It reads the same report stream as the tablets, subscribed to every hexagon (`report.>`), through NATS over WebSocket since a browser cannot open a plain NATS connection.

The merge code lives in a shared library, `core`, used by both the tablet app and the server. It runs inside both containers, and it is why both sides end up with the same record: they run the same code on the same events (R3).

Everything between a tablet and the server goes through the broker, so a tablet can hand off its events even while the server is busy. The link between two tablets does not touch the ICP at all, P2P mode only needs both crew leaders to switch it on (R5).

<figure>
  <img title="" src="../report/figures/c4-container.png" alt="C4 container diagram">
  <figcaption><em>Figure 7: C4 container view.</em></figcaption>
</figure>

### 3.5 Component View

Most of the logic sits in the tablet app and the ICP server, so those are the two we open up.

**Tablet app** (Figure 8). The screens only talk to `ReportService`, the one way to change a report. It turns every change into an event, stamps it with the tablet's next dot and its causal context, and stores it together with the derived report in one transaction. A crash cannot leave the two out of step. The local store holds the events, the replica state (the tablet's id, next sequence number and causal context), and the derived reports. The sync client sends the events the broker has not acknowledged yet, and reads the reports for the tablet's hexagons. `P2pManager` finds the other tablet over Nearby Connections. Both hand what they receive to `ReportService`, which merges it with the merge library. An event from the server and one from another tablet take the same path.

<figure>
  <img title="" src="../report/figures/c4-component-tablet.png" alt="tablet app component diagram">
  <figcaption><em>Figure 8: Components of the tablet app.</em></figcaption>
</figure>

**ICP server** (Figure 9). The event consumer reads the `operation-events` stream. `MergeService` applies each event with the same merge library and stores it through the event repository. The zone service does the H3 work: the hexagon of every report and, from a tablet's position, the hexagons that tablet gets. The report publisher puts merged reports and hexagon assignments back on the broker. Enrolment, left out of the figure, gives each tablet its id when it is set up at the ICP, the id every dot carries.

<figure>
  <img title="" src="../report/figures/c4-component-server.png" alt="ICP server component diagram">
  <figcaption><em>Figure 9: Components of the ICP server.</em></figcaption>
</figure>

Not all of this is built yet. The proof of concept has the screens, `ReportService`, the local store and `P2pManager` on the tablet, a sync client that sends single severity changes, and the event consumer with a `MergeService` that only logs on the server. The zone service, report publisher and the report stream are still to do, and so is enrolment, the tablet generates its own id for now. The command web client is not built either.


### 3.6 Code View

The C4 code level would zoom into single classes. We skip it apart from Field Reporting, whose classes Figure 6 already shows. The rest maps one to one onto the components above and is easier to read in the repository than in a diagram.

### 3.7 Runtime Views

We follow the scenarios from Section 2.5. The offline one is split in two, since logging and syncing happen at different times.

**Logging without a connection** (Figure 10). Alice's crew loses coverage and she marks a spot fire. `ReportService` stores it as events with her tablet's next dots, and nothing waits on a connection.

<figure>
  <img title="" src="../report/figures/runtime-offline.png" alt="logging offline sequence diagram">
  <figcaption><em>Figure 10: Logging without a connection.</em></figcaption>
</figure>

**Back into coverage** (Figure 11). When cellular coverage returns, the sync client sends every unsent event on `events.operation.<reportId>`, with its dot as message id, and marks it sent once the broker acknowledges it. If the connection drops halfway, the next attempt sends the rest again and the broker drops what it already has. The server merges each event, stores it, and publishes the merged report under its hexagon, where every tablet reading that hexagon picks it up.

<figure>
  <img title="" src="../report/figures/runtime-reconnect.png" alt="back into coverage sequence diagram">
  <figcaption><em>Figure 11: Back into coverage.</em></figcaption>
</figure>

**Two crews meeting** (Figure 12). Alice and Bob both switch on P2P mode and their tablets connect over Nearby Connections. Each sends the events the other has not seen, which it can tell from the other's causal context, and both merge. Tier A fields like severity settle on their own. A Tier B field changed on both tablets keeps both values, shown with who made each and when. Once Alice and Bob agree, the choice goes out as a `RESOLVE_CONFLICT` event, which merges like any other.

<figure>
  <img title="" src="../report/figures/runtime-p2p.png" alt="P2P meeting sequence diagram">
  <figcaption><em>Figure 12: Two crews meeting in P2P mode.</em></figcaption>
</figure>

**Getting the reports for a zone** (Figure 13). A tablet publishes its position on `position.<deviceId>`. The zone service takes the hexagons within its radius, larger for division leaders, plus any hexagons assigned to it, and the report publisher sends the list on `assign.<deviceId>`. The tablet then requests `report.<hexagon>.*` for each hexagon, which returns the latest version of every report in it, and keeps reading those subjects for new ones. Since the server picks the hexagons, an assignment can change what a tablet sees without the tablet moving.

<figure>
  <img title="" src="../report/figures/runtime-zones.png" alt="zone update sequence diagram">
  <figcaption><em>Figure 13: Getting the reports for a zone.</em></figcaption>
</figure>

### 3.8 Deployment View

Firefly does not run in the cloud (Section 2.2). The server side runs on one physical machine at the Incident Command Post: the broker, the ICP server and PostgreSQL, started together with Docker Compose. Tablets reach that machine over cellular when they have coverage and keep working offline when they do not. Command staff open the web client in a browser on a computer at the post, over the local network. Two tablets in P2P mode talk directly over Bluetooth or WiFi and never go through the ICP.

Tablets need Android 14 or newer, since the tablet store uses SQLite's STRICT tables, which Android only ships from version 14.

If the machine at the ICP goes down, only the server path stops. Tablets keep working and keep syncing with each other, and they catch up with the server once it is back, since every tablet still holds all of its events.

<figure>
  <img title="" src="../report/figures/c4-deployment.png" alt="C4 deployment diagram">
  <figcaption><em>Figure 14: C4 deployment view.</em></figcaption>
</figure>

### 3.9 Architectural Decisions

**We chose a two-tier merge policy instead of one single policy.** An always-automatic policy can silently pick a value nobody chose, hiding a real disagreement. An always-manual policy makes a leader step in on every sync, even for a safe field like severity. Splitting fields by tier gives us both: nothing gets dropped, and only fields where a wrong automatic choice matters need a person.

**Sync has two directions, and both go through NATS.** A tablet sends its events to NATS when it gets a connection. The server reads them, merges them, and saves the result (Section 4.1). The server then sends each merged report back through NATS. A tablet receives only the reports for its own hexagon and the ones around it (Section 4.2).

### 3.10 How This Connects to Chapter 2

Our bounded contexts match the stakeholders from Section 2.3. Field Reporting serves the crews (Offline, Meeting, and Reconnect scenarios). Zone Subscription serves division leaders and command staff (Connected scenario, R6). Table 3.2 models the same four scenarios from Section 2.5 as a domain, not as narrated steps. The Tier A/B split is exactly what R4 needs at the data level, no report is lost, which is the "how" R4 itself points here for.

## 4. Architectural Decision Records

This chapter summarises three decisions: how events get from a tablet to the server, how reports get back, and how these events are authenticated. Each is recorded in full as its own ADR, linked below.

### 4.1 ADR 1: How Events Travel from Device to Server

Getting events from a tablet to the server has to survive a sync cut off halfway, and a whole division reconnecting within minutes of each other at shift change, without the server becoming the bottleneck. We compared sending events straight over HTTP, writing directly to the database, and two message logs, Kafka and NATS JetStream. We chose NATS JetStream: a tablet gets a confirmation once its events are safely on disk and does not wait for the server to merge them, which is what makes shift change survivable, and it is small enough to run as one more program on the ICP's single machine, unlike Kafka. Full ADR: [How Events Travel from Device to Server](adrs/adr1.md).

### 4.2 ADR 2: How Reports Travel from Server to Device

Delivering new reports back to a tablet has to arrive quickly, without draining a twelve-hour battery, and keep working through the drops typical of a fire. We compared short and long polling, Server-Sent Events, and WebSocket. We chose WebSocket over the NATS connection the tablet already holds for sending events (ADR 1): it reuses a connection that already recovers from drops, rather than opening and maintaining a second one just for the other direction. Full ADR: [How Reports Travel from Server to Device](adrs/adr2.md).

### 4.3 ADR 3: How Events Are Authenticated

Firefly has to rule out false or spoofed reports without slowing a crew leader down in the field, and whatever proves a report's origin has to work fully offline, for both the server path and peer-to-peer sync. We weighed an IP/MAC whitelist with a shared secret, a per-device symmetric key with an HMAC per event, mutual TLS on the server connection, and signing each event with a device key pair. We chose per-event signing: it is the only option that covers both sync paths with one mechanism, and removing a lost or compromised device just means removing its public key, with nothing to change on any other tablet. Full ADR: [How Events Are Authenticated](adrs/adr3.md).

## 5. Architectural Experiments

Two architectural decisions rest on what we expect of the radio and of the data rather than on measurements, and the proof of concept can measure both. We designed one experiment for each, so that the results in the Swan release either confirm the decision or change it. For the Heron release the designs are complete and the measurements on the devices are still to be taken. Appendix B gives the parameters in detail.

### 5.1 Experiment 1: Peer-to-peer sync between two real tablets

The decision under test is the use of Nearby Connections as the peer-to-peer transport. We preferred it over raw Bluetooth and Wi-Fi Direct because it combines Bluetooth, BLE, and Wi-Fi behind one interface [<a href="#18">18</a>], but that reasoning comes from documentation and not from measurement. It matters for R1, R4, and R5, since two crews may stay together for only a few minutes, and a link that drops halfway must neither lose nor duplicate a report. The question is whether a sync finishes while the crews are still together, and whether an interrupted sync leaves both tablets with exactly the events they should hold.

Two Samsung devices from the team run the proof-of-concept app, and only physical devices are used, because in our first tests two emulators found each other but a phone and an emulator did not. A first manual test connected a Galaxy S25 Ultra and a Galaxy Tab S8+ and sent a severity change between them, while a Galaxy S25 Ultra and a Galaxy S24 did not connect when they were not close to each other. In a first walking test the S24 sent severity changes while moving away in clear line of sight, and they kept arriving at the S25 Ultra until the phones were about 50 to 60 metres apart (our estimate, not a measurement). In the last moment before the connection was lost, the S25 Ultra still showed the link as connected. So distance is one of the variables, and we will measure where delivery stops (Appendix B). One device sends 100, 1,000, and 10,000 generated events at one and ten metres, and some runs are interrupted halfway. We measure the time to connect, the time until the receiver has stored the last event, and the events lost and duplicated.

We keep Nearby Connections if at least 19 of 20 runs at ten metres finish within one minute for 1,000 events, a limit we chose because crews cannot be expected to stand together for longer, and if no interrupted run loses or duplicates an event. Otherwise we first add resending based on the dots and then test Wi-Fi Direct directly.

### 5.2 Experiment 2: The cost of a sync as the event log grows

Section 3.3 defines the causal context, and the sync between two tablets (Section 3.7) sends only the events the other tablet's causal context does not cover. The simpler alternative sends the whole event log and lets the receiver ignore what it already holds, which is safe because applying an event twice changes nothing. The first design sends less but needs an extra round trip and more code, so the question is whether it is worth them. The choice bears on R3 and on the scalability target that the load on a tablet stays roughly constant as an incident grows (Section 2.7).

We sync generated pairs of logs of 100 to 100,000 events per tablet, with 0 to 99 percent of the events held by both, using each design, and measure the bytes sent, the time the receiving tablet needs to store the events in SQLite, and whether both tablets end with identical logs. A first run of the byte counts on a laptop, with 10,000 events per tablet, shows about two thirds fewer bytes for the causal-context design at 50 percent overlap and more than 99 percent fewer at 99 percent overlap, against about 2 MB for the whole log. Whether that matters depends on the transfer rate from Experiment 1 and on the storing time on a tablet, which are still to be measured.

We keep the causal-context design if the whole log takes longer than one minute at 10,000 events for some overlap, or if the cost on the receiving tablet grows with the whole log instead of with the number of new events. If the whole log stays within the limit everywhere, we drop the extra round trip and keep the simpler design.

### 5.3 Validity and next steps

Both experiments use generated events, which are more uniform than real reports, and one pair of Samsung devices, so the results describe this hardware and not every tablet an agency might own. Ten metres in line of sight is also the optimistic case on a fireground. We therefore read a pass as evidence that a decision is not wrong rather than as proof. The measurements on the devices run between the Heron and Swan releases, and their results go into the Swan report.

## References

<a id="1">[1]</a> Los Angeles Fire Department, "Palisades Fire After-Action Review Report," Lafd.org, Oct. 8, 2025. https://lafd.org/sites/default/files/pdf_files/Palisades_AARR_10_08_2025_final.pdf (accessed Sep. 21, 2026)

<a id="2">[2]</a> Firehouse, "LAPD: Lack of Unified Command Led to Chaos During Palisades Fire," Firehouse.com, Nov. 2025. https://www.firehouse.com/california-fire-storm/news/55328119/lack-of-unified-command-led-to-chaos-during-palisades-fire-probe-shows (accessed Sep. 20, 2026)

<a id="3">[3]</a> Santa Monica Daily Press, "LAFD report calls for major changes after devastating Palisades Fire," Smdp.com, Oct. 2025. https://www.smdp.com/lafd-report-calls-for-major-changes-after-devastating-palisades-fire (accessed Sep. 20, 2026)

<a id="4">[4]</a> Wikipedia, "2002 Überlingen mid-air collision," Wikipedia.org. https://en.wikipedia.org/wiki/2002_%C3%9Cberlingen_mid-air_collision (accessed Sep. 20, 2026)

<a id="5">[5]</a> The Local, "Opinion: As French wildfires still burn, the blame-the-Greens deflection has begun," Thelocal.fr, Aug. 5, 2026. https://www.thelocal.fr/20260805/opinion-as-french-wildfires-still-burn-the-blame-the-greens-deflection-has-begun (accessed Sep. 20, 2026)

<a id="6">[6]</a> Notre-Planète, "Incendies de juillet 2026 en Gironde : le plus vaste feu de forêt en France depuis 1949," Notre-planete.info, Aug. 12, 2026. https://www.notre-planete.info/actualites/506-feux-foret-incendie-Gironde-juillet-2026 (accessed Sep. 20, 2026)

<a id="7">[7]</a> A. van Deursen, "Problem Analysis, Part II," CS4505 Software Architecture, Lecture 5, Delft University of Technology, Sep. 2026.

<a id="8">[8]</a> A. Cockburn, *Writing Effective Use Cases*. Boston, MA: Addison-Wesley, 2001.

<a id="9">[9]</a> SolTech, "Software Support and Maintenance Costs." Soltech.net. https://soltech.net/software-support-and-maintenance-costs/ (accessed Sep. 21, 2026)

<a id="10">[10]</a> Licenseware, "Software Vendor Price Increases in 2025." Licenseware.io. https://licenseware.io/software-vendor-price-increases-in-2025/ (accessed Sep. 21, 2026)

<a id="11">[11]</a> A. van Deursen, "Problem Analysis, Part I," CS4505 Software Architecture, Lecture 2, Delft University of Technology, Sep. 2026.

<a id="12">[12]</a> Confluent, "Confluent Platform System Requirements." Docs.confluent.io. https://docs.confluent.io/platform/current/installation/system-requirements.html (accessed Oct. 11, 2026)

<a id="13">[13]</a> Confluent, "Confluent REST Proxy for Apache Kafka." Docs.confluent.io. https://docs.confluent.io/platform/current/kafka-rest/index.html (accessed Oct. 11, 2026)

<a id="14">[14]</a> NATS, "JetStream." Docs.nats.io. https://docs.nats.io/nats-concepts/jetstream (accessed Oct. 11, 2026)

<a id="15">[15]</a> NATS, "What is NATS." Docs.nats.io. https://docs.nats.io/nats-concepts/what-is-nats (accessed Oct. 11, 2026)

<a id="16">[16]</a> NATS, "Connect over WebSocket." Docs.nats.io. https://docs.nats.io/running-a-nats-service/configuration/websocket (accessed Oct. 11, 2026)

<a id="17">[17]</a> NATS, "Authenticating with an NKey." Docs.nats.io. https://docs.nats.io/running-a-nats-service/configuration/securing_nats/auth_intro/nkey_auth (accessed Oct. 11, 2026)

<a id="18">[18]</a> Google, "Nearby Connections overview," Developers.google.com. https://developers.google.com/nearby/connections/overview (accessed Oct. 11, 2026)