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

The system is a shared incident map that lets wildfire crews keep recording what they see without a connection, and bring those records together later without losing any of them. It serves squad leaders and crew bosses who log observations in the field, division supervisors who need to know what neighbouring crews have seen, and the command post that builds the incident picture from their reports.

Radios carry urgent traffic, and existing digital tools deliver maps and layers to the field. What is missing is a way for several crews to add to and correct the same incident record while out of contact. The system keeps working on each device offline and synchronises with the command post whenever a connection is available. When two squads meet, their leaders can switch on a direct exchange to bring each other up to date. Some information is combined automatically. The rest is settled by the squads on the spot.

The system is meant for small and large incidents alike. It does not replace voice radio, model fire behaviour or direct aircraft. Passing information on through squads that never met its author is a later step (Section 2.5).

## 2.2 Key Scenarios

We describe the system's function through scenarios written from the user's side, at the level of one person reaching one goal [Cockburn]. Following Rozanski and Woods [R&W, Ch. 10], we separate functional scenarios, which say what a user does and sees, from a system quality scenario, which states how the system must hold up under a change in its environment and sets measurable targets. Table 2.1 lists the scenarios the system must support and the stakeholders from Section 1.3 each one serves. Three are worked out below, because they carry the requirements that shape the architecture (Section 2.3). The others are in Appendix A.

<!-- Stakeholder names must match Section 1.3. 1.3 currently says "crew and
division leaders" and "incident commanders and command staff". Pick one set of
ICS terms and use it in both places. -->

| ID | Actor | Goal | Stakeholders served |
|---|---|---|---|
| S1 | Squad leader | Log an observation (spot fire, blocked road, hazard) without a connection | Squads, division supervisors |
| S2 | Squad leader | Bring the tablet up to date with the command post when coverage returns | Squads, command post |
| S3 | Two squad leaders | Exchange updates directly when their squads meet | Squads, division supervisors |
| S4 | Two squad leaders | Settle a disagreement about the same road, hazard or line | Squads, division supervisors |
| S5 | Division supervisor | See what neighbouring squads have reported, and how old each report is | Division supervisors |
| S6 | Command post | See the combined incident picture and send assignments to the field | Incident commander, planning staff |
| S7 | Squad member | Correct or withdraw their own wrong entry | Squads |
| S8 | Fire service | Replay the incident record for debriefing and investigation | Fire service leadership, investigators |

*Table 2.1: Scenario overview. S1, S3 and S4 are detailed below, and the quality scenario Q1 follows them.*

### 2.2.1 Logging an observation without a connection (S1)

At 13:40 Squad 3 is working the Division B flank below a ridge, with no coverage. The squad leader sees a spot fire about 50 metres across the containment line. They call it in over the radio first, as procedure requires. They then mark it on the tablet map with a size estimate and a photo. The spot fire appears on their map straight away, stamped with their name and the time, and marked as not yet shared with anyone. If the tablet shuts down before the next exchange, the entry is still there when it restarts.

*Success guarantee:* the observation is kept on the device with its author, time and place, and is shared at the next exchange, whether with the command post or with another squad.

### 2.2.2 Reconciling divergent records when two squads meet (S3, S4)

<!-- ASSUMPTION: both leaders switch on the exchange (handshake). If one side is
enough, change the second sentence. -->

At 15:00 Squad 3 (Division B) and Squad 7 (Division C) meet at a drop point. Neither has had a connection since the morning briefing. Both squad leaders switch on direct exchange, and each tablet shows the other squad by name. After the exchange, each leader sees a short summary of what came in and what went out. Squad 3's spot fire is now on Squad 7's map, and the dozer line Squad 7 built during the afternoon is on Squad 3's map.

One item needs attention. At 14:05 Squad 3 marked Road 12 as blocked by a fallen tree. At 14:20 Squad 7, who had not seen that report, marked it as open after a dozer cleared the road from their side. Both tablets now show Road 12 as disputed, with both reports, who made them and when. The two leaders talk it through, agree that the road is open, and mark it so. Both tablets now show Road 12 as open, together with a note that it was disputed and who settled it.

*Extensions:*
- The leaders cannot agree, or they leave before settling. Road 12 stays disputed on both tablets with both reports visible, and the dispute travels with the record to the command post. <!-- OPEN: who settles it there? -->
- The exchange is cut off halfway. Everything already received is kept, and the rest is exchanged at the next meeting.

*Success guarantee:* both tablets hold the same record, and nothing either squad entered has disappeared without someone seeing it.

<!-- FIGURE 2.1: timeline of this scenario. Two lanes (Squad 3, Squad 7):
morning briefing (in sync) -> split -> each logs entries and edits Road 12 ->
meet at drop point -> exchange -> automatically combined items vs Road 12
disputed -> settled on the spot. Draw it in Mermaid and embed the exported PNG,
so the diagram source does not count toward the word limit. -->

### 2.2.3 Quality scenario: a squad out of contact for a whole shift (Q1)

<!-- R&W system quality scenario fields. All numbers are PROPOSALS for the team
to confirm. CA3 experiments should measure them. -->

| Field | Content |
|---|---|
| Overview | A squad spends a full operational period out of contact while other squads keep recording, then meets another squad. |
| System state | Both tablets hold the incident record as it was at the morning briefing. |
| System environment | No connection to the command post for 12 hours. The two tablets are within a few metres of each other for about 5 minutes at the meeting point. |
| External stimulus | Each squad has logged up to 500 entries, and some of them concern the same roads, hazards or lines. Both leaders switch on direct exchange. |
| Required response | All entries are exchanged within 60 seconds. No entry is lost, and no entry is replaced without the users seeing it. Every item that cannot be combined automatically is shown as disputed before the squads leave. The same holds for a small incident (5 squads) and a large one (60 squads). |
