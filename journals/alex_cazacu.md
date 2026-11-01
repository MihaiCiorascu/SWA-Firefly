## Journal - Alex Cazacu

### Week 1

Attended both lectures on campus. Looked for a team, found one and worked on the project proposal for one meeting (parking management). Switched to another team due to personal circumstances. Went through slides again and read up a bit on distributed system design.

#### Time spent

* Lectures (4h)
* Finding a team (1h)
* Meeting to develop the project proposal (2h)
* Finding a second team (0.5h)
* Own research (3h)

Total: 10.5h

### Week 2

Studied lecture slides. Attended first meeting with the new team. Discussed the new interesting idea. Prepared for the TA meeting with some questions about how broad the scope should be. Attended the TA meeting. Attended two following team meetings, Saturday and Sunday. Wrote the AI Contract. Did some deep research of the problem, stake holders and risks. Between the two team meetings, I explored different PoC architectures and ideas. Did some early draft writing for the final report, synthesising the findings of my research into emergency personnel responses in natural disasters.

#### Time spent

* Lecture slides (2h)
* First team meeting (2h)
* TA meeting (0.5h)
* Second team meeting (2h)
* Third team meeting (2h)
* Writing the AI contract (0.5h)
* Own research into emergency response and early report draft (3h)
* Exploring system design architecture and PoC ideation between the two meetings (4h)

Total: 16h

### Week 3

Attended a meeting with the team where we discussed how events should be ordered and how should we determined concurrent edits. We arrived at DVVs, dotted version vectors. We then discussed how this will integrate into our system and how communication between subsystems should happen. I did some of my own research into this topic and looked over the lecture slides about the finch deliverables. On Saturday we met and distributed tasks amongst ourselves. I took the PA1 part of the report because I did some research into wildfire response and I could write some relevant examples and references. I also did a very barebones part of the merging library, which only supports severity field reports for now. On Sunday we did most of the report work and synced up everything towards EOD. On Monday we looked over the report a final time, and I worked on the merging implementation.

* Lecture slides (1h)
* Meeting Tuesday (2h)
* Own research into DVVs and normal VVs (1h)
* Team meeting Sat (2h)
* Report work (~6h across two days including quick syncs and looking over edits)
* Merge library implementation (3h)

Total: 15h

### Week 4

This week I looked over the lecture slides and met with the team on Friday to discuss what we will do following our Finch release. We talked about what should we do for the Heron release, in terms of PoC and report. I also did some of my own research into how this system should be fault tolerant and scalable when deployed. We prepared and scheduled the presentation, which was pretty difficult since our schedules didn't really align with the other teams'. I also thought about how to write a testing harness and suite for the merge library.

* Lecture slides (2h)
* Meeting Friday (2h)
* Presentation prep (2h)
* Own research distributed systems fault tolerance (2h)
* Testing for merge library (1h)

Total: 9h

### Week 5

Most important thing I did this week was to rewrite the merge library to support all types of fields we decided on in the report. I also wrote tests that verify that the implementation satisfies the requirements we picked. I also started writing an ADR about our approach to security. I studied the material in the report and organized some meeting minutes and presentations for the team.

* Merge library re-write with tests (6h)
* ADR draft (2h)
* Lecture & slides (2h)
* Misc tasks (1h)

Total: 11h

### Week 6

This week I finished the security ADR. I created the presentation for the TA meeting and met with the team on Friday to rehearse for the presentation and talk about future steps on the project. We fixed some issues with the device code. I also worked with a teammate to make the library compatible with the rest of the codebase. I also had a long meeting with a teammate in which we worked on CA3 and ran the experiments in the report. I then did some more of my own research into what experiments should we do for Swan.

* Finalizing security ADR (2h)
* Meeting Friday with team (1.5h)
* Meeting with TA (0.5h)
* CA3 meeting and running experiments (4h)
* Own research into future experiments (2h)
* Final touches for Heron release on the report (1h)
* Sync with teammate for him to integrate my merge library in codebase (1h)

Total: 12h

### Week 7

This week I spent a lot of time doing the peer reviews. I attended a meeting with the team, where we discussed what else should we do for the rest of the project. I picked implementing security since I did a lot of research on it and wrote the ADR for it. However, as I was implementing it, I started to realise that it is a lot of work that provides little benefit to the goal of the PoC. I checked the example reports and I saw that none actually implement security. One of the peer reviews I performed implemented authentication, and I felt it was non-constructive to the goals of the PoC. I wrote up my concerns to the team and we voted to not implement it. I then spent quite a while reviewing some big MRs from the team, including creating a test that according to our spec should pass but it fails for the current implementation.

* Peer review (8h)
* Team meeting Saturday (2h)
* Security research (4h)
* Security implementation (5h)
* Check example reports and decide against security (2h)
* Big MRs on Monday reviewed (4h)

Total: 25h

### Week 8

In week 8, I implemented CI for this project as we did not have it. I had some trouble with Docker, but I worked it out in the end. I applied some fixes to the report that a lot of people mentioned in the peer reviews. I worked on linking the whole project together. I did the work on the merge library while Radu did the rest. I spent some time reviewing Radu's work. We also met with the TA on Friday to discuss the final release.

* CI implementation (4h)
* Report fixes (1h)
* Mergecore linking work, including research on grid zones (7h)
* Reviewing Radu's work (2h)
* TA meeting (0.5h)

Total: 14.5h

### Week 9

In the last week I met with Mihai to do the demo. We had a problem where the app no longer worked because both of our phones updated to the newest Android the day before and P2P did not work. We managed to fix it by finding a new, missing permission, and then we spent time designing the scenarios and recording them on the different devices. Radu offered to edit the video. I then spent quite some time going through the report again and the whole codebase to prepare for the presentation. We met with the entire team to rehearse the presentation and I also prepared at home. On Thursday, we attended the presentation day and presented our project. Then, during the weekend I did some proofreading of the report again in preparation for the Swan release, including reviewing Radu's PoC section.

* Fixing the demo bug (4h)
* Creating the scenarios and recording videos (4h)
* Reading report and codebase (3h)
* Meeting with the team to rehearse the presentation (2h)
* Rehearsing my part (2h)
* Report proofreading and reviewing PoC section (3h)

Total: 18h
