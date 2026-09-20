<div align="center">

### CS4505 Software Architecture Finch Report
# Firefly

**Group 67**

Rares Andrei Popa

Stefan Claudiu Avram

Alexandru Cristian Cazacu

Mihai Ciorascu

Radu Zidaru

</div>

## Table of Contents

TODO

## 1. Introduction

In today's world, climate change has made once-in-a-lifetime natural disasters a common occurence. First responders have to deal with situations that greatly exceed normal operations, in hostile environments where connectivity is unreliable or absent for extended periods of time. Supervisors coordinating relief efforts deal with immense workloads in contexts in which human life is at stake, made worse by the fact that the same communication channels used to issue orders are also carrying field reports up the command chain. This problem, and how it has broken down in the past, is explored more throughly in Chapter 2.

To relieve part of this workload, we propose Firefly, a mobile and web application that lets emergency personnel record and edit structured reports about an ongoing incident directly from the field. The system is offline-first: personnel can create and edit reports with no connectivity at all, and a backend reconciles conflicting edits once connectivity is restored. Because more than one person may edit the same report while both are offline, a thorough conflict-resolution and merging policy was designed and evaluated specifically to avoid discarding safety-critical information. Furthermore, to ensure that emergency personnel have access to the most complete and recent information, even in low connectivity scenarios, we included peer-to-peer synchronization between devices deployed in the field that enter eachother's range.

The architecture of the proposed system is explored in Chapter 3, and the results of a varied set of experiments are outlined in Chapter 4. The future roadmap is discussed in Section 2.7.

## 2. Problem and Context Analysis

### 2.1 Problem Description

During large-scale natural disaster response, coordination runs primarily through a single radio channel, used both to relay information up the chain and to issue orders back down. [<a href="#1">1</a>][<a href="#2">2</a>] Under pressure this channel becomes overcrowded, leading to mismanagement and other failures: congested radio frequencies and a fractured command structure during the Pacific Palisades Fire (2025) [<a href="#3">3</a>][<a href="#4">4</a>][<a href="#5">5</a>], and a public dispute over resource allocation during the Gironde wildfires in France (2026), though no official review has yet confirmed its cause. [<a href="#7">7</a>][<a href="#8">8</a>]

The same overload falls on the small group of people responsible for coordinating a response, and overworking them can have disastrous consequences: in the 2002 Überlingen mid-air collision, a single air traffic controller managing two positions alone failed to separate two aircraft in time, killing all 71 people aboard. [<a href="#6">6</a>]

We therefore introduce Firefly, a system that lets field workers share incident reports even when the usual communication channels are overloaded, merging conflicting or overlapping reports in cases where this is achievable without loss of information, in order to help reduce and optimize effort during a natural disaster response.

### 2.2 Context View

The Context View shows the boundary between the system and its environment. As the lecture says it, "you design the system, the context is a given" [<a href="#14">14</a>].

Firefly's scope is to coordinate field reports, so every device end up with the same, complete record, connected or not. The boundary is the Incident Command Post: the server is a physical machine at the ICP, not a cloud endpoint, and going beyond it is future work. Voice radio and where to send a crew stay human decisions, outside our scope. Our job is smaller: make sure command staff act on one complete record, not on whatever partial picture arrived first.

Crew leaders use tablets to record offline by default. Syncing happens automatic when a device reconnects, to the ICP or to another tablet. Division leaders and command staff see the current zone-scoped record through the ICP server. When two crews meet, their leaders can opt in to peer-to-peer sync. Since both leaders must agree, tablets never exchange data without the crews knowing it.

Two tools already used in wildfire response help show this problem. ArcGIS Field Maps, used by wildland agencies today, resolves concurrent edits with last-write-wins, and their own fix for real conflicts is manual, done by a database administrator after the fact. ATAK with goTenna or Meshtastic gives device-to-device communication with no cell connection needed, but what it sends is append-only, so a conflict can never happen. Our problem, concurrent offline edits with no automatic way to merge them, sits exactly in this gap between the two tools.

We depend on GPS accuracy for zone subscription, covered more in the Wardley Map below and in Section 3.1. This we have to tolerate, not assume. A fire-authority record system and a weather feed are future work, named here only so the figure stays accurate.

<figure>
  <img title="" src="../report/figures/context-view.png" alt="context view diagram">
  <figcaption><em>Context view. The system boundary is the ICP local server. Regional/cloud reach and external record systems are dashed, future work only.</em></figcaption>
</figure>

### 2.3 Stakeholders

Stakeholders fall into five categories. Field workers (firefighters, crew leads) create and edit reports and have the most direct daily contact with the system, but hold no formal power over its design or deployment. Command and decision-making staff, like incident commanders or EOC (Emergency Operations Center) personnel consume the merged view and are the primary reason the system exists. The owning agency, such as a regional fire or other emergency management service controls procurement, deployment, and data governance. Above them sit the governement and regulatory bodies, such as national ministries or civil authorities and data protection regulators. They have high power, set policy, funding, and compliance requirements without using the system themselves, and are not always aligned with the owning agency, as the resource-allocation dispute between Gironde's fire service and France's Interior Ministry proves. The platform developers design and improve the system. Indirectly affected parties, such as civilians named in reports, and any data-protection or oversight body, have a stake in how the system behaves without using it directly.

Plotting the each stakeholder in a Power/Interest grid helps to shed light on what aspects of our solution deserve the highest amount of focus and highlights any possible conflicts.

<figure>
  <img title="" src="../report/figures/stakeholder_power_interest_grid.png"  alt="power-interest grid diagram">
  <figcaption><em>Figure 2: Power-Interest Grid for Stakeholders</em> </figcaption>
</figure>

### Wardley Map

<!-- Placeholder heading, not numbered: 2.4-2.8 are all now taken by Alex's
and Stefan's real sections, so no clean slot exists. Give it a number once
the team settles final section order. -->

A Wardley map puts the user and the key system components on two axes: value visible to the user, and evolution stage of the component, from Genesis, "uncertainty about everything, including functionality," through Custom built and Product, to Commodity, "as standard as water and electricity" [<a href="#14">14</a>]. The map itself is not the point, what it shows us is where our effort should go.

Field Reporting is the merge logic that solves concurrent offline edits (Chapter 3). It sits alone in the genesis, high-value quadrant. No existing tool can merge concurrent edits correctly for this problem (Section 2.2), and this is what decides if Firefly's core promise, no edit ever disappears, actually holds. Zone Subscription is still fairly custom, we chose its radius-based H3 subscription on purpose to avoid an edge-of-hexagon blind spot, but it gives less direct value than the merge logic does. Synchronization sits closer to the middle, custom-built for us but kept free of wildfire-specific logic, so it stays more reusable. Device Identity, the tablet hardware and operating system, and the local WiFi or Bluetooth stack all sit near commodity. These are well-understood components, we can just treat them as given instead of building them ourselves.

<figure>
  <img title="" src="../report/figures/wardley-map.png" alt="wardley map diagram">
  <figcaption><em>Wardley map of the system's main components. Field Reporting is the one genesis-stage, high-value component, matching its identification as the core domain in Chapter 3.</em></figcaption>
</figure>

### 2.4 Example Scenarios

To show how the system works in practice, this section presents four scenarios at user-goal level [<a href="#10">10</a>]: a crew working with a connection, a crew logging without one, two crews meeting during a shift, and a tablet coming back into coverage. These scenarios cover the requirements that shape the architecture. The remaining requirements are in Appendix A, as user stories.

- **Working with a connection**: Alice, who leads a crew on the eastern flank, marks a blocked access road on her tablet, and it shows up on the command staff's map at the Incident Command Post. While she works, reports from the crews near her appear on her own map without her asking for them, each one naming the crew that sent it and the time they sent it. When the command staff assign her crew for the coming shift, the assignment arrives the same way, although anything urgent still reaches her over the radio. What the crews on the far side of the fire report never lands on her tablet, since it is of no use to her.

- **Logging without a connection**: Alice's crew moves below a ridge and loses coverage, from where she now sees a spot fire across the containment line; it calls it in on the radio, and then marks it on her map, noting where it is, how big it is and which way it is moving. Her tablet keeps the report with her name and the time on it, and she carries on logging through the afternoon from the map she had when the signal went.

- **Two crews meeting**: Alice's crew runs into Bob's crew later that day, and neither of them has had a connection since the morning. They both switch on P2P mode, each tablet shows the other crew, and once the sync is over each of them sees a summary of what came in and what went out. Almost everything lands on both maps, excepting conflicting logs. Take the example of one specific roadt: Alice had marked it blocked and Bob had marked it open. Both tablets show the road with both reports on it, who made them and when, and Alice and Bob settle it between themselves before their crews split up again. This way we ensure that neither report is thrown away.

- **Coming back into coverage**: Bob drives back towards the staging area and his tablet picks up a signal, where it syncs with the central server. After the sync, everything his crew logged that day, along with what he took from Alice, reaches the command staff. In return his tablet takes in the reports it missed for the zones around him.

### 2.5 Architecturally Significant Requirements

As not every requirement shapes the architecture, we treat a requirement as architecturally significant if it needs dedicated components, is expensive to change later, or affects (i.e. enables, blocks) future functions [<a href="#9">9</a>]. Six requirements meet at least one of these criteria.

- **R1, working without a connection.** A tablet can store new reports, update existing ones and sync in P2P mode while offline. Each tablet therefore keeps its own copy of the record instead of reading one from a server. It is also the hardest requirement to add later: assume a live connection first, and removing that assumption means designing the system twice.

- **R2, syncing with the central server.** When a tablet has a connection it syncs on its own, sending its reports and receiving the updates and assignments for its zones. Unlike P2P mode this happens without anyone asking, so a component has to notice the connection and act on it.

- **R3, single sync behaviour.** After two peers have synced, both hold the same record, and the central server counts as a peer. One behaviour covers every pair that syncs, and a tablet passes on what it received as readily as what its own crew recorded, so a report can reach a crew that never met its author.

- **R4, no silent loss.** No report is lost. That shapes how reports are stored and exchanged, and it is expensive to change once tablets are in the field. The architectural decisions in Section 3 cover how.

- **R5, P2P mode under the users' control.** Two tablets only sync when both crew leaders have switched P2P mode on. Tablets must find and trust each other on request, which is a component in its own right, and this rules out exchanging data in the background whenever two tablets pass each other.

- **R6, zones.** The central server subscribes each tablet to the zones within a radius of its position, or to zones it assigns, and sends only the reports from those zones. The server therefore tracks positions and subscriptions, which decides what every sync with it contains. Zones do not limit P2P mode, where two tablets exchange everything they hold.

### 2.6 Quality Attributes

Several quality attributes were prioritised while designing the system. Each is a property of how the system behaves rather than a function it offers, and each is stated with a target the proof of concept can measure.

- **Availability under partition.** Nothing is withdrawn when the connection goes. A tablet that cannot reach the central server offers what it offers when connected, and it does so for a full operational period of twelve hours. A tablet that has to ask a server for the map is useless below a ridge, which is what R1 pays for.

- **Reliability.** A report the tablet has accepted survives a crash, a flat battery and a sync that is cut off halfway, and the system never discards it. The target is zero loss across the interruptions the experiments inject, since nobody can ask for a report they never knew was made.

- **Scalability.** The volume a tablet receives stays roughly constant as an incident grows from one crew to dozens, and the central server absorbs the shift-change surge, when a whole division comes back into coverage within minutes carrying twelve hours of reports each. These are the targets the beta phase measures (Section 2.7).

- **Compatibility across versions.** Tablets and servers one version apart still sync. Agencies upgrade on their own schedule, so mutual aid puts devices of different vintages on the same fire, and the sync behaviour cannot assume that both sides are current.

### 2.7 Roadmap and Pricing

To make the future of the system concrete we have laid it out as a roadmap of four phases, over a horizon of one to two years. The roadmap carries no dates, since development time is impossible to estimate without knowing the size of the team, and following SAFe it names what each phase delivers rather than when it ships [<a href="#11">11</a>].

The **proof of concept** is the phase we build during this course, and it is deliberately narrow: the offline map, syncing with the central server, and P2P mode between two tablets. It is not deployed to anyone. Its purpose is to show that the two riskiest parts hold, namely that a tablet stays useful with no connection and that two tablets which worked apart can reconcile what they did.

The **minimum viable product** puts the system in the hands of one fire service, on real incidents and alongside the radio. It adds the first interface a crew can actually work with: a map on the tablet for logging and reading reports, and zones, so a crew receives what belongs to the ground it is working on. A decision point sits before this phase, shown in Figure 3: field trials have to show that a crew leader can log a report quickly with gloves on and a fire in front of them. If they cannot, the interface is reworked before anything is built on top of it.

The **beta** widens the same fire service from one crew to a whole incident: every crew in the field, their division leaders, and the command staff reading the incident picture. Where the MVP shows that the system works for the crew that carries it, the beta shows that it holds when a full operational period of reports flows through it at once, which is the first point at which zones and sync are under a load worth measuring. The experiments in Chapter 4 target this phase.

**Release 1.0** adds mutual aid across agencies and a reworked interface on both fronts: the tablet map the crews use, and a web client for the command staff, who work from a desk rather than a fire line. Two fire services working the same fire each run their own central server, and reports made on one agency's tablets have to reach the crews of the other, whose devices may be on a different version. This is the point at which the system stops serving one organisation and starts serving an incident, and it is where licensing and support begin.

<figure>
  <img title="" src="../report/figures/roadmap.png" alt="delivery roadmap diagram">
  <figcaption><em>Figure 3: Delivery roadmap. Only the proof of concept is built during this course.</em></figcaption>
</figure>

Fire services will not hand live crew positions to a server we run, so the system is sold as a licence deployed on the customer's own infrastructure. Of the models in [<a href="#9">9</a>] this is a subscription, but an annual licence for software the agency hosts itself, as self-hosted enterprise products have moved from perpetual licences to yearly terms [<a href="#13">13</a>]. It is priced per agency rather than per tablet, since the number of tablets swings with the season and with mutual aid. The minimum viable product is free, so that a fire service can run it for one season without a procurement process, and the licence begins at release 1.0. Annual support follows the licence, commonly 18 to 22 per cent of it, which public buyers negotiate down and cap against escalation [<a href="#12">12</a>]. Deployment and integration are charged separately.

Hosting at the customer decides more than the price. Every agency runs its own server and upgrades on its own schedule, so the mutual aid of release 1.0 has to work between servers of different versions, which falls on R3. Crews, command staff and fire services each gain from the system.

### 2.8 Ethical and Legal Considerations

- Automation bias. A commander who sees a report marked "merged successfully" may stop treating it as provisional. Because the system's entire premise is reconciling conflicting edits where possible without asking a human every time, the UI must keep surfacing that a report was auto-merged, not just that it was saved. Presenting a merge as more certain than it is would undermine the same trust the project is trying to protect. Our solution must never lose information that might be safety critical.
- False or spoofed reports. A system built to keep functioning through network chaos is also an attractive target for someone who wants to inject false situational data during exactly that chaos. Access to this system must be safeguarded by a modern type of user/device authentication.
- Privacy of people named in reports. Reports tied to a location and an incident may name real people or include personally identifiable information such as: injuries, missing persons, property loss. This is sensitive personal data under regulations such as the GDPR, which constrains retention, who can access merge history, and how long conflicting (pre-merge) versions of a report should be kept.
- Equity across device capability. Not every field worker or agency has the newest hardware. The final implementation must be lean and performance optimized such that it can run on a large variety of devices.

## 3. Architecture

### 3.1 Components

A bounded context is where one specific domain model and its own language apply [<a href="#9">9</a>]. We split the system in four bounded contexts, all of them ours to build, plus external systems we do not control.

**Field Reporting** is our core domain. It owns what a report and an incident are, and how concurrent edits get merged. This is the one genesis-stage piece in Wardley terms (the Wardley Map, Section 2). **Synchronization** moves events between devices and the server, it is generic enough to work for both the server path and the peer-to-peer path. **Zone Subscription** maps report locations to H3 hexagons, to route deliveries on the server path, it has no role in peer-to-peer mode, by design. **Device Identity** gives each tablet a stable id, used in every event's causal stamp.

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
  <figcaption><em>Bounded contexts and their dependencies.</em></figcaption>
</figure>

### 3.2 Event-Based Domain Model

Table 3.2 lists the events an event-storming pass over the four scenarios in Section 2.4 turns up. For brevity: Offline (logging without a connection), Connected (working with a connection), Meeting (two crews meeting), Reconnect (coming back into coverage).

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

Field Reporting carries our hardest problem, correctly merging concurrent offline edits.

**Ubiquitous language.** We built our own team terminology for the domain concepts [<a href="#9">9</a>]. An **Incident** is one wildfire response effort. A **Report** is one reportable thing inside an incident. An **Event** is one immutable write to one field, by one device. A **dot** pairs a device id with that device's own local sequence number, and the **causal context** stores what a device already has seen, as a map from device id to counter. Together they make the Dotted Version Vector (DVV) our merge is built on. A **Tier A field** merges by itself, automatic, no person needed. A **Tier B field** has no safe automatic merge, it stays for a crew leader to resolve. A **conflict** is what happens when two events touch the same Tier B field, and neither device knew about the other one.

**Tactical DDD.** The entity **Incident** has an immutable `incidentId`. Its `name` is Tier A, with a fixed tiebreak rule. Its `status` is Tier B, because a status change is a judgement call for a person, not something automatic. The entity **Report** has immutable `reportId` and `incidentId`. Its `reportType` is Tier A, merged by set union. Its `severity` is Tier A too, merged by taking the maximum. Its `location` and `description` stay Tier B, kept for manual resolution. **Event** is its own category, with `eventId`, `operation` (`CREATE`, `SET_FIELD`, or `RESOLVE_CONFLICT`), `field`, `value`, `dot`, `causalContext`, and a `supersedes` field that is only informational. **Dot** and **causalContext** are value objects, together they are the DVV. **MergeService** is the service that applies the Tier A/B policy. Every Tier A policy has to be deterministic, commutative, associative and idempotent, these are the CRDT convergence properties, so every device ends up with the same result no matter the order events arrive in. The Incident, Report, and Event repositories map directly onto the Room DAOs already built. We do not use a Factory, an Incident or a Report is simply created by its first `CREATE` event.

Concurrent resolutions of the same Tier B conflict become a new conflict under the same mechanism, no special case needed.

<figure>
  <img title="" src="../report/figures/field-reporting-classes.png" alt="field reporting class diagram">
  <figcaption><em>Field Reporting's entities, Event record, and MergeService.</em></figcaption>
</figure>

### 3.4 Architectural Decisions

**We chose a two-tier merge policy instead of one single policy.** An always-automatic policy can silently pick a value nobody chose, and that hides a real disagreement. An always-manual policy makes a leader step in on every sync, even for a safe field like severity. Splitting fields by tier gives us both things at once, nothing gets dropped, and only the fields where a wrong automatic choice matters actually need a person.

**We are also splitting sync into two problems, not one.** WorkManager triggers a one-shot sync when connectivity comes back, this is how offline events reach the server. Live zone updates while a tablet is already online need something different, a persistent connection, because long-polling costs too much tablet battery. In practice, a tablet publishes events to a Kafka-style topic, `operation-events`, partitioned by operation ID. A server-side consumer reads this and runs the Merge logic into the Database. A device then gets its data back in one of two ways: through the Query API on reconnect, or, if it is already connected and zone-subscribed, pushed to it over WebSocket or SSE. Both come from the same partitioned event stream, they just answer a different moment in a tablet's connectivity.

### 3.5 How This Connects to Chapter 2

Our bounded contexts match the stakeholders from Section 2.3. Field Reporting serves the crews (Offline, Meeting, and Reconnect scenarios). Zone Subscription serves division leaders and command staff (Connected scenario, R6). Table 3.2 models the same four scenarios from Section 2.4, but as a domain, not as narrated steps. The Tier A/B split is exactly what R4 needs at the data level, no report is lost, this is the "how" that R4 itself points to this chapter for.

<!-- Section 3 numbering: PA1's real, final stakeholder terminology (Section
2.3) uses different terms (field workers, EOC personnel, owning agency, etc.)
than crew leader/division leader/command staff, used here and throughout
Stefan's real content, kept as-is per team decision. Section 3.4 is the
separate, smaller "20% concept architecture" piece Finch also asks for, the
rest of Section 3 is PA3 domain modelling proper. Quotes in 3.1 and 3.3 are
primary, read directly from l05-pa123-2026.pdf's own slide text. -->

## References

<a id="1">[1]</a> National Park Service, "Wildland Fire: Incident Command System," Nps.gov. https://www.nps.gov/articles/wildland-fire-incident-command-system.htm (accessed Sep. 20, 2026)

<a id="2">[2]</a> ScanCal, "Communications and ICS," Scancal.org. https://scancal.org/calfire/ics/communications.html (accessed Sep. 20, 2026)

<a id="3">[3]</a> Los Angeles Fire Department, "LAFD Releases January 2025 Palisades Fire After-Action Review Report," Lafd.org, Oct. 8, 2025. https://lafd.org/alert/lafd-releases-january-2025-palisades-fire-afteraction-review-report-10082025 (accessed Sep. 20, 2026)

<a id="4">[4]</a> Firehouse, "LAPD: Lack of Unified Command Led to Chaos During Palisades Fire," Firehouse.com, Nov. 2025. https://www.firehouse.com/california-fire-storm/news/55328119/lack-of-unified-command-led-to-chaos-during-palisades-fire-probe-shows (accessed Sep. 20, 2026)

<a id="5">[5]</a> Santa Monica Daily Press, "LAFD report calls for major changes after devastating Palisades Fire," Smdp.com, Oct. 2025. https://www.smdp.com/lafd-report-calls-for-major-changes-after-devastating-palisades-fire (accessed Sep. 20, 2026)

<a id="6">[6]</a> Wikipedia, "2002 Überlingen mid-air collision," Wikipedia.org. https://en.wikipedia.org/wiki/2002_%C3%9Cberlingen_mid-air_collision (accessed Sep. 20, 2026)

<a id="7">[7]</a> The Local, "Opinion: As French wildfires still burn, the blame-the-Greens deflection has begun," Thelocal.fr, Aug. 5, 2026. https://www.thelocal.fr/20260805/opinion-as-french-wildfires-still-burn-the-blame-the-greens-deflection-has-begun (accessed Sep. 20, 2026)

<a id="8">[8]</a> Notre-Planète, "Incendies de juillet 2026 en Gironde : le plus vaste feu de forêt en France depuis 1949," Notre-planete.info, Aug. 12, 2026. https://www.notre-planete.info/actualites/506-feux-foret-incendie-Gironde-juillet-2026 (accessed Sep. 20, 2026)

<a id="9">[9]</a> A. van Deursen, "Problem Analysis, Part II," CS4505 Software Architecture, Lecture 5, Delft University of Technology, Sep. 2026.

<a id="10">[10]</a> A. Cockburn, *Writing Effective Use Cases*. Boston, MA: Addison-Wesley, 2001.

<a id="11">[11]</a> Scaled Agile, Inc., "Roadmap," SAFe Framework. https://framework.scaledagile.com/roadmap/ (accessed Sep. 19, 2026)

<a id="12">[12]</a> U.S. Department of Defense Enterprise Software Initiative, "Software Maintenance Negotiations Best Practices." https://itvmo.gsa.gov/assets/files/Software-Maintenance-Negotiations-Best-Practices.pdf (accessed Sep. 20, 2026)

<a id="13">[13]</a> Atlassian, "Farewell to Server." https://www.atlassian.com/blog/announcements/farewell-to-server (accessed Sep. 20, 2026)

<a id="14">[14]</a> A. van Deursen, "Problem Analysis, Part I," CS4505 Software Architecture, Lecture 2, Delft University of Technology, Sep. 2026.
