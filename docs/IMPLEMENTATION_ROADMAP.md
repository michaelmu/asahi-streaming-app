# Shield Streaming App - Implementation Roadmap

Last updated: 2026-04-01 UTC

## Purpose
Define the execution order for building the app from planning into a working vertical slice.

This roadmap is intentionally practical.
It focuses on what should be built first, what can wait, and where the biggest risks are.

---

## 1. Success Criteria for the First Real Milestone

The first major milestone should prove the product, not just the architecture.

## Vertical slice success means:
A user can:
1. launch the app on Shield / Android TV
2. search for a title
3. open details for a movie or episode
4. link Real-Debrid
5. fetch sources
6. choose a source
7. resolve it into a playable stream
8. play it with Media3
9. resume progress later

If that works, the project is real.
If that does not work, everything else is still theory.

---

## 2. Roadmap Overview

### Phase 0 - foundation and decisions
### Phase 1 - app shell + metadata
### Phase 2 - Real-Debrid integration
### Phase 3 - source provider system
### Phase 4 - playback vertical slice
### Phase 5 - polish and hardening

---

## 3. Phase 0 - Foundation and Decisions

## Goal
Make the few high-impact decisions that affect all later code.

## Tasks
- choose package name / app id
- choose UI stack approach
  - Compose for TV vs classic Android TV views
- choose DI approach
  - Hilt recommended
- choose HTTP stack
  - Ktor or OkHttp/Retrofit
- choose persistence stack
  - Room + DataStore recommended
- create repo structure / initial Android project
- define minimum Android SDK and target SDK
- confirm Media3 as playback stack

## Deliverables
- final implementation choices recorded
- empty but compiling Android project
- initial module list created

## Risks
- getting stuck on theoretical debates about frameworks

## Rule
Timebox this phase hard.
Do not let it become a week of build-system philosophy.

---

## 4. Phase 1 - App Shell + Metadata

## Goal
Get the app visible and navigable quickly.

## Tasks
- scaffold modules from `SCAFFOLDING_PLAN.md`
- add navigation host
- create placeholder screens:
  - Search
  - Details
  - Sources
  - Player
  - Settings
- implement TMDb integration
- implement search use case
- implement title details use case
- display movie/show details
- display seasons/episodes for shows
- add recent search history

## Deliverables
- app launches on Android TV
- search works
- details works
- basic navigation works

## Risks
- spending too much time on design polish before flow works

## Rule
Ugly but functional beats polished and fake.

---

## 5. Phase 2 - Real-Debrid Integration

## Goal
Make account linking and core debrid capabilities real.

## Tasks
- implement Real-Debrid API client
- implement device code auth flow
- persist tokens securely enough for local app use
- implement token refresh
- implement account info retrieval
- build account settings screen
- show linked/unlinked state
- handle unlink/revoke

## Deliverables
- user can link RD account
- linked state survives app restart
- account screen shows useful status

## Risks
- auth edge cases
- token refresh bugs
- weak error handling

## Rule
Build auth flows with explicit state handling, not ad-hoc booleans all over the place.

---

## 6. Phase 3 - Source Provider System

## Goal
Produce a source list from a clean, extensible provider pipeline.

## Tasks
- implement provider contract
- implement provider registry
- implement provider enable/disable settings
- implement source normalization
- implement release info parsing
- implement source filtering engine
- implement source scoring/ranking
- implement one or two actual providers
- add cache-check integration for RD hashes
- build source list UI
- show:
  - quality
  - size
  - video/audio flags
  - cache status
  - provider/site label

## Deliverables
- source search returns structured results
- cached results are surfaced well
- source picker is usable on TV

## Risks
- provider instability
- overcomplicated provider abstraction too early
- bad normalization causing junk results

## Rule
Support a **small, reliable subset** of providers first.
Do not chase breadth before quality.

---

## 7. Phase 4 - Playback Vertical Slice

## Goal
Turn a selected source into actual video playback.

## Tasks
- implement source resolution flow
- implement RD magnet resolve / link unrestrict logic
- implement file selection policy for pack/single file cases
- build `PlaybackItem`
- integrate Media3 playback engine
- connect subtitles if available
- implement resume prompt
- store playback progress/bookmarks
- mark completion near end of playback
- handle playback errors and source fallback behavior

## Deliverables
- playable end-to-end flow
- progress persistence works
- app feels like a real streaming app, not a prototype menu

## Risks
- weird playback edge cases on Shield
- bad file selection logic for torrents/packs
- poor failure handling on broken streams

## Rule
Test this phase on the actual target device early, not just emulator.

---

## 8. Phase 5 - Polish and Hardening

## Goal
Turn the vertical slice into a durable app foundation.

## Tasks
- continue watching row
- provider settings screen improvements
- source filter preference persistence
- better loading/error states
- logging and diagnostics
- telemetry/debug screens if useful
- next-episode prep
- maybe cloud browsing
- maybe more providers

## Deliverables
- less fragile app
- better user experience
- easier debugging

## Risks
- endless polish loop before core quality is truly stable

## Rule
Polish should mostly target friction and reliability, not vanity features.

---

## 9. Recommended Exact Build Order

If I were executing this in order, I’d do:

1. create Android project + module skeleton
2. create `core:model`, `domain`, and core interfaces
3. build Search screen + TMDb search
4. build Details screen + episode browsing
5. build Settings/Account screen shell
6. implement Real-Debrid auth
7. implement source provider contract + one provider
8. implement source normalization + ranking
9. build Sources screen
10. implement RD resolution
11. integrate Media3 playback
12. add progress/resume
13. polish errors/settings/provider toggles

That ordering minimizes fake architecture and maximizes working-product feedback.

---

## 10. First Week Target

A realistic first-week target:
- compiling project
- module skeleton exists
- search screen works
- TMDb search works
- details screen works
- settings/account placeholder exists

A strong second-week target:
- RD auth works
- source pipeline skeleton exists
- one provider returns normalized results

A strong third-week target:
- end-to-end playback on Shield

---

## 11. Highest-Leverage Testing Targets

Write tests early for:
- release parsing
- source normalization
- source ranking/filtering
- Real-Debrid file selection logic
- playback progress calculations

These are the places where silent wrongness will hurt the product most.

---

## 12. What Not To Build Yet

Avoid these until the vertical slice works:
- full home/discover ecosystem
- many debrid services
- every Fenlight setting
- massive provider catalog
- Rust shared core
- desktop/web parity planning
- advanced cloud management

These are all tempting. Most are not early blockers.

---

## 13. Best Next Action

The roadmap is now clear enough that the next practical move is:

## Start actual scaffolding
Meaning:
- create the Android project
- create the initial Gradle modules
- create the first package structure
- add placeholder Kotlin files for core contracts

That is the point where planning officially becomes implementation.
