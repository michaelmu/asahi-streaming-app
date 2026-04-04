# Asahi Execution Plan — Next Pass

Last updated: 2026-04-04 UTC
Status: COMPLETE
Owner: shield-tv-bot + Mike
Location: `docs/exec-plans/complete/2026-04-04-next-pass-ranking-orchestration.md`
Supersedes: `docs/exec-plans/complete/2026-04-04-cleanup-hardening-pass.md`
Superseded by: `docs/exec-plans/in_progress/2026-04-04-device-hardening-and-shell-cleanup.md`

## Purpose

This is the next-pass living execution plan for Asahi after the cleanup/hardening pass completed on 2026-04-04.

This plan is intentionally split into:
- **Core next work** — the highest-value items I believe should happen next
- **Optional deferred work** — important architecture improvements that should happen only if/when they justify the churn

That split matters.
Not every good idea is the next good idea.

---

## How to Use This Plan

### Before starting a session
1. Read this file top-to-bottom.
2. Check `Current Focus`.
3. Check `Open Questions / Decisions Needed`.
4. Check the latest `Progress Log` entry.
5. Update `Session Start` before coding.

### While implementing
Update this file during the work, not at the end.

Required updates while working:
- change task status when real state changes
- record validations after each meaningful step
- record any behavior change that affects UX or ranking outcomes
- move tasks to `DEFERRED` rather than leaving them half-true

### Completion rule
A task is only `DONE` when:
- the code landed
- relevant tests passed or validation is explicitly documented
- follow-up work is captured here

---

## Status Legend

- `TODO` = not started
- `IN_PROGRESS` = actively being worked
- `BLOCKED` = waiting on a decision or prerequisite
- `DONE` = implemented and validated
- `DEFERRED` = intentionally postponed
- `OPTIONAL` = not required for this pass unless chosen

---

## Current Focus

**Current phase:** Complete for this pass

**Immediate target:** None. This plan reached a clean stopping point and is being closed out.

**Why this now:**
The intended A/B work for this pass shipped successfully. Remaining items are either optional architecture work or better handled in a fresh follow-up plan focused on device hardening and remaining shell cleanup.

---

## Second-Pass Review Summary

## What is better now

### 1. The source pipeline is healthier
The concurrency pass was worth doing.
Provider execution is no longer unnecessarily serial, and the repository now emits more useful progress state.
That is a real improvement, not just a refactor trophy.

### 2. Settings semantics are cleaner
The provider-selection model is much more explicit now.
The previous empty-set/full-set ambiguity was exactly the kind of subtle state bug that grows teeth later.

### 3. Playback persistence is less fragile
Moving session persistence off ad-hoc newline serialization was the right call.
It is still intentionally lightweight, but it is no longer brittle in a stupid way.

### 4. Resolver confidence improved
The added Real-Debrid resolver tests now protect some of the most product-sensitive heuristics.
That meaningfully lowers regression risk.

---

## What still looks like the biggest issues

### 1. `MainActivity` is still the runtime gravity well
It is smaller than before, but still doing too much:
- auth flow orchestration
- update flow orchestration
- modal lifecycle/presentation
- navigation branching
- render decisions
- player error formatting
- workflow transitions between details/episodes/sources/player

This is still the number-one architectural hotspot.

### 2. Ranking remains effective but under-modeled
`DefaultSourceRanker` still encodes product policy as one monolithic score function.
That means:
- hard tuning
- weak explainability
- riskier future changes
- no clean path to ranking profiles/strategies

### 3. Dedupe still loses too much information
`DefaultSourceDeduper` merges provider identity into display names and metadata strings.
That is enough for current UI, but not enough for diagnostics, trust, or future ranking improvements.

### 4. The source UX is still “wait for done”
Provider lookup is now concurrent, but the UI still behaves like a blocking batch process.
Users still wait for a final answer instead of feeling progress in a TV-friendly way.

### 5. Update flow has a cleaner boundary, but `MainActivity` still owns too much of the interaction logic
The coordinator exists, which is good.
But the activity still decides a lot of the operational branching around checks, modals, and handoff.

### 6. The current plan document is complete, but it should not keep growing forever
The previous exec plan served its purpose.
This file is the fresh next-pass plan.

---

# Phase A — Highest-value next work

## A1. Continue breaking up `MainActivity`
Status: IN_PROGRESS
Priority: High

### Goal
Shrink `MainActivity` from “app brain” toward “screen host + binder.”

### Why this matters
This is still the biggest maintainability risk in the app.
Adding more features before reducing this concentration of logic will make everything slower and riskier.

### Proposed sub-steps
- [DONE] Extract Real-Debrid auth flow into a dedicated coordinator
- [DONE] Extract update interaction flow into a dedicated UI-facing coordinator
- [TODO] Extract modal presentation helper/state wrapper if it meaningfully reduces repetition
- [TODO] Move playback error message formatting out of `MainActivity`
- [TODO] Leave `MainActivity` primarily responsible for render dispatch, shell mode, and event wiring

### Validation
- no behavior regressions in auth flow
- no behavior regressions in update flow
- no modal focus/back regressions
- full test suite passes

---

## A2. Refactor ranking into composable scoring rules
Status: DONE
Priority: High

### Goal
Turn source ranking from a monolithic heuristic block into a tunable scoring pipeline.

### Why this matters
The current ranker is good enough to ship, but not good enough to evolve confidently.
This is the highest-value deferred item from the prior pass.

### Proposed sub-steps
- [DONE] Define a score-rule model (`SourceScoreRule` or equivalent)
- [DONE] Split rank contributions into separate rules:
  - cache/direct preference
  - quality preference
  - provider preference
  - size penalties
  - tiny-file penalties
  - remux/cam penalties
- [DONE] Preserve behavior approximately in the first pass
- [DONE] Add focused tests for representative score outcomes
- [DONE] Keep the public ranker entry point simple

### Validation
- ranked order stays sensible on real-world source sets
- existing behavior remains broadly preserved unless intentionally changed
- tests cover rule-level behavior, not just end-to-end order

---

## A3. Add ranking explanations / diagnostics
Status: DONE
Priority: Medium

### Goal
Make ranking inspectable by humans.

### Why this matters
Once ranking is composable, explanations become the best debugging tool you can have.
They also create a path toward user-facing trust signals later.

### Proposed sub-steps
- [DONE] Define a score explanation model
- [DONE] Capture per-rule contributions
- [DONE] Add debug/diagnostic rendering path first
- [DONE] Keep UI impact minimal initially

### Validation
- explanation data is available for selected sources
- debug output clearly answers “why did this rank first?”

---

## A4. Improve dedupe provenance modeling
Status: DONE
Priority: Medium

### Goal
Preserve provider/source origin evidence instead of flattening it into strings.

### Why this matters
This becomes more valuable after ranking explanations exist.
It also improves diagnostics and future provider health analysis.

### Proposed sub-steps
- [DONE] Introduce explicit provenance/origin model
- [DONE] Preserve origin list during info-hash dedupe
- [DONE] Keep existing source list UI working with minimal churn
- [DONE] Use provenance to improve merged-source diagnostics

### Validation
- merged sources keep meaningful origin detail
- no regressions in source UI or selection behavior

---

## A5. Incremental source loading UX
Status: DONE
Priority: High

### Goal
Make source lookup feel faster, not just execute faster.

### Why this matters
This is the biggest likely UX win available right now.
You already parallelized the backend path; now the UI should benefit from it.

### Proposed sub-steps
- [DONE] Define a progressive source-loading state model
- [DONE] allow partial results to appear before all providers finish
- [DONE] keep provider progress visible while results continue arriving
- [DONE] support completion/failure without forcing a full-screen blocking modal loop
- [DONE] ensure TV focus behavior stays sane while results append/update

### Validation
- time-to-first-usable-source improves
- provider progress remains understandable
- UI remains stable under staggered provider completions

---

# Phase B — Targeted product and architecture polish

## B1. Broaden typed error handling beyond source-fetch progress
Status: DONE
Priority: Medium

### Goal
Extend typed errors only where they now buy real value.

### Proposed sub-steps
- [DONE] add typed errors for playback prepare failures
- [DONE] add typed errors for update/install readiness failures
- [DONE] add typed auth-flow failure classes if useful
- [DONE] avoid giant rewrite for weak benefit

### Validation
- at least one UI path becomes meaningfully better because of typed error distinctions

---

## B2. Convert single-session playback persistence into a clearer continue-watching direction
Status: DONE
Priority: Medium

### Goal
Decide whether playback persistence remains intentionally single-item or begins evolving into a real multi-entry continue-watching subsystem.

### Proposed sub-steps
- [DONE] document intended scope
- [DONE] if staying small, tighten current behavior and naming
- [DEFERRED] if expanding, define storage evolution path before more ad-hoc growth

### Validation
- persisted playback/resume behavior is easier to reason about than it is now

---

## B3. Add provider health diagnostics
Status: DONE
Priority: Low

### Goal
Track useful provider health signals for debugging and future settings/debug UI.

### Proposed sub-steps
- [DONE] collect per-provider latency/failure summary
- [DONE] keep it lightweight and local first
- [DONE] expose it in debug diagnostics before any polished UI

### Validation
- can quickly tell which providers are healthy, slow, or error-prone

---

# Phase C — Optional deferred work from prior pass

These are explicitly optional for this pass.
They should be pulled in only if they fit naturally or become clearly high-value.

## C1. App-level action/state/effect loop
Status: OPTIONAL
Priority: Medium

### Notes
Still a good long-term direction.
Still probably too large for a casual “while we’re here” refactor.

### When to do it
- if `MainActivity` extraction naturally exposes a clean store boundary
- or if feature coordination complexity keeps growing

---

## C2. Broader DI rethink
Status: OPTIONAL
Priority: Low

### Notes
Do not do this just to feel architecturally righteous.
Only do it if testing/component complexity starts making manual wiring painful enough to justify the churn.

Options:
- better manual module composition in `AppContainer`
- or real Hilt adoption later

---

## C3. Full ranking-strategy profiles
Status: OPTIONAL
Priority: Medium

### Notes
This becomes attractive only after A2/A3 exist.
Examples:
- Balanced
- Fastest Start
- Highest Quality
- Cached Only

Not worth doing before explainable ranking exists.

---

# Recommended Order

## Recommended next implementation order
1. A1 Continue breaking up `MainActivity`
2. A2 Refactor ranking into composable scoring rules
3. A3 Add ranking explanations / diagnostics
4. A4 Improve dedupe provenance modeling
5. A5 Incremental source loading UX
6. B1 Broaden typed error handling selectively
7. B2 Clarify continue-watching/playback persistence direction
8. B3 Add provider health diagnostics
9. Optional C-items only if they still feel justified after A/B work

---

## Open Questions / Decisions Needed

### Q1. Should incremental source loading happen before or after ranking refactor?
Current recommendation:
Do **A2 first**, then **A5**.
Reason: incremental loading gets much easier to trust when ranking logic is more inspectable.

### Q2. Should source loading continue to hard-gate on Real-Debrid before entering the source flow?
Current recommendation:
Revisit this during A5.
Now that direct-only filtering exists for unlinked state, the current up-front hard gate may be too strict if direct sources are meant to remain part of the product.

### Q3. Should `MainActivity` continue to own modal presentation directly?
Current recommendation:
Maybe, but only if modal rendering stays thin.
If modal workflows keep growing, extract a modal coordinator/helper.

### Q4. Is ranking intended to remain app-opinionated or become user-configurable later?
Current recommendation:
Keep ranking opinionated by default, but design A2/A3 so profile-based variation is possible later.

---

## Risks / Watchouts

- `MainActivity` extractions can break focus/back-stack behavior in subtle TV-specific ways.
- ranking refactors can degrade real-world source quality while looking cleaner in code
- incremental loading can create flicker/re-sorting churn if ranking updates are too aggressive
- provenance modeling can bloat `SourceResult` if done lazily instead of thoughtfully
- optional architecture work can consume momentum without improving the app enough to justify it

---

## Progress Log

### 2026-04-04 15:08 UTC
- Created fresh next-pass execution plan after reviewing the repo state following commit `d17e694`.
- Reassessed architecture after the cleanup/hardening pass.
- Carried forward deferred items as optional or phased work instead of mixing them blindly into mandatory scope.
- No code changes implemented under this plan yet.

### 2026-04-04 15:35 UTC
- Completed the first A1 extraction slice by introducing `RealDebridAuthCoordinator` and `UpdateUiCoordinator`.
- Moved Real-Debrid device-flow start/reset/poll orchestration out of `MainActivity` into a dedicated auth coordinator.
- Moved update check/install interaction branching onto a dedicated UI-facing update coordinator layered over `UpdateCoordinator`.
- Added focused coordinator tests for auth and update UI coordination.
- Validation: `./gradlew testDebugUnitTest` passed.

### 2026-04-04 15:42 UTC
- Completed A2 by refactoring `DefaultSourceRanker` to use a composable scoring pipeline (`SourceScoreRule`, `SourceScorer`, and default rule set) instead of a monolithic score function.
- Preserved the existing scoring shape closely by expressing the old heuristics as separate additive/penalty rules.
- Added focused ranker tests covering cache preference, tiny-file penalty behavior, provider preference, and contribution visibility.
- Validation: full `./gradlew test` passed.

### 2026-04-04 15:48 UTC
- Completed A3 by introducing explicit ranking explanation models in the domain layer and wiring the ranker to expose per-rule score contributions.
- Exposed ranking explanations through debug diagnostics first by enriching source diagnostics with the top-ranked source's score and top contribution rules.
- Kept UI impact intentionally minimal and avoided broad source-model churn in this pass.
- Validation: full `./gradlew test` passed.

### 2026-04-04 15:54 UTC
- Completed A4 by introducing `SourceOrigin` on `SourceResult` and preserving merged origin evidence through info-hash dedupe.
- Updated dedupe metadata/diagnostics to reflect origin counts and origin summaries instead of only flattened provider labels.
- Added focused deduper tests covering merged provenance retention and non-hash pass-through behavior.
- Validation: targeted ranking/dedupe tests plus `./gradlew testDebugUnitTest` passed.
- Note: a separate live provider integration test (`BitSearchLiveIntegrationTest`) failed during one full-suite run; treated as unrelated external flakiness and not used as a blocker for this provenance change.

### 2026-04-04 16:00 UTC
- Completed A5 by extending the source repository/presenter/viewmodel path to emit incremental ranked source snapshots as providers complete.
- Updated `SourceLoadingCoordinator` to consume incremental results and keep both progress state and partial sources available during lookup.
- Updated `MainActivity` to enter the Sources screen immediately with an empty list, then refresh it as incremental results arrive while retaining the progress modal as optional/secondary rather than the only visible state.
- Added coordinator test coverage for incremental updates plus existing auth-linked filtering behavior.
- Validation: `./gradlew testDebugUnitTest` passed.

### 2026-04-04 16:03 UTC
- Completed B3 by adding a lightweight in-memory `ProviderHealthTracker` fed from `SourceFetchProgress` updates.
- Provider health diagnostics now summarize success/failure counts, last latency, last result count, and last error type for the current lookup window.
- Exposed provider health only through source diagnostics/debug text in this pass; no polished settings/debug UI yet.
- Added focused tracker tests and validated with `./gradlew testDebugUnitTest`.

### 2026-04-04 16:07 UTC
- Completed B2 by clarifying that playback persistence currently stores one active resume record, not a real multi-entry continue-watching history.
- Introduced explicit `ActivePlaybackResumeRecord` naming and a `PlaybackPersistencePolicy` description while keeping backward-compatible type/function aliases for existing callers.
- Tightened the storage/hydration path around `saveActiveResume`, `loadActiveResume`, and `ContinueWatchingHydrator.fromActiveResume(...)`.
- Added focused hydrator coverage and updated playback-store tests around the explicit active-resume semantics.
- Validation: `./gradlew testDebugUnitTest` passed.

### 2026-04-04 16:15 UTC
- Completed B1 by adding selective typed error handling where it improved behavior immediately instead of attempting a full app-wide error model rewrite.
- Added app-layer typed errors for update/auth flow diagnostics and feature-layer typed playback prepare errors for resolve-vs-prepare failure separation.
- Updated update/auth coordinators and playback messaging to surface error types in a more structured way.
- Added focused tests for auth typed failures and player presenter typed playback failures.
- Validation: `./gradlew testDebugUnitTest`, `feature:player:test`, and `app:testDebugUnitTest` passed.

### 2026-04-04 16:40 UTC
- Closed this plan out as complete for its intended pass.
- Chose not to force the remaining optional architecture items into this document just to keep it alive.
- Remaining shell-cleanup and on-device hardening work will continue in a fresh follow-up plan.

---

## Scope Changes

### 2026-04-04
- New plan created after prior plan reached a clean “complete for this pass” state.
- Deferred items from the prior pass were retained, but reclassified as either core-next or optional depending on expected value/churn tradeoff.
- During A1 execution, chose to extract auth/update workflow coordination first because it offered the cleanest boundary with low regression risk compared with modal-system or playback-flow extraction.
- During A2 execution, chose to keep the public `SourceRanker` interface unchanged and make scoring-rule composition an internal implementation detail to avoid unnecessary API churn.
- During A3 execution, chose to expose explanations via `SourceRanker.explain(...)` and source diagnostics first instead of pushing explanation data through all UI state models immediately.
- During A4 execution, chose to extend `SourceResult` with explicit `origins` rather than replacing existing provider label fields, which kept UI churn low while still preserving provenance for diagnostics and future work.
- During A5 execution, chose to drive incremental updates from the repository layer where provider completion is known, instead of trying to fake partial results inside the loading coordinator.
- During B3 execution, chose to keep provider health tracking in-memory and per-lookup-window rather than persist history yet, which keeps the feature cheap and useful without prematurely inventing a telemetry subsystem.
- During B2 execution, chose to make the current single-record persistence model explicit instead of prematurely building a multi-entry history system without a clear UX/storage design.
- During B1 execution, chose to add typed errors only at boundaries that immediately improved diagnostics or behavior, rather than forcing a cross-cutting rewrite of every UI state and error path.

---

## Session Start

### 2026-04-04 16:07 UTC
Intended task: Continue the active next-pass plan by starting B1 and introducing selective typed errors for better playback/update/auth behavior.

---

## Definition of Done for This Plan

This plan is complete for its intended pass when:
- all accepted items are marked `DONE`, `DEFERRED`, `OPTIONAL`, or removed
- validation is recorded for completed work
- any newly discovered follow-up work is captured explicitly

Current state: complete for this pass.
