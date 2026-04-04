# Asahi Execution Plan — Favorites Architecture and UI

Last updated: 2026-04-04 UTC
Status: COMPLETE
Owner: shield-tv-bot + Mike
Location: `docs/exec-plans/in_progress/2026-04-04-favorites-architecture-and-ui.md`
Supersedes: none

## Purpose

This plan is for adding favorites support for movies and TV shows.

The intended product behavior is:
- users can favorite search result entries for movies and TV shows
- favorites are stored locally in the app
- when users return to the Movies or TV Shows menus, they can open a Favorites view
- favorites are listed in descending order by when they were added (most recent first)

This plan covers both architecture and user-facing behavior.
The goal is not just “store some IDs,” but to add favorites in a way that fits the app’s current navigation and TV UI model cleanly.

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
- optionally add favorite toggle/state to details screen
- optionally improve discoverability for long-press item actions if testing shows users miss it

---

## Background / Review Summary

From the current app structure:
- navigation/state is coordinated through `AppCoordinator` / `AppState`
- search results render through `ScreenRenderers.kt`
- media search and browse UI is already TV-friendly and card-based
- there is already local persistence infrastructure for other app state
- the app does not currently appear to have a general-purpose local content library/history subsystem

That implies the right approach is:
- add a **small dedicated favorites subsystem**
- keep it local and explicit
- avoid prematurely inventing a giant media library model

Favorites should likely store enough summary data to render immediately without re-fetching remote metadata for every favorites view.

---

# Phase A — Favorites model and storage

## A1. Define favorites data model
Status: DONE
Priority: High

### Goal
Create a local favorites model that can render movie/TV favorites lists cleanly and deterministically.

### Why this matters
If favorites only store an ID, the favorites menus may depend too much on refetching remote metadata.
If they store too much, the model becomes a fake media database.

### Proposed sub-steps
- [DONE] define a `FavoriteItem` model or equivalent
- [DONE] include enough fields for local rendering, likely:
  - media type
  - stable ids (tmdb/imdb as available)
  - title
  - year
  - poster/backdrop/artwork URL if available
  - subtitle/secondary label if useful
  - added timestamp
- [DONE] define sort behavior as most-recent-added first
- [DONE] decide duplicate handling (same item favorited twice should refresh timestamp)

### Validation
- data model is sufficient to render favorites lists without immediate refetch
- duplicate behavior is explicit and tested

---

## A2. Add local favorites storage
Status: DONE
Priority: High

### Goal
Persist favorites locally in a simple, evolvable format.

### Why this matters
Favorites should survive app restarts and be fast to read.
This does not need a full database unless future scope grows.

### Proposed sub-steps
- [DONE] choose storage approach (JSON file)
- [DONE] support add/remove/list operations
- [DONE] preserve descending added-time order
- [DONE] define migration/versioning expectations if file-based
- [DONE] add focused tests for add/remove/order/deduplication behavior

### Validation
- favorites persist across restarts
- add/remove/order behavior is deterministic
- invalid storage fails safely

---

# Phase B — Search results integration

## B1. Add favorite/unfavorite affordance to search result entries
Status: DONE
Priority: High

### Goal
Let users favorite movies/TV shows directly from search results.

### Why this matters
That is the primary requested interaction path.
If favoriting requires going somewhere else first, the feature loses value.

### Proposed sub-steps
- [DONE] choose TV-friendly affordance for result cards:
  - dedicated favorite button on card
  - long-press/context action
  - secondary action row
- [DONE] show favorited state clearly
- [DONE] support toggling favorite state from results
- [DONE] ensure focus/navigation remains sensible on TV

### Validation
- favoriting from both movie and TV search results works
- favorite state is visually obvious
- no major focus regressions in results UI

---

## B2. Surface favorite state in details/search UI consistently
Status: DONE
Priority: Medium

### Goal
Avoid favorites feeling random or hidden after being added.

### Proposed sub-steps
- [DONE] show favorited state in result cards
- [OPTIONAL] show it in details screen if valuable
- [DONE] keep the visual treatment subtle but obvious

### Validation
- user can tell whether an item is already favorited

---

# Phase C — Favorites menu integration

## C1. Add Favorites entry points to Movies and TV menus
Status: DONE
Priority: High

### Goal
Expose favorites as a top-level browse option when users enter Movies or TV.

### Why this matters
This is the second half of the requested behavior: users should be able to come back later and browse favorites easily.

### Proposed sub-steps
- [DONE] add a Favorites entry point in Movies flow
- [DONE] add a Favorites entry point in TV Shows flow
- [DONE] ensure the entry fits naturally into existing menu/navigation patterns

### Validation
- favorites entry is easy to discover in both Movies and TV areas
- no navigation confusion with existing menu structure

---

## C2. Render favorites lists sorted by most recent add
Status: DONE
Priority: High

### Goal
Show favorites lists with newest favorites first.

### Why this matters
This is explicitly requested behavior and the most useful default ordering.

### Proposed sub-steps
- [DONE] add favorites destination/view state for movie favorites
- [DONE] add favorites destination/view state for TV favorites
- [DONE] render favorite items in descending `addedAt` order
- [DONE] handle empty-state UI cleanly

### Validation
- lists render correctly for movies and TV separately
- most recently favorited item appears first
- empty state is clear and not broken-looking

---

# Phase D — Optional polish

## D1. Add favorite toggle to details screen
Status: OPTIONAL
Priority: Medium

### Notes
This may be worthwhile, but it is not required to satisfy the current requested behavior.
The core requested flow is favoriting from search results and browsing from movie/TV favorites menus.

## D2. Add remove-from-favorites action inside favorites lists
Status: DONE
Priority: Medium

### Notes
Implemented via long-press item actions so favorites management works from both search results and favorites lists without cluttering the default card UI.

---

## Recommended Order

1. A1 Define favorites data model
2. A2 Add local favorites storage
3. B1 Add favorite/unfavorite affordance to search results
4. C1 Add Favorites entry points to Movies and TV menus
5. C2 Render favorites lists sorted by most recent add
6. B2 Improve favorite-state consistency in UI
7. D2 Add long-press item actions and remove-from-favorites management
8. Optional D1 if still justified

---

## Open Questions / Decisions Needed

### Q1. Should favorites store only identifiers, or enough display metadata to render locally?
Current recommendation:
Store enough metadata to render favorite rows/cards locally without an immediate network round-trip.

### Q2. Should favoriting the same item again refresh its `addedAt` and move it to the top?
Current recommendation:
Yes. Treat repeat favorite as “refresh recency” unless that feels surprising in testing.

### Q3. Should movie favorites and TV favorites be separate lists or one mixed list filtered by entry point?
Current recommendation:
Store in one unified favorites store with media type on each item, then filter by entry point.
That keeps storage simpler while matching the requested Movies-vs-TV browse flows.

### Q4. What is the right TV-friendly affordance for toggling favorites on search results?
Current recommendation:
Use click-to-open as the primary default action, and support long-press to open an item actions modal with favorite/unfavorite and other contextual actions.
Avoid making long-press the only way to favorite, but use it as the clean management surface.

---

## Risks / Watchouts

- storing too little metadata makes favorites lists dependent on remote lookups
- storing too much metadata risks accidental library-system creep
- adding favorite toggles to search cards can harm focus/navigation if done carelessly
- if the favorite affordance is visually weak, users may miss the feature entirely
- if favorites menus are awkwardly inserted into Movies/TV flows, the feature will feel bolted on

---

## Progress Log

### 2026-04-04 18:57 UTC
- Created a dedicated favorites architecture/UI plan based on the requested feature behavior.
- Scoped the plan around local persistence, search result favoriting, and favorites entry points in Movies/TV menus.
- No implementation work completed under this plan yet.

### 2026-04-04 19:05 UTC
- Completed A1 and the storage seam for A2 by introducing `FavoriteItem`, JSON encoding/decoding, and a local `FavoritesStore`.
- Favorites now have explicit most-recent-first ordering and duplicate refresh behavior via stable keys.
- Added focused storage tests covering add/remove/order/deduplication/favorited checks.
- Validation: `./gradlew testDebugUnitTest` passed.

### 2026-04-04 19:42 UTC
- Completed the browse-side favorites flow by wiring home-screen Movies and TV entry points to open favorites lists directly.
- Reused the existing results screen for the first favorites browsing experience, with dedicated favorites titles/captions and empty-state messaging.
- Added app state for favorites browse mode so the renderer can distinguish favorites from ordinary search results.
- Added `FavoritesCoordinator.listByType(...)` returning local `SearchResult` rows sorted newest first from the unified favorites store.
- Validation: `./gradlew testDebugUnitTest assembleDebug` passed.

### 2026-04-04 19:45 UTC
- New product direction: add long-press item actions for results/favorites cards.
- Goal is to keep click-to-open fast while using a modal for contextual actions such as favorite/unfavorite.
- This will also provide the cleanest place to support direct remove-from-favorites inside favorites lists.

### 2026-04-04 19:51 UTC
- Implemented long-press item actions on results/favorites cards.
- Results cards now support Android long-press and MENU-key actions that open a modal with contextual actions.
- Added favorite/unfavorite management to the modal, including direct removal while browsing favorites.
- Favorites browse lists now refresh immediately after a modal-driven toggle/remove so the current screen stays accurate.
- Validation: `./gradlew testDebugUnitTest assembleDebug` passed.

---

## Scope Changes

### 2026-04-04
- New plan created specifically for favorites architecture + UI.
- The requested behavior is centered on favoriting search results and browsing favorites from Movies/TV menus, so that is the core scope.
- Started implementation from the data/storage side first to avoid building UI behavior on a shaky persistence model.
- Reused the existing results screen for favorites browsing instead of adding a separate favorites renderer in this pass.
- Added long-press item actions to result cards as the preferred contextual-management surface for TV.

---

## Session Start

### 2026-04-04 19:52 UTC
Intended task: close the favorites plan after implementation and validation are complete.

---

## Definition of Done

This plan is complete for its intended pass when:
- accepted favorites items are marked `DONE`, `DEFERRED`, `OPTIONAL`, or removed
- validation is recorded for completed work
- follow-up work is explicitly captured

Result: complete for this pass. Remaining items are optional polish only.
