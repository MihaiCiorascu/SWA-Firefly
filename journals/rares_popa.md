# Personal Journal - Rares Popa

## Week 1 (31 August - 4 September 2026)

### What I did this week

- **Lectures.** Went through the lectures and the slides: how the course is organised, team formation, picking a system, and the three stages of the project (Finch, Heron, Swan) together with what is expected for the proof of concept.
- **Team formation.** Spent time on the organisational side of the course and on finding a team to work with.

### Time estimate

| Activity | Estimated time |
| --- | --- |
| Lectures and slides | 4 h |
| Course organisation and team formation | 2 h |
| **Total** | **~6 h** |


## Week 2 (7 - 13 September 2026)

### What I did this week

- **Monday 7 September - team meeting (2h).** We selected and developed the project idea: a native application that helps field workers communicate and synchronise information when the network is unreliable. For the proof of concept we settled on wildfire crews: firefighters submit reports from the field and the application merges them and shows the evolving situation on a live map.
- **Wednesday 9 September - TA meeting (25 min).** The whole team met the TA. We explained the idea we had at that moment and asked what is expected from us for the project and for the proof of concept.
- **Saturday 12 September - team meeting (2h).** We started by going through the deliverables of the project and agreed on a timeline for them, so that everyone knows what has to be ready and by when. After that we moved to the idea itself, and while discussing it in detail we noticed that each of us was describing the application in a different way. Instead of settling it quickly in the meeting, we decided it deserved proper work: the task until Sunday was that everyone writes down their own detailed version of how the app should work.
- **Saturday evening and Sunday morning - work on my own.** I wrote down concretely how I think the application should work: who uses it, what happens step by step, and what the app has to do in each case. I also went through the use cases we could support and checked which technologies exist for the parts I was unsure about, so that on Sunday I could say what is realistic to build and not only what sounds good.
- **Sunday 13 September - team meeting (2h).** Everyone presented what they had prepared. Because we all came with something written, the discussion was much easier than on Saturday and we agreed on one version of the idea and on the scope. At the end we split the work into clear tasks, each with an owner, to be finished by the next meeting.
- **Lectures and slides.** Went through the lecture material again this week.

### Time estimate

| Activity | Estimated time |
| --- | --- |
| Team meeting to select the project (7 Sept) | 2 h |
| TA meeting (9 Sept) | 0.5 h |
| Team meeting (12 Sept) | 2 h |
| Own work: writing down the app, use cases, technologies | 3 h |
| Team meeting (13 Sept) | 2 h |
| Lectures and slides | 2 h |
| **Total** | **~11.5 h** |


## Week 3 (14 - 20 September 2026)

### What I did this week

- **Tuesday 15 September - team meeting (2h).** We went deep into how we order events across the system and agreed to use vector clocks or DVV for this. After that we went through the use cases in detail and worked out how the communication between the services should actually happen.
- **Tuesday - own work (1.5h).** I made the diagram in Lucidchart based on what we agreed, so that the whole team has one thing to look at and guide themselves by.
- **Saturday 19 September - team meeting (2h).** We discussed the report: how it should look and what goes in it. We already had all the ideas from the previous meetings, so it was mostly a matter of writing them down. We also divided the tasks so that we finish the Finch release in time.
- **Saturday - own work (2.5h).** After the meeting I initialized the project with the high level directories and structure.
- **Sunday 20 September - short sync (1h).** We talked about the part of the report that we still have to write.
- **Sunday - own work (1.5h).** I detailed the Lucidchart diagram further, as we are preparing to get into more code writing.
- **Sunday - own work (1h).** I set up the server module with its main parts, so that we have a hello world equivalent flow end to end: the dependencies, an entity, the repository, the mappers and the service.
- **Lectures and slides.** Went through the lecture material and the slides on the Finch deliverables.

### Time estimate

| Activity | Estimated time |
| --- | --- |
| Team meeting (15 Sept) | 2 h |
| Own work: Lucidchart diagram | 1.5 h |
| Team meeting (19 Sept) | 2 h |
| Own work: project initialization and structure | 2.5 h |
| Team sync (20 Sept) | 1 h |
| Own work: detailing the Lucidchart diagram | 1.5 h |
| Own work: server module setup (dependencies, repository, service, mappers) | 1 h |
| Lectures and slides | 2 h |
| **Total** | **~13.5 h** |


## Week 4 (21 - 27 September 2026)

### What I did this week

- **Friday 25 September - team meeting (2h).** We discussed what comes after the Finch release: what the Heron release should contain, both for the proof of concept and for the report.
- **Friday - own work (2h).** After the meeting I started on the Kafka integration. I looked into how the messages can be monitored, what is needed for that, and then started the implementation.
- **Saturday - own work (2h).** I started my part of the Heron report, CA2 (Architectural Decision Records). I have to write three ADRs, each with the chosen option and three alternatives, so I went through which decisions of ours are challenging and relevant enough to be worth documenting.
- **Presentation preparation (2h).** We prepared and scheduled the peer review presentation, which took some coordination because our availability did not align well with the other teams' schedules.
- **Lectures and slides.** Went through the lecture material again this week.

### Time estimate

| Activity | Estimated time |
| --- | --- |
| Team meeting about the Heron release (25 Sept) | 2 h |
| Own work: Kafka integration and message monitoring | 2 h |
| Own work: Heron report, CA2 (Architectural Decision Records) | 2 h |
| Presentation preparation and scheduling | 2 h |
| Lectures and slides | 2 h |
| **Total** | **~10 h** |


## Week 5 (28 September - 4 October 2026)

### What I did this week

- **Compared the messaging options (4h).** Looked at Kafka, NATS JetStream, a queue table in Postgres and plain NATS. Our volume is low, so throughput did not decide it. A Kafka client has to reach every broker, a NATS client uses one endpoint, which is better for tablets on bad networks. We switched to NATS JetStream.
- **Message format and subject names in core (1h).** One definition the device and the server both use.
- **Server side (3h).** Connects to NATS, declares the stream, consumes the operations and passes them to a service.
- **Device side (2h).** Function that sends an operation on the subject of its report. Set up docker compose and tested the flow from the device to the server.
- **CA2 drafts (2h).** Started writing the ADRs. The messaging choice is one of them.
- **Lectures and slides (2h).**

### Time estimate

| Activity | Estimated time |
| --- | --- |
| Comparing the messaging options | 4 h |
| Message format and subjects in core | 1 h |
| Server side | 3 h |
| Device side | 2 h |
| CA2 drafts | 2 h |
| Lectures and slides | 2 h |
| **Total** | **~14 h** |


## Week 6 (5 - 11 October 2026)

### What I did this week

- **Friday 9 October - team meeting (1h) and TA meeting (30 min).** The team meeting, then the TA went over her feedback with us.
- **ADR 1 and ADR 2 writing (8h).** Wrote both ADRs for the Heron report.
- **ADR 2 research (7h).** Read in detail about short polling, long polling, SSE and WebSocket, so we can start implementing next week.
- **Report review (1.5h).** Checked that the terms in my ADRs match the rest of the report.
- **Lectures and slides (2h).**

### Time estimate

| Activity | Estimated time |
| --- | --- |
| Team meeting (9 Oct) | 1 h |
| TA meeting (9 Oct) | 0.5 h |
| ADR 1 and ADR 2 writing | 8 h |
| ADR 2 research on the four options | 7 h |
| Report review for consistent terms | 1.5 h |
| Lectures and slides | 2 h |
| **Total** | **~20 h** |
