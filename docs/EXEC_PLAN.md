# Asahi Execution Plan

Last updated: 2026-04-04 UTC
Status: Active living document
Owner: shield-tv-bot + Mike

## Purpose

This is the working execution plan for the next major Asahi cleanup/improvement pass.

It is intentionally operational, not aspirational.
The goal is to make implementation reliable, resumable, and auditable while changes are actively happening.

Use this doc to:
- track what we intend to change
- track what is in progress right now
- record decisions and scope changes as they happen
- avoid losing context across sessions
- keep implementation focused on shipping meaningful improvements instead of scattered cleanup

---

## How to Use This Plan

### Before starting a work session

1. Read this file top-to-bottom.
2. Check `Current Focus`.
3. Check `Open Questions / Decisions Needed`.
4. Check `Progress Log` for the last completed or interrupted step.
5. Update `Session Start` with the current date/time and intended task.

### While making changes

Update this file during the work, not only after the work.

Specifically:
- mark items as `IN_PROGRESS` when started
- mark items as `DONE` when actually complete
- mark items as `BLOCKED` if stuck
- add brief notes when scope changes
- record surprises, regressions, or follow-up work

### After each meaningful implementation step

Add an entry to `Progress Log` with:
- date/time
- what changed
- what was validated
- what remains
- any new risks discovered

### If the plan changes

Do not silently drift.
Update:
- `Scope Changes`
- `Current Focus`
- relevant phase/task status
- `Open Questions / Decisions Needed`

### Completion rule

A task is only `DONE` when:
- code changes are in place
- relevant tests pass or validation was explicitly recorded
- any follow-up work is captured in this doc

---

## Status Legend

- `TODO` = not started
- `IN_PROGRESS` = actively being worked
- `BLOCKED` = cannot proceed without decision/fix/input
- `DONE` = implemented and validated
- `DEFERRED` = intentionally postponed

---

## Current Focus

**Current phase:** Phase 1 — Runtime architecture cleanup and source pipeline reliability

**Immediate target:** P1.1 first extraction pass — move source loading coordination out of `MainActivity` into a dedicated component without changing user-facing behavior.

**Why this first:**
The codebase already has good module-level direction, but app-level orchestration and source-loading flow are the biggest near-term maintainability risks.
Fixing those first reduces future breakage and makes later product work faster.

---

## Implementation Principles

1. **Prefer structural improvements that reduce future churn.**
   Do not paper over core coordination problems with more conditionals in `MainActivity`.

2. **Keep vertical functionality working while refactoring.**
   Search → details → sources → playback must remain usable.

3. **Favor incremental, testable refactors over one-shot rewrites.**
   Move one flow at a time behind clearer interfaces.

4. **Promote product policy out of UI glue where practical.**
   Auth requirements, provider eligibility, and ranking/filter behavior should not live primarily in activity code.

5. **Keep the plan current.**
   This document is part of the implementation, not paperwork after the fact.

---

## Phase Overview

### Phase 1 — Runtime architecture cleanup and source pipeline reliability
Goal: Reduce coordination sprawl and improve source loading behavior.

### Phase 2 — Ranking, dedupe, and resolver quality
Goal: Turn source quality logic into something more explainable, testable, and resilient.

### Phase 3 — Persistence, error modeling, and polish
Goal: Make the app safer to evolve and easier to debug.

### Phase 4 — Optional larger architectural upgrades
Goal: Only after the earlier phases are stable.

---

# Phase 1 — Runtime architecture cleanup and source pipeline reliability

## P1.1 Break up `MainActivity` coordination responsibilities
Status: IN_PROGRESS
Priority: High

### Objectives
- reduce `MainActivity` size and responsibility load
- isolate workflow logic from direct view/render code
- create reusable coordinators/controllers for major flows

### Proposed sub-steps
- [TODO] Identify major workflow clusters in `MainActivity`
- [TODO] Extract auth flow coordination into a dedicated component
- [DONE] Extract source loading flow into a dedicated component
- [TODO] Extract update/install flow into a dedicated component
- [TODO] Extract modal presentation helper/state manager if useful
- [TODO] Leave `MainActivity` primarily responsible for rendering/binding/navigation dispatch

### Expected deliverables
- smaller `MainActivity`
- clearer orchestration boundaries
- easier testing of workflow logic without full activity context

### Validation
- app still launches
- key flows still function:
  - search
  - details
  - source loading
  - playback prep
  - settings auth flow
  - update check/install prompt

---

## P1.2 Make provider source fetching concurrent
Status: TODO
Priority: High

### Objectives
- reduce total source lookup latency
- preserve per-provider failure isolation
- preserve progress updates

### Proposed sub-steps
- [TODO] Refactor `SourceRepositoryImpl.findSources` to use structured concurrency
- [TODO] run provider searches in parallel
- [TODO] add per-provider timeout behavior
- [TODO] preserve progress callbacks for STARTED / COMPLETED / FAILED
- [TODO] ensure partial provider failure does not fail full lookup
- [TODO] confirm ranking/dedupe/cache-mark flow still works correctly after parallelization

### Nice-to-have if easy
- [TODO] capture provider latency for diagnostics
- [TODO] record timeout vs parse vs transport failure separately

### Validation
- source fetching still returns stable results
- progress modal still updates sensibly
- provider failures no longer stall unrelated providers
- unit/integration coverage updated if needed

---

## P1.3 Clean up provider enable/disable semantics
Status: TODO
Priority: Medium

### Objectives
- make provider preference behavior explicit and unsurprising
- avoid ambiguous meaning of empty/full sets

### Proposed sub-steps
- [TODO] define clearer settings model for provider selection
- [TODO] update storage semantics in `SourcePreferencesStore`
- [TODO] update UI helper logic in settings/provider modal flow
- [TODO] ensure migration behavior from existing stored prefs is reasonable

### Validation
- settings text matches real behavior
- toggling providers behaves predictably
- “all enabled” vs custom selection is unambiguous

---

## P1.4 Push source eligibility/policy logic out of UI glue where practical
Status: TODO
Priority: Medium

### Objectives
- reduce product policy logic in `MainActivity`
- move auth/provider/source eligibility rules closer to domain/use-case layer

### Proposed sub-steps
- [TODO] identify policy checks currently living in activity code
- [TODO] create domain/service helper(s) for effective provider selection / eligibility
- [TODO] reduce UI-specific filtering logic for auth/provider gating

### Validation
- source eligibility behavior remains unchanged or intentionally improved
- logic becomes easier to reason about in one place

---

# Phase 2 — Ranking, dedupe, and resolver quality

## P2.1 Refactor ranking into composable scoring rules
Status: TODO
Priority: High

### Objectives
- reduce hardcoded monolithic ranking logic
- make ranking easier to tune and test
- make future source strategy profiles possible

### Proposed sub-steps
- [TODO] define scoring rule interface(s)
- [TODO] split existing rank logic into rule components
- [TODO] preserve current behavior initially as closely as practical
- [TODO] add focused tests around rule outputs

### Validation
- ranked ordering remains sensible on representative samples
- tests cover major heuristic paths

---

## P2.2 Add ranking explanations / diagnostics
Status: TODO
Priority: Medium

### Objectives
- make source ranking more inspectable
- improve debugging and future UI trust/explanations

### Proposed sub-steps
- [TODO] define score explanation model
- [TODO] capture score contributions during ranking
- [TODO] expose explanation in debug/diagnostic path without bloating UI yet

### Validation
- diagnostic output helps explain why a source ranked where it did

---

## P2.3 Improve source dedupe provenance modeling
Status: TODO
Priority: Medium

### Objectives
- preserve richer provider provenance after hash dedupe
- avoid flattening too much information into strings/maps

### Proposed sub-steps
- [TODO] design provenance model
- [TODO] update deduper merge behavior
- [TODO] preserve compatibility with current UI presentation where needed

### Validation
- merged sources retain useful provider-origin context
- existing source UI remains functional

---

## P2.4 Expand Real-Debrid resolver test coverage significantly
Status: TODO
Priority: High

### Objectives
- protect one of the most product-critical heuristic systems
- reduce regressions in file selection logic

### Proposed sub-steps
- [TODO] add table-driven tests for movie file selection
- [TODO] add table-driven tests for episodic selection
- [TODO] add tests for sample/extras/junk exclusion
- [TODO] add tests for ambiguous titles / year matching / multi-file torrents
- [TODO] add tests around size heuristics and quality expectations

### Validation
- strong coverage around resolver heuristics
- future behavior changes become intentional instead of accidental

---

# Phase 3 — Persistence, error modeling, and polish

## P3.1 Replace fragile playback session text persistence with safer format
Status: TODO
Priority: High

### Objectives
- make playback session persistence more robust and evolvable
- reduce corruption / parsing fragility

### Proposed sub-steps
- [TODO] choose storage approach (likely JSON file or DataStore)
- [TODO] add schema/version support if file-based
- [TODO] migrate existing load path if practical
- [TODO] update tests

### Validation
- save/load still works
- invalid/partial persisted data fails safely

---

## P3.2 Improve typed error handling across key flows
Status: TODO
Priority: Medium

### Objectives
- reduce dependence on nullable strings for behavior
- improve retryability and diagnostics

### Proposed sub-steps
- [TODO] define initial error model(s) for source/auth/playback/update flows
- [TODO] apply where it gives immediate value first
- [TODO] avoid giant cross-cutting rewrite in one pass

### Validation
- UI can distinguish at least some meaningful error classes

---

## P3.3 Consolidate update flow behind cleaner boundary
Status: TODO
Priority: Medium

### Objectives
- reduce update-flow orchestration scattered across activity code
- make the update pipeline easier to test and evolve

### Proposed sub-steps
- [TODO] define update coordinator/controller boundary
- [TODO] move check/download/install readiness logic behind that boundary
- [TODO] keep permission/install UI hooks cleanly separated

### Validation
- update checks and install handoff still work
- failure modes are easier to surface cleanly

---

# Phase 4 — Optional larger architectural upgrades

## P4.1 Introduce app-level action/state/effect loop
Status: DEFERRED
Priority: Medium

### Notes
This is likely worthwhile, but should follow the earlier cleanup work unless current refactors naturally grow into it.

### Proposed direction
- `AppAction`
- `AppState`
- `AppEffect`
- thin activity renderer/binder

---

## P4.2 Revisit DI strategy
Status: DEFERRED
Priority: Low

### Notes
Options:
- better manual module split inside `AppContainer`
- or real Hilt adoption once lifecycle/component complexity grows

Do this after the runtime cleanup work settles.

---

## Execution Order Recommendation

Recommended near-term order:
1. P1.1 Break up `MainActivity`
2. P1.2 Concurrent source fetching
3. P1.3 Provider settings semantics cleanup
4. P1.4 Push source policy out of UI glue
5. P2.4 Real-Debrid resolver tests
6. P2.1 Ranking refactor
7. P2.2 Ranking explanations
8. P2.3 Dedupe provenance modeling
9. P3.1 Playback persistence upgrade
10. P3.2 Typed error handling
11. P3.3 Update flow consolidation

Reasoning:
- tackle structural runtime risks first
- then strengthen product-quality heuristics
- then harden persistence/error pathways

---

## Open Questions / Decisions Needed

### Q1. How far should the first `MainActivity` breakup go?
Options:
- conservative extraction into helper coordinators
- stronger move toward app-controller/store architecture

Current recommendation:
Start conservative, but design extracted pieces so they can later plug into a store/action model.

### Q2. Should concurrent provider fetch wait for full completion before showing the source list?
Options:
- keep current “wait then show” behavior but parallelize internally first
- move immediately to incremental result rendering

Current recommendation:
Parallelize first without changing UX too much. Then consider incremental rendering as a follow-up once correctness is stable.

### Q3. For playback persistence, JSON file or DataStore?
Current recommendation:
If only a single/current session record is needed now, JSON file is probably enough.
If continue-watching/history is about to grow meaningfully, prefer DataStore or Room planning soon.

### Q4. How much backward-compat migration is needed for stored preferences/session state?
Current recommendation:
Support lightweight migration where easy, but do not overcomplicate early cleanup work for tiny local state.

---

## Risks / Watchouts

- Refactoring `MainActivity` can easily cause regressions in focus, modal handling, or navigation sequencing.
- Parallel provider fetch can change diagnostics ordering and expose hidden thread-safety assumptions.
- Ranking refactors can silently worsen real-world source quality even if tests still pass.
- Resolver tests can become too synthetic if not based on real observed torrent naming patterns.
- Storage migrations can produce confusing resume behavior if old state is partially compatible but semantically stale.

---

## Progress Log

### 2026-04-04 07:52 UTC
- Created initial execution plan as a living implementation document.
- Seeded plan from architecture/code review findings.
- Established phases, priorities, validation expectations, and update discipline.
- No code changes implemented yet under this plan.

### 2026-04-04 07:59 UTC
- Completed the first P1.1 extraction pass by moving source loading/progress coordination out of `MainActivity` into `SourceLoadingCoordinator`.
- Updated `MainActivity` to delegate source loading, cancellation, and progress snapshot access to the coordinator.
- Added targeted coordinator tests covering progress propagation and current auth-linked filtering behavior.
- Validation: `./gradlew testDebugUnitTest` passed.
- Follow-up noted: source auth/provider eligibility semantics still need dedicated cleanup under P1.4 because current behavior is not yet intuitive.

---

## Scope Changes

### 2026-04-04
- Initial scope established.
- During the source-loading extraction, preserved existing auth-linked source filtering behavior instead of changing product policy mid-refactor.
- Policy cleanup remains explicitly scheduled under P1.4.

---

## Session Start

### 2026-04-04 07:54 UTC
Intended task: Begin P1.1 with the first safe extraction pass by moving source loading coordination and progress handling out of `MainActivity` into a dedicated component.

---

## Definition of Done for This Plan

This execution plan remains active until:
- all intended accepted tasks are marked `DONE`, `DEFERRED`, or explicitly removed
- relevant validation is recorded
- major follow-up items are either captured in docs/issues or scheduled next

When major implementation begins, this file must be updated alongside the code changes.
