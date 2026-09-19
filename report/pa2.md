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

In response to the problem in Section 1.1, the system is a shared incident map that crews keep using without a connection. It serves squad leaders and crew bosses who log observations in the field, division supervisors who need to know what neighbouring crews have seen, and the command staff at the Incident Command Post, who build the incident picture from their reports.

Each device keeps working offline and synchronises with a central server whenever a connection is available, which is also how updates and assignments from the command staff reach the squads. When two squads meet, their leaders can switch on a direct exchange to bring each other up to date. Some information is combined automatically. The rest is settled by the squads on the spot.

The system is meant for small and large incidents alike. It does not replace voice radio, model fire behaviour or direct aircraft. Passing information on through squads that never met its author is a later step (Section 2.5).

## 2.2 Key Scenarios

We describe the system's function through scenarios written from the user's side, at the level of one person reaching one goal [Cockburn]. Table 2.1 lists them with the stakeholders from Section 1.3 they serve. Two functional scenarios and one quality scenario are worked out below.

<!-- Actor and stakeholder names must match Section 1.3. -->

| ID | Actor | Goal | Stakeholders served |
|---|---|---|---|
| S1 | Squad leader | Log an observation without a connection | Squads, division supervisors |
| S2 | Squad leader | Receive updates and assignments once the tablet reaches the central server | Squads, command staff |
| S3 | Two squad leaders | Exchange updates directly when their squads meet | Squads, division supervisors |
| S4 | Two squad leaders | Settle conflicting reports about the same thing | Squads |
| S5 | Division supervisor | See what the squads in their division have reported | Division supervisors |
| S6 | Command staff | See the combined incident picture and send assignments to squads | Command staff |
| S7 | Squad leader | Correct or withdraw their own wrong entry | Squads |

*Table 2.1: Scenario overview.*

### 2.2.1 Logging an observation without a connection (S1)

Squad 3 is working below a ridge with no coverage when the squad leader sees a spot fire across the containment line. They call it in over the radio, then mark it on the tablet map. It appears on their map straight away, marked as not yet shared, and is sent at the next exchange, with the central server or with another squad.

### 2.2.2 Two squads meet (S3, S4)

Squad 3 and Squad 7 meet during the shift. Neither has had a connection since the morning briefing. Both squad leaders switch on direct exchange, and each tablet shows the other squad. Afterwards, each leader sees a summary of what came in and what went out. Reports that do not conflict now appear on both maps. Where the squads recorded conflicting things about the same item, such as one marking a road as blocked and the other as open, both tablets show the conflict with both reports, and the two leaders settle it on the spot.

*Success guarantee:* both tablets hold the same record, and nothing either squad entered has been lost.

<!-- FIGURE 2.1: timeline of this scenario (two lanes: Squad 3, Squad 7).
Export the Mermaid source to PNG so it does not count toward the word limit. -->

### 2.2.3 Quality scenario: extended disconnection

<!-- Numbers are proposals for the team to confirm; CA3 experiments measure them. -->

| Field | Content |
|---|---|
| Situation | A squad has been out of contact for a full shift, then meets another squad. |
| Required response | Both tablets exchange everything they hold. No entry is lost, and conflicting reports are shown to both leaders, never dropped. |
| Target | 500 entries per tablet are exchanged within 60 seconds. |
