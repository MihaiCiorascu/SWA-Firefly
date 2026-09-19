# 2. Function

<!--
DRAFT NOTES (delete before merging into the report)

Section definitions, checked by a literature search on 2026-09-19. Tags:
VERIFIED = primary text seen. TOC = chapter titles only. UNVERIFIED =
secondary source or memory. Check anything below VERIFIED before citing it.

- 2.1 Vision: neither deck defines it. The course reading has no "vision"
  chapter. The closest is Coplien & Bjørnvig, Lean Architecture, Ch. 4
  "Problem Definition" (TOC). A good problem definition states the gap between
  the current and the required state, is measurable, and is in natural,
  non-solution language (UNVERIFIED). Pitfall named in that chapter:
  "creeping featurism" (TOC).

- 2.2 Scenarios: R&W Ch. 10 "Identifying and Using Scenarios" (TOC).
  Functional vs system quality scenarios, and the fields overview / state /
  environment / stimulus / response, are UNVERIFIED. The R&W reference card on
  viewpoints-and-perspectives.info is VERIFIED: "Have you identified the
  important usage scenarios and used these to validate the system's functional
  structure?" For the quality scenario, the safer structure is Bass, Clements &
  Kazman (SAiP, 3rd ed.): source, stimulus, environment, artifact, response,
  response measure. The per-attribute "general scenario" sections are TOC; the
  field wording is UNVERIFIED.
  Cockburn: goal levels and the basic use case template are VERIFIED (read in
  full on alistair.cockburn.us). Actor, goal, trigger, main success scenario,
  extensions, stakeholders and interests.

- 2.3 ASRs: slide 21 criteria are primary. Support: SAiP Ch. 16 "Architecture
  and Requirements", ASRs captured in a utility tree (16.4) (TOC). Chen et al.,
  "Characterizing Architecturally Significant Requirements", IEEE Software 2013:
  cite the paper itself, not Wikipedia (not yet read).

- 2.4 Quality attributes: Pautasso's categories (design, operational,
  dependability & security, change, long-term) are UNVERIFIED, so check
  against the lecture. SAiP Part Two (Ch. 4 onwards) and ATAM (Ch. 21) for
  tradeoffs (TOC).

- 2.5 Roadmap: SAFe definitions VERIFIED (framework.scaledagile.com):
  Epic = "a significant solution development initiative"; Roadmap = "a schedule
  of events and milestones that forecasts and communicates planned solution
  deliverables over a time horizon"; advice to emphasise "themes, epics, or
  features instead of specific release dates". Wardley evolution stages: cite
  L05 slide 27. "Zig zagging": probably Nuseibeh's Twin Peaks model (IEEE
  Computer 2001), UNVERIFIED.

- 2.6 Pricing / sustainability: no support in the reading list. Argue it from
  L05 slides 22-23 and our own reasoning.
-->

## 2.1 Vision

The system is a shared incident map that crews can use without a connection. It answers the problem in Section 1.1. Its users are squad leaders, division supervisors, and the command staff at the Incident Command Post.

Each tablet works offline and syncs with a central server when it has a connection. Assignments reach squads the same way. The server sends each tablet only the reports from the zones around it. When two squads meet, their leaders can switch on P2P mode to sync directly. Some information merges automatically, and the squads settle the rest on the spot.

The system is for small and large incidents, and it does not replace voice radio. Relaying information through squads that never met its author is a later step (Section 2.5).

## 2.2 Key Scenarios

Table 2.1 lists the scenarios at user-goal level [Cockburn], with the stakeholders from Section 1.3 that each one serves. We detail two of them below.

<!-- Actor and stakeholder names must match Section 1.3. -->

| ID | Actor | Goal | Serves |
|---|---|---|---|
| S1 | Squad leader | Log an observation without a connection | Squads |
| S2 | Squad leader | Receive updates and assignments once connected | Squads, command staff |
| S3 | Two squad leaders | Sync in P2P mode when their squads meet | Squads |
| S4 | Two squad leaders | Settle conflicting reports about the same thing | Squads |

*Table 2.1: Scenario overview.*

### 2.2.1 Logging without a connection (S1)

The leader of Squad 3 works below a ridge with no coverage and sees a spot fire across the containment line. They report it over the radio and then mark it on the map. The report shows immediately, with a "not yet shared" mark until the next sync.

### 2.2.2 Two squads meet (S3, S4)

Squads 3 and 7 meet during the shift. Neither squad has had a connection since the morning briefing. Both leaders switch on P2P mode. After the sync, each leader sees a summary of the reports that came in and went out. Reports that do not conflict show on both maps. Sometimes the squads recorded different things about the same item, for example a road that one squad marked as blocked and the other as open. Then both tablets show both reports, and the leaders settle the conflict on the spot. No report from either squad is lost.

<!-- FIGURE 2.1: timeline of this scenario (two lanes: Squad 3, Squad 7).
Export the Mermaid source to PNG so it does not count toward the word limit. -->

## 2.3 Architecturally Significant Requirements

A requirement is architecturally significant if it needs dedicated components, is expensive to change, or enables or blocks future functions [L05, slide 21].

| ID | Requirement | From | Why significant |
|---|---|---|---|
| R1 | Without a connection, a tablet can store new reports, update existing reports, and sync with another tablet in P2P mode. | S1, S3 | Each tablet keeps its own copy of the record. It is expensive to add this to an online design later. |
| R2 | With a connection, a tablet syncs with the central server automatically. It sends its reports and receives the updates and assignments for its zones. | S2 | A component must detect the connection and sync without user action. |
| R3 | After two peers sync, both hold the same record. The central server counts as a peer. | S2, S3 | All pairs of peers use one sync behaviour. This also makes relaying possible later (E4). |
| R4 | No entry is lost. The users see conflicting reports, and the system never drops them. | S3, S4 | The stored data must show which changes were concurrent. This is expensive to change after deployment (ADR in Part II). |
| R5 | Two tablets only sync when both squad leaders have switched on P2P mode. | S3 | Tablets must find and trust each other only on request. This prevents exchange in the background. |
| R6 | The central server subscribes each tablet to the zones within a radius of its position, or to zones that the server assigns. The radius depends on the user's role. The server sends each tablet only the reports from its zones. Zones do not limit P2P mode. | S2 | A server component must track positions and subscriptions. Traffic stays small, so the system works for small and large incidents. |
<!-- Part II notes, not for PA2:
- Zones are Uber's H3 hexagons; subscription = all hexagons intersecting a circle of radius x around the tablet (figure available). Name H3 in CA1 or an ADR.
- ADR candidate: the server pushes updates to tablets over a persistent connection (e.g. WebSockets) instead of tablets long-polling every 30 s, because polling costs too much battery and hardware on the tablets. -->

## 2.4 Quality Attributes and Tradeoffs

Section 1.5 lists all non-functional requirements. The three qualities below are the most important for our scenarios. The proof of concept can test each target.

| Quality | Target | From |
|---|---|---|
| Offline operation | Tablets store, update and sync reports in P2P mode for a full operational period (12 hours) without the central server. | R1, R5 |
| No lost reports | Zero reports lost. A stored report survives crashes and interrupted syncs. It reaches the central server at the next sync. | R3, R4 |
| Visible report age | Each report shows its author and age on each tablet. | R1, R2 |

<!-- The 12-hour target is a proposal for the team to confirm. -->

The main tradeoff is between manual P2P mode and the spread of data. Leaders sync only when they choose to, so reports reach other squads later than with automatic exchange. We accept this delay for lower battery use, and because leaders then control with whom they sync.

<!-- CONFIRM: are battery use and control the team's actual reasons for the manual toggle? -->

## 2.5 Delivery Roadmap

The roadmap in Figure 2.2 puts epics in order over one to two years. An epic is a set of user stories that together give value [L05, slide 20]. As SAFe recommends, the roadmap shows no dates [SAFe]. Each epic contains the interface and the functions behind it.

![Figure 2.2: Delivery roadmap](figures/roadmap.png)

*Figure 2.2: Delivery roadmap. E1 and E2 are built in the proof of concept.*

<!-- Source: figures/roadmap.mmd. Re-render with mmdc after changes. -->

| Epic | The squad leader gets | Delivers | Serves | Value measured by |
|---|---|---|---|---|
| E1 | An offline map to log reports, with sync to the central server | R1 to R4 | Squads, command staff | Reports logged offline per shift |
| E2 | P2P mode, a sync summary, and a view to settle conflicts | R1, R5 | Squads | P2P syncs per shift |
| E3 | Reports from the zones around them | R6 | Division supervisors | Data sent per tablet |
| E4 | Reports that other squads relay from their author | R3 | Squads | Time for a report to reach squads |

*Table 2.2: Epics.*

At D1, field trials check if squad leaders can log reports quickly with gloves on. If they cannot, we improve the interface before E2. At D2, we build E4 only if squads use P2P mode often, because relaying depends on it.

## 2.6 Pricing and Sustainability

Governments buy through their fire services, with yearly budgets and multi-year contracts. Of the pricing models in L05 (slide 23), a yearly subscription per fire service, priced per tablet, fits best. Comparable tools use the same model [Avenza], [Esri].

A subscription needs license management on the central server. A lapsed license must never stop a tablet in the field, so the server checks the license only when a tablet syncs with it. Squads get the reports of their neighbours, command staff get a combined picture, and fire services get a predictable cost. The disadvantage for crews is that their tablets show where they are, which Section 1.3 discusses.

<!-- Sources (verified by the pricing research, 2026-09-19):
[Avenza] https://store.avenza.com/pages/pricing (Pro $169.99/device/yr; enterprise volume pricing)
[Esri] https://www.esri.com/en-us/arcgis/products/arcgis-field-maps/buy (bundled in user-type licenses)
Also verified: Technosylva $383k California RFI2 contract, https://statescoop.com/california-wildfire-technology-contracts-fast-procurement/
Full research: scratchpad pricing_research.md. Tablet Command contract amounts are UNVERIFIED, so don't cite them. -->
