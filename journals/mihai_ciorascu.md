Logbook Mihai Ciorascu


W1 (1 - 7 sep)

What happened this week:

* Short week, mostly settling in.
* Monday 7 sep, team call: picked the project. Native app for field workers. PoC is about wildfire crews, giving them better situational awareness than radios. Firefighters send reports, app shows a live map of the fire. Multiple teams can report at once, so we need a way to merge or pick the right info.

hours:

* went to lectures: 4h
* read the material again at home: 1.5h
* looked for a team: 2h
* project pick call (7 sep): 2h

* coding: 0h
* writing: 0h

total: 9.5h

W2 (8 - 14 sep)

What happened this week:

* Tuesday 8 sep: first TA meeting, short one.
* Saturday 12 sep and Sunday 13 sep: two long calls back to back. Went through the problem statement, design, planning, architecture. Ended Sunday with a list of open questions: how to design the system, how to merge reports, which "time" ordering to use (vector clocks vs hybrid logical clocks), what Android dev actually needs, how to handle several events at once (like more than one fire).
* Read the slides again this week too.
* Spent time researching through the sync/merge design and some Android questions.

hours:

* TA meeting (8 sep): 0.5h
* Saturday call (12 sep): ~2h
* Sunday call (13 sep): ~2h
* slides: 1h
* sync/merge + Android: 3h

* coding: 3h (sync/merge + Android research 3h)
* writing: 0h

total: 8.5h

W3 (15 - 21 sep)

What happened this week:

* Tuesday 15 sep, team meeting (2h): We talked about how to order events across the system and agreed to use vector clocks or DVV for this. Then we went through the use cases one by one and worked out how the services should talk to each other.
* Thursday 17 sep: I spent some time on Android (1.5h). I went through Activities, Fragments, Room for saving data locally, WorkManager for background sync, and for the P2P part researched about how Bluetooth, WiFi Direct and Nearby Connections work, there pros and cons and which one to pick for our project, since we need these for the tablet side of Firefly.
* Saturday 19 sep: Report meeting (2h). We talked about how the report should look and what goes in it. We already had most of the ideas from earlier meetings, so it was mostly about writing them down. We also split the tasks so we can finish the Finch release on time. After that I started PA3 (4h), the domain model chapter. I went through our scenarios and picked out the events, then set the 4 bounded contexts: Field Reporting, Synchronization, Zone Subscription and Device Identity. I worked out Field Reporting in detail: the Incident and Report entities, the Event record, the DVV and the two-tier merge policy.
* Sunday 20 sep: Short sync (1h). We talked about what was still left to write. Then we merged the PA1, PA2 and PA3 parts into one report. I picked up Context View, Wardley Map and Architecture sections. I used what I wrote for PA3 for the Architecture section and contributed to the shared file with Context View and Wardley Map as well. Finally, read through the whole report to check for contradictions and words that did not match between sections. (5h)
* Monday 21 sep: Lowered the word count and fixed inconsistencies between text and citations & diff subchapters.(2h)

hours:

* team meeting on event ordering and use cases (Tue): 2h
* Android research (Thu): 1.5h
* report meeting and task split (Sat): 2h
* PA3 domain model work (Sat): 4h
* short sync (Sun): 1h
* finishing my part of report, merging and checking the report (Sun): 5h
* lowering word count and fixes (Mon): 2h

* coding: 1.5h (Android research 1.5h)
* writing: 11h (PA3 domain model work 4h, finishing my part of the report, merging and checking 5h, lowering word count and fixes 2h)

total: 17.5h

W4 (22 - 28 sep)

What happened this week:

* Went to both lectures.
* Friday 25 sep, team meeting (2h): We split up the implementation work for Heron, everyone took a piece of the PoC and I ended up with P2P.
* Saturday 26 sep: Worked on the Android side of P2P (5h). Got 2 devices to find each other over Nearby Connections and exchange a simple test message. Still rough, but the basic connection works :))
* After that, put together the peer review presentation (2h).

hours:

* lectures (Tue, Thu): 4h
* team meeting on Heron planning (Fri): 2h
* P2P implementation (Sat): 5h
* presentation prep (Sat): 2h

* coding: 5h (P2P implementation 5h)
* writing: 0h

total: 13h

W5 (29 sep - 5 oct)

What happened this week:

* Went to both lectures.
* Tuesday 29 sep: we gave the peer review presentation (1h).
* Thursday 1 oct: online meeting with our TA (45 min) about our progress and the report.
* The rest of the week I worked on P2P. One device can now send a severity change and the other one receives it. I fixed a few problems (location permission, an error message, a device connecting to itself) and wrote a short guide to test it on 2 devices. Then I put my branch together with my teammates' work on the database and the messaging.
* Monday 5 oct: My P2P branch was merged into master. I tested it on 2 emulators and they exchange operations in both directions. A phone and an emulator do not find each other, so we still need a test on 2 real devices.

hours:

* lectures (Tue, Thu): 4h
* peer review presentation (Tue): 1h
* TA meeting (Thu): 0.75h
* P2P severity changes and fixes: 6h
* test guide and putting the branches together: 3h
* P2P testing on emulators (Mon 5): 2h
* merge requests (Mon 5): 1h

* coding: 12h (P2P severity changes and fixes 6h, test guide and putting the branches together 3h, P2P testing on emulators 2h, merge requests 1h)
* writing: 0h

total: 17.75h

W6 (6 - 12 oct)

What happened this week:

* Went to one lecture.
* Friday 9 oct: Team meeting (1.5h) and TA meeting in person (30 min). The TA wants experiments linked to our quality attributes, READMEs and all report changes on master. The ADRs do not count for the word limit. After that I started planning the CA3 experiments and the PoC README.
* Monday 12 oct: Met with Alex (4h) to work on the experiments, fix problems and improve the report. We did a first walking test of the P2P range with the S24 and the S25 Ultra. I walked away in a straight line while sending severity changes until Alex stopped receiving them. That was at 50-60m.

hours:

* lecture: 2h
* team meeting (Fri 9): 1.5h
* TA meeting (Fri 9): 0.5h
* CA3 plan and PoC README (Fri 9): 3h
* meeting with Alex: experiments, walking test and report (Mon 12): 4h

* coding: 4h (meeting with Alex: experiments and walking test)
* writing: 3h (CA3 plan and PoC README 3h)

total: 11h

W7 (13 - 19 oct)

What happened this week:

* Tuesday 13 oct: Checked that the Heron release was ready and created the tag and the release on GitLab.
* Wednesday 14 oct to Thursday 15 oct: peer review of the first team.
* Friday 16 oct to Saturday 17 oct: peer review of the second team.
* Saturday 17 oct: team meeting (~2h). Next steps: resilience experiment, apply the feedback to the report, DVVs, server-tablet communication, adapt the PoC device to the new merge library, edge cases of the merge logic and the final presentation. We also decided to leave security for later.
* Sunday 18 oct and Monday 19 oct: read our report again with the mistakes from the peer reviews in mind, and told the team what to change.
* Monday 19 oct: code review of five merge requests from the team. Ran the tests, read the code and wrote comments for the authors.
* Spent time researching what we need to do in the next weeks.

hours:

* Heron release check, tag and release (Tue 13): 3h
* peer review of team 1 (Wed 14 - Thu 15): 6h
* peer review of team 2 (Fri 16 - Sat 17): 6h
* research for the next weeks: 4h
* team meeting (Sat 17): ~2h
* second look at the report and telling the team what to change (Sun 18 - Mon 19): ~5h
* code review of the team's merge requests (Mon 19): 2.5h

* coding: 9.5h (research for the next weeks 4h, Heron tag and release 3h, code review 2.5h)
* writing: 5h (second look at the report 5h)

total: 28.5h

W8 (20 - 26 oct)

What happened this week:

* Tuesday 20 oct: edge cases of the merge logic. Wrote 4 tests (equal severity on 2 devices, a resolution that saw only one of 2 values, 2 creates with different immutable values, a field write that arrives before its create)
* Wednesday 21 oct: more edge cases of the merge logic. Wrote 3 more tests (a field from a newer app version is refused and the world stays the same, a resolution when there is no conflict acts like a plain write, 2 events that each say they saw the other).
* Thursday 22 oct: wrote 4 more edge case tests (3 values in conflict, a late write after a resolution, the same text on 2 devices, 2 events with the same dot). Committed the first one.
* Friday 23 oct: TA meeting in person (30 min). The TA wants the focus on functionality, not on the UI and discussed the demo as well. Committed the second test.
* Friday 23 oct to Monday 26 oct: read Radu's device to server link (the server merges and stores events and sends reports back by zone). On Monday I merged the merge request. I committed the third test on Sunday and the fourth on Monday.

hours:

* edge cases of the merge logic, tests (Tue 20): 4.5h
* edge cases of the merge logic, 3 more tests (Wed 21): 4h
* edge cases of the merge logic, 4 more tests (Thu 22): 4h
* TA meeting (Fri 23): 0.5h
* reading and merging the device to server link (Fri 23 - Mon 26): 4h

* coding: 16.5h (edge cases of the merge logic: Tue 4.5h, Wed 4h, Thu 4h, reading and merging the server link 4h)
* writing: 0h

total: 17h

W9 (27 oct - 2 nov)

What happened this week:

* Tuesday 27 oct: Met with Alex to do the demo. P2P failed on Android 17 with the error "8: INTERNAL_ERROR". I added the local network permission to the manifest and ask for it in the app. The edge case tests and this fix were merged the same day.
* Wednesday 28 oct: I rehearsed the presentation and checked the screen texts and the server log lines against the code. I prepared for the Q&A (Nearby Connections, the merge tiers, the experiments, pricing, security). Met with the entire team to rehearse the presentation (2h)
* Sunday 1 nov: merging (2h)

hours:

* meeting with Alex to work on demo: 4h
* Android 17 fix (Tue 27): 4h
* rehearsing my part at home, preparing the Q&A (Wed 28 - Sat 31): 4h
* meeting with the whole team: 2h
* merging: 2h

* coding: 10h (meeting with Alex 4h, Android 17 fix 4h, merging 2h)
* writing: 0h

total: 16h