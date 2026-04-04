# Asahi Execution Plan — Watch History Architecture and UI

Last updated: 2026-04-04 UTC
Status: IN_PROGRESS
Owner: shield-tv-bot + Mike
Location: `docs/exec-plans/in_progress/2026-04-04-watch-history-architecture-and-ui.md`
Supersedes: none

## Purpose

This plan is for adding watch history support for movies and TV shows.

The intended product behavior is:
- the app records watched movie and TV entries locally
- when users return to the Movies or TV Shows menus, they can open a Watch History view
- watch history lists are shown in descending order by most recent watch activity
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

**Current phase:** Phase A — watch history model and storage design

**Immediate target:** define the local storage/model shape and the event rules for when a movie/show should enter watch history.

**Why this now:**
Watch history touches multiple layers:
- playback/session completion signals
- media models
- local persistence
- Movies/TV menu entry points
- list rendering and sort behavior

If the write rules are vague, the history list will feel noisy or unreliable.

---

## Background / Review Summary

From the current app structure:
- the app already has local persistence for favorites and playback sessions
- Movies and TV now have favorites entry points that establish a browse pattern worth reusing
- the app already tracks some playback/resume state, which may provide a natural seam for history writes
- the UI is card-based and TV-first, so history should likely reuse existing list patterns instead of inventing a separate heavy screen

That suggests the right initial approach is:
- add a **small dedicated watch history subsystem**
- keep storage local and straightforward
- store enough metadata to render history rows locally
- separate movie vs TV browsing by entry point while keeping storage unified if that simplifies implementation

---

# Phase A — Watch history model and storage

## A1. Define watch history data model
Status: TODO
Priority: High

### Goal
Create a local watch history model that can render movie/TV history lists and sort deterministically by recency.

### Why this matters
If history only stores IDs, the history menus may depend on extra lookups.
If it stores too much playback-specific data, it can become an accidental analytics/progress subsystem.

### Proposed sub-steps
- [TODO] define a `WatchHistoryItem` model or equivalent
- [TODO] include enough fields for local rendering, likely:
  - media type
  - stable ids
  - title
  - year
  - poster/backdrop/artwork URL if available
  - subtitle/secondary label if useful
  - last watched timestamp
  - optional season/episode context for shows if needed for display
- [TODO] define sort behavior as most-recent-watch first
- [TODO] define duplicate behavior (same item watched again should refresh recency rather than duplicate)

### Validation
- model is sufficient to render history lists without immediate refetch
- repeat-watch behavior is explicit and deterministic

---

## A2. Add local watch history storage
Status: TODO
Priority: High

### Goal
Persist watch history locally in a simple, evolvable format.

### Why this matters
History should survive app restarts and feel instant to browse.
A file-based approach is probably sufficient unless scope expands.

### Proposed sub-steps
- [TODO] choose storage approach
- [TODO] support record/list/clear-or-trim operations as needed
- [TODO] preserve descending watched-time order
- [TODO] define safety behavior for invalid/corrupt storage
- [TODO] add focused tests for order/deduplication/update behavior

### Validation
- history persists across restarts
- repeated watch events refresh recency
- load failures fail safely

---

# Phase B — History write rules

## B1. Decide what counts as “watched” for history
Status: TODO
Priority: High

### Goal
Define when the app should write a history entry.

### Why this matters
If history writes too early, it becomes cluttered and misleading.
If it writes too late, users will think it is broken.

### Proposed sub-steps
- [TODO] decide whether history writes on playback start, successful playback preparation, resume past threshold, or playback exit after progress
- [TODO] decide whether source-resolution-only flows should count
- [TODO] define rules separately for movies vs episodes if needed
- [TODO] choose a pragmatic first-pass rule that matches current app seams

### Validation
- write rule is documented and implemented consistently
- normal playback flows create expected history entries

---

## B2. Record history from playback flow
Status: TODO
Priority: High

### Goal
Actually write history entries from the chosen playback seam.

### Proposed sub-steps
- [TODO] connect the chosen playback lifecycle event to history recording
- [TODO] store the right metadata for movies and shows
- [TODO] ensure repeat watches refresh recency
- [TODO] avoid duplicate spam from the same viewing session if possible

### Validation
- watching a movie creates/updates movie history
- watching a show creates/updates TV history
- repeated viewing moves the item to the top

---

# Phase C — History browse integration

## C1. Add Watch History entry points to Movies and TV menus
Status: TODO
Priority: High

### Goal
Expose watch history as a top-level browse option in Movies and TV flows.

### Proposed sub-steps
- [TODO] add Watch History entry point in Movies flow
- [TODO] add Watch History entry point in TV Shows flow
- [TODO] keep the entry aligned with the current favorites/browse interaction pattern

### Validation
- history entry is easy to discover in both Movies and TV areas
- navigation stays consistent

---

## C2. Render watch history lists sorted by most recent watch
Status: TODO
Priority: High

### Goal
Show movie/show watch history lists newest first.

### Proposed sub-steps
- [TODO] add history destination/view state for movie history
- [TODO] add history destination/view state for TV history
- [TODO] render local history items in descending watched-time order
- [TODO] handle empty-state UI cleanly

### Validation
- lists render correctly for movies and TV separately
- most recently watched item appears first
- empty state is clear and not broken-looking

---

# Phase D — Optional polish

## D1. Add long-press item actions in history lists
Status: OPTIONAL
Priority: Medium

### Notes
This could mirror the favorites pattern and provide actions like open details or remove history entry if desired.
Not required for the first pass unless requested.

## D2. Add per-item or global history clearing controls
Status: OPTIONAL
Priority: Medium

### Notes
Useful eventually, but not required for the initial browse/use case unless Mike wants explicit history management in v1.

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
Current recommendation:
Prefer a pragmatic playback-based rule tied to actual playback start/progress rather than merely opening details or resolving a source.

### Q3. Should movies and TV history be separate lists or one unified store filtered by entry point?
Current recommendation:
Use one unified local store with media type on each item, then filter by entry point.

### Q4. For TV shows, should history track show-level entries, episode-level entries, or both?
Current recommendation:
Likely show-level for the first browse pass unless episode-level context is easy to carry and materially improves usefulness.
Keep scope tight unless testing says otherwise.

---

## Risks / Watchouts

- writing history too early will create noisy junk entries
- writing history too late will make the feature feel unreliable
- storing too little metadata makes browse lists dependent on remote refetches
- storing too much playback detail risks accidental complexity creep
- if history and continue-watching overlap awkwardly, the product model may become confusing

---

## Progress Log

### 2026-04-04 19:53 UTC
- Created a dedicated watch history architecture/UI plan based on Mike’s request.
- Scoped the first pass around local persistence, playback-driven history recording, and movie/TV menu entry points with newest-first ordering.
- No implementation work completed under this plan yet.

---

## Scope Changes

### 2026-04-04
- New plan created specifically for watch history architecture + UI.
- Initial requested behavior is browse-focused: expose history under Movies and TV menus with descending recency.
- History write rules are treated as first-class scope because they determine whether the browse experience feels trustworthy.

---

## Session Start

### 2026-04-04 19:53 UTC
Intended task: create and scope the watch history plan before implementation.

---

## Definition of Done

This plan is complete for its intended pass when:
- accepted watch history items are marked `DONE`, `DEFERRED`, `OPTIONAL`, or removed
- validation is recorded for completed work
- follow-up work is explicitly captured
