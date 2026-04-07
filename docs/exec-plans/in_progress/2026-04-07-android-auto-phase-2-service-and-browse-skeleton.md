# Exec Plan — Android Auto Phase 2 Service and Browse Skeleton

Last updated: 2026-04-07 UTC
Status: IN_PROGRESS
Owner: shield-tv-bot + Mike
Location: `docs/exec-plans/in_progress/2026-04-07-android-auto-phase-2-service-and-browse-skeleton.md`
Supersedes: `docs/exec-plans/in_progress/2026-04-07-android-auto-phase-1-implementation.md`
References: `docs/exec-plans/in_progress/2026-04-07-android-auto-decision-doc.md`

## Purpose

Define and drive the second Android Auto implementation phase for Asahi by wiring the first service/session shell, browse skeleton, and playback handoff seams on top of the Phase 1 Auto boundary.

This phase exists to make the Auto architecture real enough to iterate on without yet trying to ship a complete, polished car experience.

It should prove that:
- the app can expose an Auto-facing media service boundary
- browse roots can be projected from existing app data without touching TV UI state
- Auto playback requests can route through the dedicated Auto abstractions rather than bypassing them

This phase should still stop short of full feature completeness.

---

## How to Use This Plan

### Before starting a session
1. Read this file top-to-bottom.
2. Check `Current Focus`.
3. Check `Open Questions / Decisions Needed`.
4. Check the latest `Progress Log` entry.
5. Update `Session Start` before coding.

### While implementing
Update this file during the work, not only after the work.

Minimum required updates:
- task status changes
- progress log entries after meaningful milestones
- scope-change notes when direction shifts
- validation notes after completed steps
- exact commit hashes for meaningful landed slices

### Completion rule
A task is only `DONE` when:
- the code landed
- the milestone commit(s) are named when practical
- relevant tests/build/manual validation happened
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

**Current phase:** Phase 2 — service shell and browse skeleton

**Immediate target:** complete the minimal Android Auto media service/session shell plus the root browse projection, then move next to playback facade wiring

**Why this now:**
Phase 1 established the contracts and pure rule logic. The next honest step is to exercise those contracts through a real Auto entry surface so future implementation can build on something runnable instead of abstract interfaces alone.

---

## Repository Reality Check

Before implementation begins, confirm:
- Phase 1 boundary now exists under `app/src/main/kotlin/ai/shieldtv/app/auto/`
- `DefaultAutoSourceSelector` and `DefaultAutoShowProgressResolver` already landed and are unit-tested
- app currently has favorites, watch-history, continue-watching, metadata, and playback coordinators that can feed an Auto projection layer
- there is not yet an Android Auto-specific service/session implementation in the app module
- unrelated local edits currently exist in `app/src/main/kotlin/ai/shieldtv/app/MainActivity.kt`, `app/src/main/kotlin/ai/shieldtv/app/ui/ScreenRenderers.kt`, and untracked `tv-screenshot.png`; Phase 2 should avoid entangling with them

---

## Locked Decisions

- Asahi Auto is a playback-first companion surface, not a full browse/source-management clone of the TV app.
- Auto source policy remains stricter than TV policy: cached first, then direct, else fail.
- Auto show selection remains deterministic: resume, then next unwatched, then latest, then `S01E01` fallback.
- Auto code stays inside `app/.../auto` for now, but must remain cleanly separable from TV-specific classes.
- This phase should not introduce manual source picking, setup flows, or deep episode browsing trees.

---

## Background / Review Summary

Phase 1 created the core internal API for Auto work:
- framework-neutral Auto browse/playback models
- `AutoBrowseRepository` and `AutoPlaybackFacade` interfaces
- `DefaultAutoSourceSelector`
- `DefaultAutoShowProgressResolver`

That was the right stop point because it prevented direct leakage of TV state into Auto rules.

Phase 2 now needs to turn those internal seams into a minimal runnable shell by:
- exposing an Auto-capable media service/session boundary
- creating root browse nodes that represent the MVP product definition
- mapping existing stores into Auto browse data without touching TV renderer logic
- creating placeholder but honest playback handoff plumbing through Auto-specific interfaces

The purpose is not to finish the whole Android Auto app. The purpose is to de-risk service integration and browse architecture while keeping playback rules centralized.

---

# Phase A — Service and manifest skeleton

## A1. Add minimal Auto media service shell
Status: DONE
Priority: High

### Goal
Create the initial Android Auto media service/session entry point in the app module so the Auto surface has a concrete runtime boundary.

### Why this matters
Until a real service shell exists, Auto work remains theoretical. This task makes the architecture executable and reveals lifecycle / dependency issues early.

### Proposed sub-steps
- [TODO] confirm whether the project already uses Media3 playback/session components that can anchor the Auto service
- [TODO] add an `auto/service/` package with a minimal service implementation and narrow session callback surface
- [TODO] keep initial behavior intentionally small: root exposure, simple item lookup hooks, and playback handoff seams only
- [TODO] avoid binding the service to TV `MainActivity` state or UI renderers

### Validation
- project compiles with the service added
- manifest wiring builds successfully in debug
- if practical, unit/instrumentation-free sanity checks verify service creation paths compile cleanly

---

## A2. Add required manifest and metadata wiring
Status: DONE
Priority: High

### Goal
Register the Auto media service and any required intent/filter/metadata entries needed for the next implementation slices.

### Why this matters
Without manifest registration, the new service shell is dead code and future DHU/Auto discovery work cannot begin.

### Proposed sub-steps
- [TODO] inspect current manifest structure and service declarations
- [TODO] add the minimal Auto service declaration and required media-browser/media-session intent filters
- [TODO] add any placeholder automotive metadata/resources required for discovery, but avoid premature branding/polish work
- [TODO] keep comments/TODOs explicit where Phase 3 or later will expand behavior

### Validation
- `assembleDebug` passes
- manifest merger output is clean enough to confirm the new service is registered

---

# Phase B — Browse projection skeleton

## B1. Implement a root-level `AutoBrowseRepository` projection
Status: DONE
Priority: High

### Goal
Add a first repository implementation that can expose root browse nodes and small list projections from existing app data sources.

### Why this matters
The Auto UX depends on a browse tree that is independent from TV screens. This repository is the seam that keeps Auto read-optimized and UI-neutral.

### Proposed sub-steps
- [TODO] create a concrete repository implementation under `auto/browse/`
- [TODO] implement `root()` with MVP-safe top-level nodes such as Continue Watching, Favorites, Recent, Movies, TV Shows, and Search if those map cleanly to the current app state
- [TODO] decide which collections are immediately backed by real data versus placeholder empty lists with TODOs
- [TODO] reuse favorites/history/continue-watching stores or coordinators where possible without depending on screen state
- [TODO] keep node ids stable and future-compatible with child loading

### Validation
- root mapping covered by unit tests if practical
- compile/build passes with repository implementation in place
- any placeholder branches are documented honestly in code and this plan

---

## B2. Add browse-node mapping helpers
Status: DONE
Priority: Medium

### Goal
Create lightweight mapping helpers that convert existing search/history/favorites/continue-watching models into `AutoBrowseNode` consistently.

### Why this matters
This reduces duplicated mapping logic and keeps browse semantics stable as more child collections get implemented.

### Proposed sub-steps
- [TODO] inspect existing favorites/history/continue-watching item shapes and identify common fields
- [TODO] add a small factory/mapper for node construction
- [TODO] encode sensible action hints for resume/play/open-collection/search cases
- [TODO] keep the mapper free of Android UI dependencies

### Validation
- mapper behavior covered by unit tests if it contains nontrivial branching
- no TV-only imports in Auto browse code

---

# Phase C — Playback handoff skeleton

## C1. Add a thin `AutoPlaybackFacade` implementation
Status: TODO
Priority: High

### Goal
Implement the first concrete Auto playback facade that routes movie/show requests through Auto-specific rules and existing app/domain services.

### Why this matters
The facade is the main guardrail preventing the Auto integration from bypassing source-selection and deterministic show-default rules.

### Proposed sub-steps
- [TODO] inspect current playback and source-loading coordinators to find the narrowest reusable handoff path
- [TODO] implement movie playback path using `DefaultAutoSourceSelector`
- [TODO] implement show default playback path using `DefaultAutoShowProgressResolver`
- [TODO] return concise `AutoPlaybackResult` values instead of TV-oriented UI outcomes
- [TODO] keep setup/auth failure states short and car-safe

### Validation
- unit tests for facade behavior in at least the happy path and clean-failure path cases
- `testDebugUnitTest` passes

---

## C2. Add lookup/identifier strategy for Auto playable nodes
Status: TODO
Priority: Medium

### Goal
Define how browse nodes map back to media references and playback actions so the service layer can resolve user selections deterministically.

### Why this matters
The service shell will need stable ids for browse callbacks and playback dispatch. Doing this sloppily now will create migration pain later.

### Proposed sub-steps
- [TODO] choose stable id conventions for collections, media items, and show-default actions
- [TODO] decide whether ids should encode media type + external ids + optional episode target context
- [TODO] add parser/helper utilities if needed, but keep the scheme compact and readable

### Validation
- id encode/decode logic unit-tested if introduced
- ids remain stable across repository/service/facade boundaries

---

# Phase D — Service-to-repository wiring

## D1. Wire service callbacks to root browse and playback seams
Status: TODO
Priority: High

### Goal
Connect the new service shell to the browse repository and playback facade so the app has an end-to-end skeleton path for root browse and basic playback requests.

### Why this matters
This is the first end-to-end proof that the architecture works in practice rather than just in isolated classes.

### Proposed sub-steps
- [TODO] wire service root request handling to `AutoBrowseRepository.root()`
- [TODO] wire item selection or play-from-id hooks to `AutoPlaybackFacade`
- [TODO] keep unsupported branches explicit and concise rather than faking full support
- [TODO] add inline TODOs where child browse trees, search, or richer metadata will land in Phase 3+

### Validation
- `./gradlew testDebugUnitTest assembleDebug`
- if practical, manual install/DHU sanity later, but not required to call this phase complete unless the wiring reaches a runnable state worth checking

---

# Optional Work

## O1. Add an internal package note / README-style comment under `auto/`
Status: OPTIONAL
Priority: Low

### Notes
Useful only if the service/browse wiring gets complex enough that a short architecture note would reduce drift. Do not let this delay actual code.

---

## Recommended Order

1. Add service shell and manifest wiring
2. Implement root browse repository projection
3. Implement thin playback facade
4. Define stable id strategy and wire service callbacks to repository/facade
5. Add targeted tests and validate build

---

## Open Questions / Decisions Needed

### Q1. Should Phase 2 target full Media3 `MediaLibraryService` now, or start with the smallest possible service shell that can graduate cleanly?
Current recommendation:
Start with the smallest real `MediaLibraryService`/session shell that matches Android Auto’s actual shape, rather than a fake abstraction. Keep behavior tiny, but use the right platform boundary now.

### Q2. Which root nodes should be real in this phase versus placeholders?
Current recommendation:
Resolved for this slice: `Continue Watching`, `Favorites`, and `Recent` are real store-backed projections. `Movies`, `TV Shows`, and `Search` are explicit placeholders/stubs and should only become real when child browse/search execution is implemented.

### Q3. Should Phase 2 include real search handling already?
Current recommendation:
Only if the existing search pipeline plugs in cheaply. Otherwise, expose the root/search affordance but defer meaningful search execution to the next phase.

---

## Risks / Watchouts

- Media3/Android Auto service wiring may reveal dependency assumptions that currently live too close to `MainActivity`.
- Browse ids can become ad hoc quickly if not standardized before service callback wiring.
- It is easy to accidentally smuggle TV-specific messaging, state, or source-picker assumptions into the Auto playback facade.
- Search can sprawl fast; keep it constrained unless it comes together almost for free.
- Current service shell owns its own `ExoPlayer` only to satisfy the Media3 session boundary; real playback handoff still needs Phase C/D work so Auto requests route through app playback rules instead of pretending playback is complete.
- Unrelated local edits in the repo increase the chance of accidental overlap; keep commits tightly scoped.

---

## Validation Notes / Honesty Check

### Phase 2 initial planning state
- Validated by: Phase 1 landed with passing `./gradlew testDebugUnitTest assembleDebug` and created the required Auto contracts.
- Not validated: service lifecycle, manifest discovery, browse id scheme, and end-to-end playback handoff for Auto.
- Known uncertainty: exact Media3/manifest shape still needs repo inspection before coding.

---

## Progress Log

### 2026-04-07 18:17 UTC
- Created the Phase 2 execution plan.
- Established scope around service shell, root browse projection, playback facade wiring, and stable id handling.
- No Phase 2 implementation work completed yet.

### 2026-04-07 18:45 UTC
- Added the first concrete Android Auto service shell as `AsahiAutoService` using Media3 `MediaLibraryService` and a minimal `MediaLibrarySession` callback.
- Added manifest wiring plus `res/xml/automotive_app_desc.xml` so the app now declares an Auto media surface.
- Added a concrete `DefaultAutoBrowseRepository`, `AutoBrowseNodeMapper`, and stable `AutoMediaId` helpers.
- Root collections now expose `Continue Watching`, `Favorites`, `Recent`, `Movies`, `TV Shows`, and `Search`.
- Real projections are implemented for continue watching, favorites, and recent via existing stores; movies / TV shows / search remain explicit stubs for later phases.
- Validation: `./gradlew :app:testDebugUnitTest :app:assembleDebug` passed after landing the service and browse skeleton.

---

## Scope Changes

### 2026-04-07
- Initial Phase 2 scope established.
- Future hooks to preserve: child browse expansion, real search execution, metadata/session polish, and DHU/manual Auto validation.

---

## Session Start

### 2026-04-07 18:17 UTC
Intended task: create the Phase 2 execution plan and use it as the sole active implementation driver for the next Android Auto pass

### 2026-04-07 18:29 UTC
Intended task: land Phase 2 service shell + manifest wiring + root browse repository and stable media-id skeleton before moving on to playback facade work

---

## Definition of Done

This plan is complete for its intended pass when:
- a minimal Auto service/session shell exists and is manifest-registered
- root browse wiring exists through `AutoBrowseRepository`
- a thin concrete `AutoPlaybackFacade` exists for core playback paths
- service callbacks are wired to browse and playback seams without TV UI coupling
- required tests/build validation are recorded
- deeper search/browse/polish work is explicitly deferred to the next phase instead of being half-implemented
