# Asahi Execution Plan — Watch History Architecture and UI

Last updated: 2026-04-04 UTC
Status: COMPLETE
Owner: shield-tv-bot + Mike
Location: `docs/exec-plans/in_progress/2026-04-04-watch-history-architecture-and-ui.md`
Supersedes: none

## Purpose

This plan is for adding watch history support for movies and TV shows.

The intended product behavior is:
- the app records watched movie and TV entries locally
- when users return to the Movies or TV Shows menus, they can open a Watch History view
- watch history lists are shown in descending order by most recent watch activity
- movie history is movie-level, while TV history is episode-level
- movie history and TV history are browsed separately by entry point, while storage may remain unified underneath

This plan covers both storage/architecture and user-facing behavior.
The goal is to add a simple, TV-friendly history feature without accidentally turning it into a large library/progress-tracking system unless that becomes explicitly desirable later.

---

## How to Use This Plan

### Before starting a session
1. Read this file top-to-bottom.
2. Check `Current Focus`.
3. Check `Open Questions / Decisions Needed`.
4. Check the latest `Progress Log` entry.
5. Update `Session Start` before coding.

### While implementing
Update this file during the work, not after the fact.

Minimum required updates:
- task status changes
- progress log entries after meaningful milestones
- scope changes when behavior or storage design shifts
- validation notes after completed work

### Completion rule
A task is only `DONE` when:
- code landed
- relevant validation happened and was recorded
- follow-up work is captured here if needed

---

## Status Legend

- `TODO` = not started
- `IN_PROGRESS` = actively being worked
- `BLOCKED` = waiting on a prerequisite or decision
- `DONE` = implemented and validated
- `DEFERRED` = intentionally postponed
- `OPTIONAL` = not required unless chosen

---

## Current Focus

This pass is complete.

Remaining follow-up is optional polish only:
- optional history long-press/action refinement
- optional broader watched-state surfacing beyond movie search results and TV episode rows
- optional stronger emulator/device UX validation for focus/discoverability

---

## Repository Reality Check

Before implementation begins, confirm:
- the app currently has local favorites persistence that can serve as a pattern for a parallel history subsystem
- playback/session persistence already exists and may offer the cleanest write seam for history recording
- Movies and TV entry-point browsing currently routes through shared renderer/state patterns that history can likely reuse
- TV history requirements differ from favorites because TV entries should be episode-level, not show-level

---

## Locked Decisions

- movie watch history is movie-level
- TV watch history is episode-level
- history should be stored locally in a lightweight subsystem, not a large library database
- storage should support future watched-indicator lookups for movie search results and TV episode rows

---

## Background / Review Summary

From the current app structure:
- the app already has local persistence for favorites and playback sessions
- Movies and TV now have favorites entry points that establish a browse pattern worth reusing
- the app already tracks some playback/resume state, which may provide a natural seam for history writes
- the UI is card-based and TV-first, so history should likely reuse existing list patterns instead of inventing a separate heavy screen
- the same stored watch-history data should later support watched indicators in search results and episode lists

That suggests the right initial approach is:
- add a **small dedicated watch history subsystem**
- keep storage local and straightforward
- store enough metadata to render history rows locally and support future watched-indicator lookups
- separate movie vs TV browsing by entry point while keeping storage unified if that simplifies implementation
- treat TV history as episode-level in the first pass

---

# Phase A — Watch history model and storage

## A1. Define watch history data model
Status: DONE
Priority: High

### Goal
Create a local watch history model that can render movie/TV history lists and sort deterministically by recency.

### Why this matters
If history only stores IDs, the history menus may depend on extra lookups.
If it stores too much playback-specific data, it can become an accidental analytics/progress subsystem.

### Proposed sub-steps
- [DONE] define a `WatchHistoryItem` model or equivalent
- [DONE] include enough fields for local rendering and watched-indicator lookups, likely:
  - media type
  - stable ids
  - title
  - year
  - poster/backdrop/artwork URL if available
  - subtitle/secondary label if useful
  - last watched timestamp
  - season/episode context for TV entries
  - optional episode title if available
- [DONE] define sort behavior as most-recent-watch first
- [DONE] define duplicate behavior (same item watched again should refresh recency rather than duplicate)

### Validation
- model is sufficient to render history lists without immediate refetch
- repeat-watch behavior is explicit and deterministic

---

## A2. Add local watch history storage
Status: DONE
Priority: High

### Goal
Persist watch history locally in a simple, evolvable format.

### Why this matters
History should survive app restarts and feel instant to browse.
A file-based approach is probably sufficient unless scope expands.

### Proposed sub-steps
- [DONE] choose storage approach (JSON file)
- [DONE] support record/list/clear-or-trim operations as needed
- [DONE] preserve descending watched-time order
- [DONE] define safety behavior for invalid/corrupt storage
- [DONE] add focused tests for order/deduplication/update behavior

### Validation
- history persists across restarts
- repeated watch events refresh recency
- load failures fail safely

---

# Phase B — History write rules

## B1. Decide what counts as “watched” for history
Status: DONE
Priority: High

### Goal
Define when the app should write a history entry.

### Why this matters
If history writes too early, it becomes cluttered and misleading.
If it writes too late, users will think it is broken.

### Proposed sub-steps
- [DONE] decide whether history writes on playback start, successful playback preparation, resume past threshold, or playback exit after progress
- [DONE] decide whether source-resolution-only flows should count
- [DONE] define rules separately for movies vs episodes
- [DONE] choose a pragmatic first-pass rule that matches current app seams
- [DONE] ensure the write rule produces stable data usable for future watched indicators

### Validation
- write rule is documented and implemented consistently
- normal playback flows create expected history entries

---

## B2. Record history from playback flow
Status: DONE
Priority: High

### Goal
Actually write history entries from the chosen playback seam.

### Proposed sub-steps
- [DONE] connect the chosen playback lifecycle event to history recording
- [DONE] store the right metadata for movies and shows
- [DONE] ensure repeat watches refresh recency
- [DONE] avoid duplicate spam from the same viewing session if possible for the first pass

### Validation
- watching a movie creates/updates movie history
- watching a show creates/updates TV history
- repeated viewing moves the item to the top

---

# Phase C — History browse integration

## C1. Add Watch History entry points to Movies and TV menus
Status: DONE
Priority: High

### Goal
Expose watch history as a top-level browse option in Movies and TV flows.

### Proposed sub-steps
- [DONE] add Watch History entry point in Movies flow
- [DONE] add Watch History entry point in TV Shows flow
- [DONE] keep the entry aligned with the current favorites/browse interaction pattern for a first pass by reusing the existing menu entry seam

### Validation
- history entry is easy to discover in both Movies and TV areas
- navigation stays consistent

---

## C2. Render watch history lists sorted by most recent watch
Status: DONE
Priority: High

### Goal
Show movie/show watch history lists newest first.

### Proposed sub-steps
- [DONE] add history destination/view state for movie history
- [DONE] add history destination/view state for TV history
- [DONE] render local history items in descending watched-time order
- [DONE] handle empty-state UI cleanly

### Validation
- lists render correctly for movies and TV separately
- most recently watched item appears first
- empty state is clear and not broken-looking

---

# Phase D — Watched indicators and optional polish

## D0. Add watched indicators in browsing flows
Status: DONE
Priority: Medium

### Goal
Use stored history data to show watched state outside the history lists themselves.

### Proposed sub-steps
- [DONE] add watched indicator support for movie search results
- [DONE] add watched indicator support for TV episode rows
- [DONE] reuse stable history keys so indicators are driven by the same underlying storage model

### Validation
- watched movies show a watched badge in movie search results
- watched TV episodes show watched state in the episode picker/list

---

## D1. Add long-press item actions in history lists
Status: OPTIONAL
Priority: Medium

### Notes
This could mirror the favorites pattern and provide actions like open details or remove history entry if desired.
Not required for the first pass unless requested.

## D2. Add per-item or global history clearing controls
Status: DONE
Priority: Medium

### Notes
Implemented in a pragmatic first pass via history-list long-press actions and clear-history handling from the same modal flow.

---

## Recommended Order

1. A1 Define watch history data model
2. A2 Add local watch history storage
3. B1 Decide what counts as watched
4. B2 Record history from playback flow
5. C1 Add Watch History entry points to Movies and TV menus
6. C2 Render watch history lists sorted by most recent watch
7. Optional D-items if still justified

---

## Open Questions / Decisions Needed

### Q1. Should watch history store only IDs, or enough display metadata to render locally?
Current recommendation:
Store enough metadata to render history rows/cards locally without an immediate network round-trip.

### Q2. What is the right rule for when an item becomes history?
Decision:
Use successful playback preparation/start as the first-pass history write seam.

Rationale:
This app already has a clear success path in playback preparation where continue-watching and session persistence are updated. Reusing that seam keeps the implementation simple and avoids writing history from mere detail browsing or source resolution.

### Q3. Should movies and TV history be separate lists or one unified store filtered by entry point?
Current recommendation:
Use one unified local store with media type on each item, then filter by entry point.

### Q4. For TV shows, should history track show-level entries, episode-level entries, or both?
Decision:
Episode-level for the first pass.

Rationale:
This keeps TV history honest about what was actually watched and gives the stored data enough fidelity to support watched indicators on episode rows later.

---

## Risks / Watchouts

- writing history too early will create noisy junk entries
- writing history too late will make the feature feel unreliable
- storing too little metadata makes browse lists dependent on remote refetches
- storing too much playback detail risks accidental complexity creep
- if history and continue-watching overlap awkwardly, the product model may become confusing
- if watched-indicator lookups are not considered in the model now, future UI integration may require awkward rework

---

## Validation Notes / Honesty Check

### Plan setup
- Validated by: reviewed current app direction and recent favorites implementation pattern
- Not validated: actual history-write seam in playback flow was not implemented yet at plan creation time
- Known uncertainty: exact write trigger needed to be chosen against the real playback lifecycle

### Phase A / B1 foundation
- Validated by: focused storage tests plus full Gradle unit/build validation for the app
- Not validated: end-to-end on emulator/device yet
- Known uncertainty: first-pass write rule uses playback preparation success, which is pragmatic but may later want refinement if it proves too eager in real use

### Phase C browse integration
- Validated by: full Gradle unit/build validation for the app
- Not validated: end-to-end on emulator/device yet
- Known uncertainty: Home/menu layout expansion now exposes both favorites and history for Movies/TV, but real remote testing would still be useful for focus/discoverability

### Phase D watched indicators
- Validated by: full Gradle unit/build validation for the app
- Not validated: end-to-end on emulator/device yet
- Known uncertainty: watched indicators currently cover movie search results and TV episode rows, but broader show-level summaries are still out of scope

---

## Progress Log

### 2026-04-04 19:53 UTC
- Created a dedicated watch history architecture/UI plan based on Mike’s request.
- Scoped the first pass around local persistence, playback-driven history recording, and movie/TV menu entry points with newest-first ordering.
- No implementation work completed under this plan yet.

### 2026-04-04 20:01 UTC
- Refined product direction: movie history remains movie-level, but TV history should be episode-level.
- Added a design requirement that watch-history storage should also support future watched indicators in movie search results and TV episode lists.
- This pushes the data model toward stable per-movie and per-episode lookup keys instead of browse-only storage.

### 2026-04-04 20:08 UTC
- Completed A1/A2 by adding `WatchHistoryItem`, JSON encoding/decoding, `WatchHistoryStore`, and focused storage tests.
- Completed B1 by choosing successful playback preparation/start as the first-pass history write seam.
- Wired playback success in `MainActivity` to record watch history via `WatchHistoryCoordinator`, alongside existing continue-watching and playback-session persistence.
- Validation: `./gradlew testDebugUnitTest assembleDebug` passed.

### 2026-04-04 20:13 UTC
- Completed C1/C2 by adding movie and TV watch-history browse entry points and reusing the existing results renderer for history lists.
- Added dedicated history browse state so results UI can distinguish watch history from favorites and normal search results.
- Added history-to-results mapping with watched badges and episode-aware subtitles for TV entries.
- Validation: `./gradlew testDebugUnitTest assembleDebug` passed.

### 2026-04-04 20:17 UTC
- Completed D0 by using watch-history data to decorate movie search results with watched badges and TV episode rows with watched markers.
- Added stable-key lookup helpers in `WatchHistoryCoordinator` so browse indicators are driven by the same data model as history storage.
- Kept the scope intentionally narrow: indicators now cover movie search results and episode lists, not broader show-level summaries.
- Validation: `./gradlew testDebugUnitTest assembleDebug` passed.

### 2026-04-04 20:22 UTC
- Completed B2/D2 and the remaining Home integration work by expanding the Movies/TV home shortcuts to expose both Favorites and Watch History simultaneously.
- Added first-pass history management: long-press on history items can remove a single entry, and the same modal flow can clear the current movie/TV history list.
- This makes the watch-history pass functionally complete for the originally intended browse/use/indicator scope.
- Validation: `./gradlew testDebugUnitTest assembleDebug` passed.

---

## Scope Changes

### 2026-04-04
- New plan created specifically for watch history architecture + UI.
- Initial requested behavior is browse-focused: expose history under Movies and TV menus with descending recency.
- History write rules are treated as first-class scope because they determine whether the browse experience feels trustworthy.
- Stored history data should be designed so it can later drive watched badges/indicators in search results and episode lists.
- Future hooks to preserve: stable per-movie and per-episode lookup keys for watched-indicator rendering.
- Home/menu layout was expanded so Movies and TV now expose browse, favorites, and watch history simultaneously.
- Watched indicators currently focus on movie search results and TV episode rows; broader watched-state surfacing can be added later if still useful.

---

## Session Start

### 2026-04-04 20:25 UTC
Intended task: close the watch-history plan after the remaining management/home-integration work validated successfully.

---

## Definition of Done

This plan is complete for its intended pass when:
- accepted watch history items are marked `DONE`, `DEFERRED`, `OPTIONAL`, or removed
- validation is recorded for completed work
- follow-up work is explicitly captured

Result: complete for this pass. Remaining items are optional polish only.
