Logbook Mihai Ciorascu

W1 (31 aug - 4 sep)

Short week, mostly settling in.

* went to lectures: 4h
* read the material again at home: 1.5h
* looked for a team: 2h

total: 7.5h

W2 (7 - 13 sep)

What happened this week:

* Monday 7 sep, team call: picked the project. Native app for field workers. PoC is about wildfire crews, giving them better situational awareness than radios. Firefighters send reports, app shows a live map of the fire. Multiple teams can report at once, so we need a way to merge or pick the right info.
* Tuesday 8 sep: first TA meeting, short one.
* Saturday 12 sep and Sunday 13 sep: two long calls back to back. Went through the problem statement, design, planning, architecture. Ended Sunday with a list of open questions: how to design the system, how to merge reports, which "time" ordering to use (vector clocks vs hybrid logical clocks), what Android dev actually needs, how to handle several events at once (like more than one fire).
* Read the slides again this week too.
* Spent time researching through the sync/merge design and some Android questions.

hours:

* project pick call (7 sep): 2h
* TA meeting (8 sep): 0.5h
* Saturday call (12 sep): ~2h
* Sunday call (13 sep): ~2h
* slides: 1h
* sync/merge + Android: 3h

total: 10.5h

W3 (14 - 20 sep)

What happened this week:

### Tuesday
Team meeting (2h). We talked about how to order events across the system and agreed to use vector clocks or DVV for this. Then we went through the use cases one by one and worked out how the services should talk to each other.

### Thursday
I spent some time on Android (1.5h). I went through Activities, Fragments, Room for saving data locally, WorkManager for background sync, and for the P2P part researched about how Bluetooth, WiFi Direct and Nearby Connections work, there pros and cons and which one to pick for our project, since we need these for the tablet side of Firefly.

### Saturday
Report meeting (2h). We talked about how the report should look and what goes in it. We already had most of the ideas from earlier meetings, so it was mostly about writing them down. We also split the tasks so we can finish the Finch release on time.

After that I started PA3 (4h), the domain model chapter. I went through our scenarios and picked out the events, then set the 4 bounded contexts: Field Reporting, Synchronization, Zone Subscription and Device Identity. I worked out Field Reporting in detail: the Incident and Report entities, the Event record, the DVV, and the two-tier merge policy. I also made a context map diagram and a class diagram.

### Sunday
Short sync (1h). We talked about what was still left to write. Then we merged the PA1, PA2, and PA3 parts into one report. I picked up Context View, Wardley Map and Architecture sections. I used what I wrote for PA3 for the Architecture section and contributed to the shared file with Context View and Wardley Map as well. Finally, read through the whole report to check for contradictions and words that did not match between sections. (5h)

### Monday
Lowered the word count and fixed inconsistencies between text and citations & diff subchapters.(2h)

### Hours

Team meeting on event ordering and use cases (Tue): 2h
Android research (Thu): 1.5h
Report meeting and task split (Sat): 2h
PA3 domain model work (Sat): 4h
Short sync (Sun): 1h
Finishing my part of report, merging and checking the report (Sun): 5h
Lowering word count and fixes. (Mon): 2h

total: 17.5h

W4 (21 - 27 sep)

What happened this week:

Went to both lectures.

### Friday
Team meeting (2h). We split up the implementation work for Heron, everyone took a piece of the PoC and I ended up with P2P.

### Saturday
Worked on the Android side of P2P (5h). Got 2 devices to find each other over Nearby Connections and exchange a simple test message. Still rough, but the basic connection works :))

After that, put together the peer review presentation (2h).

### Hours

Lectures (Tue, Thu): 4h
Team meeting on Heron planning (Fri): 2h
P2P implementation (Sat): 5h
Presentation prep (Sat): 2h

total: 13h

W5 (28 sep - 4 oct)

What happened this week:

Went to both lectures. On Tuesday we gave the peer review presentation (~1h). On Thursday we had an online meeting with our TA (~45 min) about our progress and the report.

The rest of the week I worked on P2P. One device can now send a severity change and the other one receives it. I fixed a few problems (location permission, an error message, a device connecting to itself) and wrote a short guide to test it on 2 devices. Then I put my branch together with my teammates' work on the database and the messaging.

### Hours

Lectures (Tue, Thu): 4h
Peer review presentation (Tue): 1h
TA meeting (Thu): 0.75h
P2P severity changes and fixes: 6h
Test guide and putting the branches together: 3h

total: 14.75h

W6 (5 - 12 oct)

What happened this week:

Went to one lecture.

### Monday 5 oct
My P2P branch was merged into master. I tested it on 2 emulators and they exchange operations in both directions. A phone and an emulator do not find each other, so we still need a test on 2 real devices.

### Friday 9 oct
Team meeting (1.5h) and TA meeting in person (30 min). The TA wants experiments linked to our quality attributes, READMEs and all report changes on master. The ADRs do not count for the word limit. After that I started planning the CA3 experiments and the PoC README.

### Monday 12 oct
Met with Alex (4h) to work on the experiments, fix problems and improve the report. We did a first walking test of the P2P range with the S24 and the S25 Ultra. I walked away in a straight line while sending severity changes until Alex stopped receiving them. That was at 50-60m.

### Hours

Lecture: 2h
P2P testing on emulators (Mon 5): 2h
Merge requests (Mon 5): 1h
Team meeting (Fri 9): 1.5h
TA meeting (Fri 9): 0.5h
CA3 plan and PoC README (Fri 9): 3h
Meeting with Alex: experiments, walking test and report (Mon 12): 4h

total: 14h