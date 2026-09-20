## 2.4 Example Scenarios

To show how the system works in practice, this section presents four scenarios: a crew working with a connection, a crew logging without one, two crews meeting during a shift, and a tablet coming back into coverage. These scenarios cover the requirements that shape the architecture. The remaining requirements are in Appendix A, as user stories.

- **Working with a connection**: Alice, who leads a crew on the eastern flank, marks a blocked access road on her tablet, and it shows up on the command staff's map at the Incident Command Post. While she works, reports from the crews near her appear on her own map without her asking for them, each one naming the crew that sent it and the time they sent it. When the command staff assign her crew for the coming shift, the assignment arrives the same way, although anything urgent still reaches her over the radio. What the crews on the far side of the fire report never lands on her tablet, since it is of no use to her.

- **Logging without a connection**: Alice's crew moves below a ridge and loses coverage, from where she now sees a spot fire across the containment line; it calls it in on the radio, and then marks it on her map, noting where it is, how big it is and which way it is moving. Her tablet keeps the report with her name and the time on it, and she carries on logging through the afternoon from the map she had when the signal went.

- **Two crews meeting**: Alice's crew runs into Bob's crew later that day, and neither of them has had a connection since the morning. They both switch on P2P mode, each tablet shows the other crew, and once the sync is over each of them sees a summary of what came in and what went out. Almost everything lands on both maps, excepting conflicting logs. Take the example of one specific roadt: Alice had marked it blocked and Bob had marked it open. Both tablets show the road with both reports on it, who made them and when, and Alice and Bob settle it between themselves before their crews split up again. This way we ensure that neither report is thrown away.

- **Coming back into coverage**: Bob drives back towards the staging area and his tablet picks up a signal, where it syncs with the central server. After the sync, everything his crew logged that day, along with what he took from Alice, reaches the command staff. In return his tablet takes in the reports it missed for the zones around him.

## 2.5 Architecturally Significant Requirements

As not every requirement shapes the architecture, we treat a requirement as architecturally significant if it needs dedicated components, is expensive to change later, or affects (i.e. enables, blocks) future functions [L05, slide 21]. Six requirements meet at least one of these criteria.

- **R1, working without a connection.** A tablet can store new reports, update existing ones and sync in P2P mode while offline. Each tablet therefore keeps its own copy of the record instead of reading one from a server. It is also the hardest requirement to add later: assume a live connection first, and removing that assumption means designing the system twice.

- **R2, syncing with the central server.** When a tablet has a connection it syncs on its own, sending its reports and receiving the updates and assignments for its zones. Unlike P2P mode this happens without anyone asking, so a component has to notice the connection and act on it.

- **R3, single sync behaviour.** After two peers have synced, both hold the same record, and the central server counts as a peer. One behaviour covers every pair that syncs, and a tablet passes on what it received as readily as what its own crew recorded, so a report can reach a crew that never met its author.

- **R4, no silent loss.** No report is lost. That shapes how reports are stored and exchanged, and it is expensive to change once tablets are in the field. The architectural decisions in Section 3 cover how.

- **R5, P2P mode under the users' control.** Two tablets only sync when both crew leaders have switched P2P mode on. Tablets must find and trust each other on request, which is a component in its own right, and this rules out exchanging data in the background whenever two tablets pass each other.

- **R6, zones.** The central server subscribes each tablet to the zones within a radius of its position, or to zones it assigns, and sends only the reports from those zones. The server therefore tracks positions and subscriptions, which decides what every sync with it contains. Zones do not limit P2P mode, where two tablets exchange everything they hold.

## 2.6 Quality Attributes

Several quality attributes were prioritised while designing the system. Each is a property of how the system behaves rather than a function it offers, and each is stated with a target the proof of concept can measure.

- **Availability under partition.** Nothing is withdrawn when the connection goes. A tablet that cannot reach the central server offers what it offers when connected, and it does so for a full operational period of twelve hours. A tablet that has to ask a server for the map is useless below a ridge, which is what R1 pays for.

- **Reliability.** A report the tablet has accepted survives a crash, a flat battery and a sync that is cut off halfway, and the system never discards it. The target is zero loss across the interruptions the experiments inject, since nobody can ask for a report they never knew was made.

- **Scalability.** The volume a tablet receives stays roughly constant as an incident grows from one crew to dozens, and the central server absorbs the shift-change surge, when a whole division comes back into coverage within minutes carrying twelve hours of reports each. These are the targets the beta phase measures (Section 2.7).

- **Compatibility across versions.** Tablets and servers one version apart still sync. Agencies upgrade on their own schedule, so mutual aid puts devices of different vintages on the same fire, and the sync behaviour cannot assume that both sides are current.

## 2.7 Roadmap and Pricing

To make the future of the system concrete we have laid it out as a roadmap of four phases, over a horizon of one to two years. The roadmap carries no dates, since development time is impossible to estimate without knowing the size of the team, and following SAFe it names what each phase delivers rather than when it ships [SAFe].

The **proof of concept** is the phase we build during this course, and it is deliberately narrow: the offline map, syncing with the central server, and P2P mode between two tablets. It is not deployed to anyone. Its purpose is to show that the two riskiest parts hold, namely that a tablet stays useful with no connection and that two tablets which worked apart can reconcile what they did.

The **minimum viable product** puts the system in the hands of one fire service, on real incidents and alongside the radio. It adds the first interface a crew can actually work with: a map on the tablet for logging and reading reports, and zones, so a crew receives what belongs to the ground it is working on. A decision point sits before this phase, shown in Figure 3: field trials have to show that a crew leader can log a report quickly with gloves on and a fire in front of them. If they cannot, the interface is reworked before anything is built on top of it.

The **beta** widens the same fire service from one crew to a whole incident: every crew in the field, their division leaders, and the command staff reading the incident picture. Where the MVP shows that the system works for the crew that carries it, the beta shows that it holds when a full operational period of reports flows through it at once, which is the first point at which zones and sync are under a load worth measuring. The experiments in Section 5 target this phase.

**Release 1.0** adds mutual aid across agencies and a reworked interface on both fronts: the tablet map the crews use, and a web client for the command staff, who work from a desk rather than a fire line. Two fire services working the same fire each run their own central server, and reports made on one agency's tablets have to reach the crews of the other, whose devices may be on a different version. This is the point at which the system stops serving one organisation and starts serving an incident, and it is where licensing and support begin.

![Figure 3: Delivery roadmap](figures/roadmap.png)

_Figure 3: Delivery roadmap. Only the proof of concept is built during this course._

Fire services will not hand live crew positions to a server we run, so the system is sold as a licence deployed on the customer's own infrastructure. Of the models in L05 (slide 23) this is a subscription, but an annual licence for software the agency hosts itself, as self-hosted enterprise products have moved from perpetual licences to yearly terms [Atlassian]. It is priced per agency rather than per tablet, since the number of tablets swings with the season and with mutual aid. The minimum viable product is free, so that a fire service can run it for one season without a procurement process, and the licence begins at release 1.0. Annual support follows the licence, commonly 18 to 22 per cent of it, which public buyers negotiate down and cap against escalation [DoD ESI]. Deployment and integration are charged separately.

Hosting at the customer decides more than the price. Every agency runs its own server and upgrades on its own schedule, so the mutual aid of release 1.0 has to work between servers of different versions, which falls on R3. Crews, command staff and fire services each gain from the system; the cost falls on the crews, whose tablets show where they are, which Section 2.1 discusses.

## References

[L05] A. van Deursen, "Problem Analysis, Part II," CS4505 Software Architecture, Lecture 5, TU Delft, Sep. 2026.

[Cockburn] A. Cockburn, _Writing Effective Use Cases_. Boston, MA: Addison-Wesley, 2001.

[SAFe] Scaled Agile, Inc., "Roadmap," _SAFe Framework_. [Online]. Available: https://framework.scaledagile.com/roadmap/ (accessed Sep. 19, 2026).

[DoD ESI] U.S. Department of Defense Enterprise Software Initiative, "Software Maintenance Negotiations Best Practices." [Online]. Available: https://itvmo.gsa.gov/assets/files/Software-Maintenance-Negotiations-Best-Practices.pdf (accessed Sep. 20, 2026).

[Atlassian] Atlassian, "Farewell to Server." [Online]. Available: https://www.atlassian.com/blog/announcements/farewell-to-server (accessed Sep. 20, 2026).
