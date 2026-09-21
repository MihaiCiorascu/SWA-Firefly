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

### Monday
Team meeting (2h). We talked about how to order events across the system and agreed to use vector clocks or DVV for this. Then we went through the use cases one by one and worked out how the services should talk to each other.

### Thursday
I spent some time on Android (1.5h). I went through Activities, Fragments, Room for saving data locally, WorkManager for background sync, and for the P2P part researched about how Bluetooth, WiFi Direct and Nearby Connections work, there pros and cons and which one to pick for our project, since we need these for the tablet side of Firefly.

### Saturday
Report meeting (2h). We talked about how the report should look and what goes in it. We already had most of the ideas from earlier meetings, so it was mostly about writing them down. We also split the tasks so we can finish the Finch release on time.

After that I started PA3 (4h), the domain model chapter. I went through our scenarios and picked out the events, then set the 4 bounded contexts: Field Reporting, Synchronization, Zone Subscription and Device Identity. I worked out Field Reporting in detail: the Incident and Report entities, the Event record, the DVV, and the two-tier merge policy. I also made a context map diagram and a class diagram.

### Sunday
Short sync (1h). We talked about what was still left to write. Then we merged the PA1, PA2, and PA3 parts into one report. I picked up Context View, Wardley Map and Architecture sections. I used what I wrote for PA3 for the Architecture section and contributed to the shared file with Context View and Wardley Map as well. Finally, read through the whole report to check for contradictions and words that did not match between sections. (5h)

### Hours

Team meeting on event ordering and use cases (Mon): 2h
Android research (Thu): 1.5h
Report meeting and task split (Sat): 2h
PA3 domain model work (Sat): 4h
Short sync (Sun): 1h
Finishing my part of report, merging and checking the report (Sun): 5h

total: 15.5h