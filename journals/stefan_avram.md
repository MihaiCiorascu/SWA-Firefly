# Journal Stefan Avram (@savram)

## W1 (31 aug - 4 sep)

First week was mostly getting into the course. I read through the slides to see how everything works: team formation, picking a system, the three stages of the project (finch, heron, swan) and general organizational business.
hours:

- reading slides: 1h
- getting to know the course + organisational stuff (finding a team etc): 1.5h

total: 2.5h

## W2 (7 - 13 sep)

Had a team meeting on Monday the 7th of September (~2h) where we talked more about the project. We're planning on creating a native app for helping field workers sync better while working. The PoC will be focused around firefighting, more specifically helping wildfire crews communicate in a way that allows for better situational awareness than using radios (how its currently done). We want firefighters to be able to create and communicate reports about the state of the wildfire, with a live map to show the way the fire is evolving. Since many such reports could come from different teams, there must be a way to merge them or settle which information.

On Wednesday the 9th we had our first meeting with our TA, about 30 min. Then saturday and sunday we did two meetings back to back, around 2h each, where we talked about the problem statement, the design, planning and the high level architecture. In the sunday meeting we settled on some main open questions, such as system design, report merging mechanism, defining the "time"-related ordering relation we'll use for merging (vector clocks, hybrid logical clocks, etc.), what native android software development requires, how the app would handle multiple events (e.g. multiple wildfires across the globe), etc.

After the saturday meeting I spent ~3h by myself reading about system architecture and planning and working on a high level design for the system. Also went through the slides this week, for another ~3h.

I will look into handling multiple events (I am currently thinking PubSub pattern + WS for updating client states), and definig the "time" ordering relation.

hours:

- team meeting to pick the project (7 sep): 2h
- first TA meeting (9 sep): 0.5h
- team meeting saturday 12 sep: ~2h
- team meeting sunday 13 sep: ~2h
- own research on system architecture / planning + high level design: 3h
- reading slides: 3h

total: ~12.5h

## W3 (14 - 20 sep)

On Tuesday we had a team meeting (~2h) about merging strategies, so how reports from different crews get merged when their tablets sync. Separately I spent ~3h reading about distributed systems, mostly vector clocks and dotted version vectors, and going through the lecture slides.

Saturday morning we had another meeting (~1.5h) to split the tasks for the Finch release and talk through the architecture. In the afternoon, from 15:00 to 20:00, I worked on the PA2 (Function) chapter of the report: the vision, key scenarios, architecturally significant requirements, quality attributes and tradeoffs, the delivery roadmap with a diagram, and a short pricing section. I also looked at how comparable tools are priced and trimmed the chapter down to fit the word budget.

On Suday, the team working on the report this week met for another hour, where we discussed more on architecture and the status of writing the report. Afterwards, we merged the parts (PA 1, 2 & 3) of the report into a version that contains all of them, fixing any issues that comes with that.

hours:

- team meeting on merging strategies (tue 15 sep): 2h
- reading on distributed systems (vector clocks, dotted version vectors) + lecture slides: 3h
- team meeting on task splitting and architecture (sat 19 sep): 1.5h
- writing the PA2 (Function) chapter (sat 19 sep, 15:00 - 20:00): 5h
- sunday discussion (20 sept): 1h
- merging and fixing small errors(e.g. inconsitencies in terminology): 4h

total: 16.5h

## W4 (21 - 27 sep)

This week was mostly about the peer review presentation. I skipped the team meeting on Friday and spent those two hours making some small fixes to the presentation instead. I also spent another 2h preparing for the presentation itself. Apart from that I did some more research on my own into distributed systems (~3h) and went through the lecture slides (~2h).

hours:

- small fixes to the presentation (fri 25 sep, instead of the team meeting): 2h
- preparing for the presentation: 2h
- own research on distributed systems: 3h
- lecture slides: 2h

total: 9h

## W5 (28 sep - 4 oct)

On Tuesday we had the peer review presentation (~1h). On Thursday we had another meeting with our TA, which took about 45 minutes. I also went through the lectures for ~2h.

The rest of the week I worked on setting up SQLite on the tablet side. The idea is that tablets save all reports locally first and compare with the server once there is connectivity again. For now it's only the first step: the tablet opens a local SQLite database and there is a small example API (ReportService) that the rest of the device code can use without touching SQL, with severity as the only field. The tables themselves come from the database schema MR, so that the device and server schemas match. What still needs to be done is written down as TODOs.

hours:

- peer review presentation (tue 29 sep): 1h
- TA meeting (thu 1 oct): 0.75h
- lectures: 2h
- setting up SQLite on the tablet side: 6.25h

total: 10h

## W6 (5 - 11 oct)

On Friday we had a team meeting (~1h) and a 30 min meeting with our TA, where she went over her feedback with us.

The rest of Friday I worked on the tablet side. First I moved the local storage onto the database schema that got merged. Every change is now saved as an event (with the tablet's next sequence number and causal context), and the reports table gets updated from that event in the same transaction. Then I merged the android module into the device module, so there is only one tablet app now. sqlite-jdbc and Jackson 3 don't fit an android app, so the storage now uses android's own SQLite and the NATS sender uses Gson. The two CLIs became a Reports screen and a Server screen in the app, next to the P2P screen. Since the schema uses STRICT tables the app needs Android 14 or newer. I tested it on the emulator against the server running locally (creating reports, setting severity, sending to NATS).

Over the weekend I worked on CA1 for the Heron release. I first looked at what the slides ask for and how the JitAuction sample report does it. Then I wrote the C4 views (container, components for the tablet and the server, code, runtime and deployment) and made the diagrams for them. On Sunday I went through the older parts of the architecture chapter and fixed what didn't match the implementation anymore (Kafka, WorkManager, Room). I also renamed the report to the Heron report, added a table of contents and put the command staff web client in the architecture.

hours:

- team meeting (fri 9 oct): 1h
- TA meeting (fri 9 oct): 0.5h
- saving report changes as events on the shared schema: 2h
- merging the android and device modules into one tablet app (android sqlite, gson, reports and server screens, tests): 4.5h
- testing the app on the emulator against the server: 1h
- slides + JitAuction sample report for CA1: 1h
- CA1 container view (fri 9 oct): 1.5h
- CA1 component, code, runtime and deployment views (sat 10 oct): 5h
- syncing the architecture chapter with the implementation, title, table of contents, web client, wording (sun 11 oct): 3.5h

total: 20h

## W7 (12 - 18 oct)

On Monday and Tuesday I reworked the CA1 part of the report after review: I trimmed the deployment view, reworded the contrasts in the function sections and reworked the container, component, code and runtime views. On Wednesday I worked on the merge core: RESOLVE_CONFLICT now settles the candidates it supersedes, locations off the map are rejected and a value can replace the values its writer had seen.

From Thursday to Sunday I worked on the tablet app. Reports, incidents and conflicts are now built from the merge core, and the screens and P2P share one store, so two tablets sync and merge over P2P. I added an incidents tab, editors for every report field and settling conflicts, where only the values the crew leader was shown get settled. The tablet now sends its real events to the server, and the report form and tabs fit on a phone screen. I also updated the docs for the tablet app.

On Saturday we had a team meeting to plan the next steps (resilience experiment, report feedback, DVVs, the server-tablet link, merge edge cases and the final presentation).

hours:

- reworking the container, component, code, runtime and deployment views after review (mon 12 - tue 13 oct): 4h
- merge core: RESOLVE_CONFLICT, off-map locations, replacing seen values (wed 14 oct): 5h
- tablet app on the merge core, shared store, p2p sync and merge (thu 15 - fri 16 oct): 7h
- incidents tab, report field editors, settling conflicts, sending real events to the server, phone layout (fri 16 - sun 18 oct): 6h
- docs for the tablet app (sun 18 oct): 1h
- team meeting (sat 17 oct): 2h

total: 25h
