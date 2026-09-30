# Personal Journal - Radu Zidaru

## Week 1 (31 August - 4 September 2026)

This week was mainly about getting familiar with the course and forming the team. Since I was working from the office during the weekdays, I went through the lectures and slides from home in the evenings instead of attending them on campus. I reviewed the course structure, the Finch, Heron, and Swan stages of the project, and the expectations for the proof of concept. I also spent time finding a team and discussing possible project directions.

### Time spent

* Lectures and slides at home: 4h
* Course organization and team formation: 2h

**Total: 6h**

## Week 2 (7 - 13 September 2026)

On Monday, 7 September, we had a two-hour team call in which we selected and developed our project idea: a native application that helps field workers communicate and synchronize information when network access is unreliable. For the proof of concept, we decided to focus on wildfire crews. Firefighters would submit reports from the field, while the application would combine those reports and show the changing situation on a live map.

On September 9, we had our first meeting with the TA. We presented the idea and clarified the expected project scope and proof-of-concept deliverables.

We met again for roughly two hours on both Saturday and Sunday. During these meetings, we refined the problem statement, compared our interpretations of how the application should work, agreed on the main use cases, and discussed planning and the high-level architecture. The main open questions were how reports from different field worker teams should be synchronized and merged, how to order updates using mechanisms such as vector clocks or hybrid logical clocks, and how the system should handle multiple simultaneous events.

Between the meetings, I did my own research into offline-first architecture, synchronization, conflict resolution, and the practical constraints of building the native application. As in the first week, I went through the lecture material at home after work because I was in the office during the scheduled weekday lectures.

### Time spent

* Team meeting to select the project (7 September): 2h
* TA meeting (9 September): 0.5h
* Team meeting (12 September): 2h
* Team meeting (13 September): 2h
* Lectures and slides at home: 3h
* Individual architecture and synchronization research: 3h

**Total: 12.5h**
## Week 3 (14 - 20 September 2026)

On Tuesday, 15 September, we had a two-hour team meeting in which we examined how events should be ordered across the system. We compared vector clocks and dotted version vectors and continued refining the use cases and communication between the application, nearby devices, and the backend.

During my individual work, I investigated the minimum data the application needs to store and how conflicts should be handled. I defined the fields required for incidents, reports, and synchronization events, and examined how dotted version vectors can detect whether edits to the same record field are causally ordered or concurrent. I also explored field-specific conflict policies, separating fields that can be merged automatically from fields that must preserve all concurrent values for manual resolution.

On Saturday, 19 September, we had another two-hour team meeting focused on the Finch report. We discussed its structure and content, reviewed the decisions made during the previous meetings, and divided the remaining tasks so that the release could be completed on time.

On Sunday, 20 September, we had a one-hour synchronization meeting to review the report sections that still needed to be written and coordinate the remaining work.

### Time spent

* Team meeting about event ordering, use cases, and communication (15 September): 2h
* Individual investigation into stored fields and conflict resolution: 3h
* Team meeting about the Finch report and task division (19 September): 2h
* Team synchronization meeting (20 September): 1h

**Total: 8h**