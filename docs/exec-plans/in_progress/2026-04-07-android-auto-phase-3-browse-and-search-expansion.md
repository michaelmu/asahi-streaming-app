# Exec Plan — Android Auto Phase 3 Browse and Search Expansion

Last updated: 2026-04-07 UTC
Status: IN_PROGRESS
Owner: shield-tv-bot + Mike
Location: `docs/exec-plans/in_progress/2026-04-07-android-auto-phase-3-browse-and-search-expansion.md`
Supersedes: `docs/exec-plans/in_progress/2026-04-07-android-auto-phase-2-service-and-browse-skeleton.md`
References:
- `docs/exec-plans/in_progress/2026-04-07-android-auto-decision-doc.md`
- `docs/exec-plans/in_progress/2026-04-07-android-auto-phase-2-service-and-browse-skeleton.md`

## Purpose

Drive the third Android Auto implementation phase for Asahi by turning the current service/browse/playback skeleton into a genuinely navigable Auto companion surface.

This phase exists to complete the first useful browse/search experience while preserving the MVP product boundaries already decided:
- playback-first, not TV-parity
- cached/direct-only playback policy
- deterministic show continuation behavior
- no in-car setup or source-picking flows

Phase 3 should make the Auto surface feel real enough that DHU/emulator validation is worth doing, without prematurely chasing polish or parked-mode complexity.

It should prove that:
- root browse nodes lead to meaningful child content instead of placeholders
- Auto search can return actionable, car-safe items
- service callbacks can support browse and play flows end-to-end through the Auto abstractions
- error/empty states are concise and explicit instead of silently failing

---

## How to Use This Plan

### Before starting a session
1. Read this file top-to-bottom.
2. Check `Current Focus`.
3. Check `Locked Decisions`.
4. Review the latest `Progress Log` entry.
5. Update `Session Start` before coding.

### While implementing
Update this file during the work, not only after the work.

Minimum required updates:
- task status changes
- progress log entries after meaningful milestones
- scope-change notes when direction shifts
- validation notes after completed steps
- exact commit hashes for meaningful landed slices when practical

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

**Current phase:** Phase 3 — browse and search expansion

**Immediate target:** replace the Phase 2 placeholder branches (`Movies`, `TV Shows`, `Search`) with real browse/search behavior and tighten end-to-end Auto service behavior around selection, empty states, and failure reporting

**Why this now:**
Phase 2 proved the architecture is real: service, stable IDs, browse projection seams, and playback facade now exist. The next honest step is to make the Auto surface actually navigable and searchable rather than technically present but mostly skeletal.

---

## Repository Reality Check

Before implementation begins, confirm:
- Phase 2 landed a Media3 `MediaLibraryService` shell in `app/.../auto/service/AsahiAutoService.kt`
- stable `AutoMediaId` item/collection identifiers exist and are already used by browse/playback paths
- `DefaultAutoBrowseRepository` currently backs `Continue Watching`, `Favorites`, and `Recent`
- `Movies`, `TV Shows`, and `Search` root nodes still resolve to explicit placeholder/stub behavior
- `DefaultAutoPlaybackFacade` now routes playable Auto actions through source search + Auto playback rules, but final player/session integration remains intentionally shallow
- unrelated local edits may still exist elsewhere in the repo; Phase 3 should remain tightly scoped to Auto files unless an extraction is clearly necessary

---

## Locked Decisions

- Asahi Auto remains a playback-first companion surface, not a full TV clone.
- Auto source policy stays stricter than TV: cached first, then direct, else fail.
- Show default behavior remains deterministic: resume, next unwatched, latest, then `S01E01` fallback.
- Auto should not expose manual source selection or in-car auth/setup.
- TV-specific UI renderers and `MainActivity` state should stay out of browse/search/service logic.
- Search results must resolve to simple car-safe playable actions rather than deep branching flows.
- Phase 3 may add child browse nodes, but should still avoid long season/episode exploration trees unless they fall out naturally and safely.

---

## Background / Review Summary

Phase 2 delivered the first real Auto runtime boundary:
- `AsahiAutoService`
- manifest + automotive metadata wiring
- `AutoMediaId`
- `DefaultAutoBrowseRepository`
- `DefaultAutoPlaybackFacade`
- targeted unit coverage for playback selection behavior

That was the right stop point because it made Auto real without overcommitting to browse complexity.

Phase 3 now needs to move the experience from “architecture skeleton” to “usable MVP browse/search surface” by:
- backing `Movies` and `TV Shows` with lightweight real projections
- wiring real search execution into the service callback layer
- ensuring browse/search items resolve into stable, playback-oriented actions
- making empty/failure states explicit enough that DHU/manual Auto testing is meaningful

The purpose is not to ship the final Android Auto app. The purpose is to produce the first honest MVP-shaped surface that users could actually navigate.

---

# Phase A — Real child browse branches

## A1. Make `Movies` root child content real
Status: TODO
Priority: High

### Goal
Turn the current `Movies` placeholder branch into a lightweight, useful child list sourced from existing app data or cheap metadata/search-backed projections.

### Why this matters
Right now the root exposes a movie entry point that does not yet lead anywhere meaningful. That undercuts the Phase 2 service/browse work.

### Proposed sub-steps
- [TODO] inspect what existing app data can back a car-safe movie branch cheaply (favorites, recent, continue watching, trending-like fallback only if cheap)
- [TODO] decide whether `Movies` should act as a filtered collection hub or a flat playable list for MVP
- [TODO] keep item count constrained and titles concise for Auto readability
- [TODO] ensure returned nodes resolve into stable movie play actions via `AutoMediaId`

### Validation
- unit test repository mapping if nontrivial
- `Movies` branch returns real children in service callbacks
- build/tests pass

---

## A2. Make `TV Shows` root child content real
Status: TODO
Priority: High

### Goal
Turn the `TV Shows` placeholder branch into a lightweight show list that supports deterministic default playback behavior without deep season trees.

### Why this matters
The product direction explicitly supports TV in MVP, but only in constrained action-oriented form. The browse tree should reflect that.

### Proposed sub-steps
- [TODO] back `TV Shows` from existing favorites/history/continue-watching projections or other cheap show-centric sources
- [TODO] decide whether nodes should be directly playable show-default actions, browsable mini-hubs, or a constrained mix
- [TODO] avoid deep episode trees unless they are very cheap and clearly helpful
- [TODO] ensure show nodes stay aligned with Auto’s deterministic continuation rules

### Validation
- real `TV Shows` child content visible through repository/service paths
- unit tests if branching behavior grows beyond trivial mapping
- build/tests pass

---

## A3. Define collection child-loading conventions
Status: TODO
Priority: Medium

### Goal
Standardize how collection nodes map to child loaders so the repository/service boundary does not become a pile of special cases.

### Why this matters
Phase 2 made root-level browse work. Phase 3 will multiply child-loading branches quickly if conventions are not made explicit now.

### Proposed sub-steps
- [TODO] decide whether `AutoBrowseRepository` should gain explicit child-loading APIs or whether current branch-specific methods should expand in a controlled way
- [TODO] document stable collection id conventions in code and this plan
- [TODO] keep the scheme compact and future-compatible with Phase 4 hardening/polish

### Validation
- service child loading becomes simpler, not more ad hoc
- ids remain stable across repository/service/playback boundaries

---

# Phase B — Real Auto search

## B1. Implement Auto search execution in repository/service flow
Status: TODO
Priority: High

### Goal
Add meaningful Auto search so the `Search` affordance returns actionable movie/show results instead of a stub.

### Why this matters
Search is a core MVP capability in the decision doc. Without it, the Auto surface remains noticeably incomplete.

### Proposed sub-steps
- [TODO] inspect the existing app search pipeline and identify the narrowest Auto-safe integration path
- [TODO] wire `MediaLibraryService` search callbacks to `AutoBrowseRepository.search()` or equivalent
- [TODO] keep the first pass simple: concise titles, lightweight subtitles, stable artwork when available, immediate play-oriented actions
- [TODO] ensure movie results map to `PLAY_MOVIE` and show results map to `PLAY_SHOW_DEFAULT`
- [TODO] keep query handling/failure states short and explicit

### Validation
- search callback returns results in debug/service flow
- unit tests for search-result mapping if practical
- build/tests pass

---

## B2. Add search-result mapping helpers and constraints
Status: TODO
Priority: Medium

### Goal
Create dedicated mapping rules for Auto search results so search semantics stay distinct from favorites/history/continue-watching projections.

### Why this matters
Search result items carry different expectations than library/history items. Conflating them will create drift and awkward subtitles/actions.

### Proposed sub-steps
- [TODO] inspect existing `SearchResult` model and choose best available artwork/subtitle fields for Auto
- [TODO] keep result metadata terse and readable in the car
- [TODO] cap result count to something realistic for Auto
- [TODO] explicitly handle empty search results with honest empty-state behavior

### Validation
- search mapping remains UI-neutral and free of TV-only imports
- service search returns stable ids and sensible action hints

---

# Phase C — Service behavior tightening

## C1. Improve service browse/play callback coverage
Status: TODO
Priority: High

### Goal
Tighten `AsahiAutoService` so it handles child browse branches, search callbacks, and item lookup more completely and less as a skeleton.

### Why this matters
Phase 2 proved the service boundary. Phase 3 needs to make that boundary robust enough for actual Auto interaction.

### Proposed sub-steps
- [TODO] add real child browse handling for new `Movies` / `TV Shows` branches
- [TODO] implement search-specific callbacks supported by the chosen Media3 API surface
- [TODO] make item lookup less placeholder-oriented where practical
- [TODO] keep unsupported branches explicit rather than pretending broad support exists

### Validation
- `./gradlew :app:testDebugUnitTest :app:assembleDebug`
- service code remains understandable and not overly entangled

---

## C2. Add concise blocked/failed/empty-state handling in service responses
Status: TODO
Priority: Medium

### Goal
Make Auto failures and empty collections observable and honest instead of silently returning confusing no-op behavior.

### Why this matters
Car surfaces should fail clearly and briefly. Silent empties or unsupported-path drops make debugging and user experience much worse.

### Proposed sub-steps
- [TODO] decide where to surface blocked/failure outcomes in the Media3/Auto boundary without inventing fake UX
- [TODO] add explicit empty-state items or equivalent where appropriate
- [TODO] keep wording short, non-technical, and car-safe
- [TODO] document any Media3 limitations discovered during implementation

### Validation
- blocked/failure cases are testable or at least code-auditable
- empty branches/searches are not ambiguous in behavior

---

# Phase D — Manual validation readiness

## D1. Prepare the surface for DHU/emulator/manual Android Auto checks
Status: TODO
Priority: Medium

### Goal
Get the Auto MVP far enough that actual Android Auto runtime validation is worth doing and easy to interpret.

### Why this matters
At this point, architecture-only confidence starts to taper off. Manual validation should become a worthwhile next step.

### Proposed sub-steps
- [TODO] add small diagnostics/logging where they materially improve service/browse debugging
- [TODO] confirm root + child browse + search all behave coherently in code before manual checks
- [TODO] define the minimum manual checklist for the first DHU/emulator pass
- [TODO] defer broad polish until after real runtime observations

### Validation
- debug build installs/runs
- plan contains a short manual checklist once the surface is ready

---

# Optional Work

## O1. Add a small Auto package note documenting browse/search architecture
Status: OPTIONAL
Priority: Low

### Notes
Only do this if Phase 3 introduces enough branching that a short architecture note would reduce drift. Do not let documentation outrun the code.

---

## Recommended Order

1. Define child-loading conventions
2. Make `Movies` and `TV Shows` branches real
3. Implement real Auto search
4. Tighten service browse/search/item callback behavior
5. Improve failure/empty-state handling
6. Prepare for DHU/emulator/manual validation

---

## Open Questions / Decisions Needed

### Q1. Should `Movies` and `TV Shows` be flat playable lists or mini collection hubs in MVP?
Current recommendation:
Prefer the simplest thing that feels real: likely flat or near-flat lists sourced from existing signals, unless a two-level structure clearly improves clarity without deepening the browse tree too much.

### Q2. Should show search results be directly playable or browsable?
Current recommendation:
Directly playable via `PLAY_SHOW_DEFAULT` for MVP. Avoid turning search into a gateway to deeper episode browsing unless a specific use case demands it.

### Q3. Should empty collections be represented as explicit message items or as empty lists?
Current recommendation:
Use explicit empty-state representation only where Media3/Auto behavior would otherwise feel broken or ambiguous. Keep the wording terse.

### Q4. Is it time to deepen playback integration into the shared player/session path?
Current recommendation:
Not yet unless Phase 3 service/runtime validation reveals clear limitations. Keep the current facade-driven approach until browse/search behavior is proven.

---

## Risks / Watchouts

- Search can sprawl fast and drag in TV-oriented assumptions if not kept constrained.
- `Movies` / `TV Shows` branches can become accidental mini-TV UIs if hierarchy grows unchecked.
- Media3 search and error signaling details may not map perfectly to the desired product behavior; keep implementation honest rather than overfitted.
- Richer browse branches can tempt coupling to existing TV coordinators/screens rather than staying projection-based.
- The current playback handoff is intentionally shallow; do not accidentally represent it as final architecture if deeper player integration is still likely later.
- Unrelated local repo edits still increase overlap risk; keep commits tight.

---

## Validation Notes / Honesty Check

### Phase 3 initial planning state
- Already validated: root browse wiring, stable Auto ids, minimal service shell, manifest wiring, and thin playback facade all compile and pass targeted unit tests/build validation.
- Not yet validated: real child browse collections for `Movies` / `TV Shows`, search callback behavior, end-to-end search→play flow, and DHU/manual runtime behavior.
- Known uncertainty: the best Auto-safe shape for `Movies` / `TV Shows` branches may become clearer only after the first manual Auto run.

---

## Progress Log

### 2026-04-07 18:54 UTC
- Created the Phase 3 execution plan.
- Scoped this phase around real child browse collections, search execution, service callback tightening, and manual-validation readiness.
- No Phase 3 implementation work completed yet.

---

## Scope Changes

### 2026-04-07
- Initial Phase 3 scope established.
- Preserved future hooks for deeper player/session integration, browse/search polish, and DHU-driven fixes after the first manual Auto pass.

---

## Session Start

### 2026-04-07 18:54 UTC
Intended task: create the Phase 3 execution plan as the next active driver after the Phase 2 service/browse/playback skeleton landed

---

## Definition of Done

This plan is complete for its intended pass when:
- `Movies` and `TV Shows` are backed by real child content rather than placeholders
- Auto search returns real actionable results
- service callbacks support browse/search/play flows in a coherent end-to-end way
- empty/blocked/failure behaviors are explicit enough for real runtime testing
- required tests/build validation are recorded
- the resulting surface is ready for meaningful DHU/emulator/manual Android Auto validation
