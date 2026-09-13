# Journal Stefan Avram

## W1 (31 aug - 4 sep)

First week was mostly getting into the course. I read through the slides to see how everything works: team formation, picking a system, the three stages of the project (finch, heron, swan) and general organizational business.
hours:

- reading slides: 1h
- getting to know the course + organisational stuff (finding a team etc): 1.5h

total: 2.5h

## W2 (7 - 13 sep)

Had a team meeting on Monday the 7th of September (~2h) where we talked more about the project. We're planning on creating a native app for helping field workers sync better while working. The PoC will be focused around firefighting, more specifically helping wildfire crews communicate in a way that allows for better situational awareness than using radios (how its currently done). We want firefighters to be able to create and communicate reports about the state of the wildfire, with a live map to show the way the fire is evolving. Since many such reports could come from different teams, there must be a way to merge them or settle which information.

On tuesday we had our first meeting with our TA, about 30 min. Then saturday and sunday we did two meetings back to back, around 2h each, where we talked about the problem statement, the design, planning and the high level architecture. In the sunday meeting we settled on some main open questions, such as system design, report merging mechanism, defining the "time"-related ordering relation we'll use for merging (vector clocks, hybrid logical clocks, etc.), what native android software development requires, how the app would handle multiple events (e.g. multiple wildfires across the globe), etc.

After the saturday meeting I spent ~3h by myself reading about system architecture and planning and working on a high level design for the system. Also went through the slides this week, for another ~3h.

I will look into handling multiple events (I am currently thinking PubSub pattern + WS for updating client states), and definig the "time" ordering relation.

hours:

- team meeting to pick the project (7 sep): 2h
- first TA meeting (8 sep): 0.5h
- team meeting saturday 12 sep: ~2h
- team meeting sunday 13 sep: ~2h
- own research on system architecture / planning + high level design: 3h
- reading slides: 3h

total: ~12.5h
