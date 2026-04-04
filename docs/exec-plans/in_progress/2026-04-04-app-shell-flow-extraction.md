# Asahi Execution Plan — App Shell Flow Extraction

Last updated: 2026-04-04 UTC
Status: COMPLETE
Owner: shield-tv-bot + Mike
Location: `docs/exec-plans/in_progress/2026-04-04-app-shell-flow-extraction.md`
Supersedes: none

## Purpose

This pass exists to turn the latest architectural review into concrete, low-risk shell cleanup work.

It is focused on two things:
- identifying the highest-value next refactor targets in the current app shell
- extracting one of the highest-value workflow seams out of `MainActivity` while keeping behavior stable

This is intentionally not a broad app-state rewrite.
The goal is to reduce orchestration concentration in `MainActivity` by continuing the existing coordinator-driven cleanup style.

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

### Completion rule
A task is only `DONE` when:
- the code landed
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

**Current phase:** Complete

**Immediate target:** none — the controlled settings source-preferences cleanup for this plan is complete.

**Why this now:**
This plan now covers three controlled shell reductions: playback launch, browse search/detail loading, and source-preferences settings actions. Broader settings modal/auth/update flow remains separate future work.

> Update this section whenever the active phase or immediate target changes.

---

## Repository Reality Check

Before implementation begins, confirm:
- `app/src/main/kotlin/ai/shieldtv/app/MainActivity.kt` exists and still owns playback launch, source loading triggers, settings modals, navigation wiring, and shell rendering.
- `AppContainer`, `AppCoordinator`, `SourceLoadingCoordinator`, `RealDebridAuthCoordinator`, `UpdateUiCoordinator`, and `SourcePreferencesCoordinator` already exist, so the current architecture already favors incremental workflow extraction over a full rewrite.
- `preparePlayback(...)` in `MainActivity` is a real extraction seam because it currently mixes policy checks, user-facing message construction, view-model calls, state persistence, history recording, and navigation updates.
- The app source layout is under `app/src/main/kotlin`, not Java-only paths.
- Recent repo state includes an uncommitted UI validation folder under `docs/ui-validation/2026-04-04-home-shortcuts-visible-check/`, so commits during this pass must stay honest and avoid accidentally sweeping unrelated validation artifacts into the same change.

---

## Locked Decisions

- Do not introduce a full app-wide state/effect loop in this pass.
- Do not introduce a DI framework migration in this pass.
- Keep the cleanup incremental and behavior-preserving.
- Favor small focused coordinators over large abstraction layers.
- Keep plan/docs updates honest and in sync with the implementation.

---

## Background / Review Summary

The repo review found that Asahi’s module split, source pipeline, and integration layers are in good shape.
The primary architectural risk is still app-shell orchestration concentration inside `MainActivity`.

The highest-value next refactor candidates identified during review were:
1. playback launch and playback-session workflow
2. search/details/sources browse workflow
3. settings modal/action workflow
4. navigation/back-stack transition policy
5. shell state restoration / resume reconciliation

This pass starts with the first item because it is both high-impact and relatively self-contained.

---

# Phase A — Audit and targeted extraction

## A1. Capture the top refactor candidates in docs
Status: DONE
Priority: High

### Goal
Write down the most valuable next shell refactor targets so future sessions do not have to rediscover them.

### Why this matters
The architecture review is only useful if its findings become durable repo context.

### Proposed sub-steps
- [DONE] add a concise architecture review note under `docs/`
- [DONE] record the top five next refactor targets
- [DONE] note why playback-launch extraction was chosen first

### Validation
- a future session can identify the next shell seams from repo docs alone
- doc content matches the code reality at the time of writing

---

## A2. Extract playback launch workflow from `MainActivity`
Status: DONE
Priority: High

### Goal
Move playback-launch orchestration out of `MainActivity` into a focused coordinator while keeping current behavior stable.

### Why this matters
Playback launch currently bundles multiple responsibilities in one activity-level method and is a clear ongoing maintenance risk.

### Proposed sub-steps
- [DONE] define a small coordinator API around playback launch
- [DONE] move auth-gating, player prepare result handling, resume/start-position choice, persistence updates, and history updates behind that coordinator
- [DONE] keep activity-owned responsibilities limited to UI message application, render triggers, and navigation handoff where needed
- [DEFERRED] add focused tests if the extracted shape supports them cleanly

### Validation
- `./gradlew testDebugUnitTest` passes
- `./gradlew assembleDebug` passes
- behavior for successful playback prep and blocked Real-Debrid playback remains unchanged

---

# Phase B — Follow-up shell map

## B1. Document the next recommended extractions after playback
Status: DONE
Priority: Medium

### Goal
Leave behind a clear next-step map after the chosen extraction lands.

### Why this matters
Refactors go stale fast if the repo does not preserve what should come next.

### Proposed sub-steps
- [DONE] document likely next seam after playback extraction
- [DONE] identify whether browse-flow or settings-flow is the cleaner next move

### Validation
- docs clearly state what should happen after this pass

---

# Optional Work

## O1. Extract browse-flow coordinator in the same pass
Status: DONE
Priority: Low

### Notes
Pulled in after the playback extraction stayed small and validation stayed green.

---

## Recommended Order

1. A1 capture audit findings in docs
2. A2 extract playback launch workflow
3. validate with tests/build
4. B1 document the next recommended extraction
5. O1 extract browse-flow coordinator after playback if scope remains controlled

---

## Open Questions / Decisions Needed

### Q1. Should playback navigation (`showPlayer`) live inside the extracted coordinator or remain in `MainActivity`?
Current recommendation:
Let the coordinator return a structured result and keep final navigation/render triggers in `MainActivity` unless a cleaner boundary appears naturally.

### Q2. Should failure/debug message formatting move with the coordinator too?
Current recommendation:
Only if it reduces duplication materially. Do not force unrelated formatting churn into the same pass.

### Q3. Should browse-flow extraction happen immediately after playback extraction?
Current recommendation:
Yes for this pass, since the playback extraction stayed controlled and browse flow remained the next highest-value seam.

---

## Risks / Watchouts

- accidentally broadening this into a generic shell rewrite
- mixing unrelated uncommitted UI validation artifacts into the same commit
- changing playback behavior subtly while “just refactoring”
- burying UI-owned status/render responsibilities inside the new coordinator

---

## Validation Notes / Honesty Check

### Initial state
- Validated by: direct repo inspection of `MainActivity`, `AppCoordinator`, `AppContainer`, and existing shell coordinators
- Not validated: on-device playback flows in this session
- Known uncertainty: exact extraction shape may change once the playback method is split apart for real

### After playback extraction
- Validated by: code review against real repo state, successful coordinator extraction, and upcoming build/test validation
- Not validated: on-device playback launch on Shield in this session
- Known uncertainty: `PlaybackLaunchCoordinator` currently still depends on app-shell collaborators (`AppCoordinator`, current artwork selection, and current playback-state snapshot) and may benefit from another pass later if a cleaner interface emerges

### Browse-flow follow-up
- Validated by: repo inspection showing `runSearch(...)` and `onSearchResultSelected(...)` remained strong extraction seams with stable existing feature-view-model boundaries
- Not validated: browse-flow behavior after extraction yet in this session
- Known uncertainty: the next likely remaining browse-side seam after this extraction will be detail/episode/source transition glue rather than raw search/detail loading

### After browse-flow extraction
- Validated by: successful `./gradlew testDebugUnitTest assembleDebug` after introducing `BrowseFlowCoordinator` and rewiring `MainActivity`
- Not validated: manual on-device browse flow in this session
- Known uncertainty: details-to-episodes-to-sources navigation still lives in `MainActivity`, so browse logic is reduced but not fully extracted end-to-end

### Settings follow-up target
- Validated by: repo inspection showing source preference labels and mutations were still duplicated in `MainActivity` despite `SourcePreferencesCoordinator` already existing
- Not validated: settings behavior after this cleanup yet in this session
- Known uncertainty: auth/update/settings-modal orchestration is still a larger separate seam after the source-preferences slice is cleaned up

### After settings source-preferences cleanup
- Validated by: successful `./gradlew testDebugUnitTest assembleDebug` after routing provider selection labels, provider toggles, max-size mutations, and reset behavior through `SourcePreferencesCoordinator`
- Not validated: manual settings interaction on device in this session
- Known uncertainty: modal-page building and auth/update actions still leave the wider settings workflow concentrated in `MainActivity`

---

## Progress Log

### 2026-04-04 22:39 UTC
- Created this plan from the architecture review request.
- Confirmed the main architectural concern is still workflow concentration in `MainActivity` rather than deep module structure issues.
- Identified the top next refactor targets and selected playback launch as the first implementation seam for this pass.
- No code changes completed yet.

### 2026-04-04 22:46 UTC
- Added `docs/APP_SHELL_REVIEW_2026-04-04.md` to preserve the architecture review findings inside the repo.
- Documented the top five next refactor targets and explicitly ranked playback launch first, with browse-flow second and settings-flow third.
- Updated `README.md` to point at the new shell review note and to reflect that app-shell thinning is now an active practical focus.

### 2026-04-04 22:49 UTC
- Extracted `PlaybackLaunchCoordinator` from `MainActivity`.
- Moved playback auth gating, player prepare handling, failure message construction, continue-watching update, playback session persistence, and watch-history recording behind the new coordinator.
- Kept `MainActivity` responsible for UI state application, rendering, and final player-screen navigation.
- Deferred focused coordinator unit tests for this pass to keep scope contained.

### 2026-04-04 22:52 UTC
- Ran `./gradlew testDebugUnitTest assembleDebug` successfully before commit.
- Committed the pass as `37c988b Extract playback launch workflow from MainActivity`.
- Confirmed the unrelated UI validation folder `docs/ui-validation/2026-04-04-home-shortcuts-visible-check/` remained untracked and was not swept into the commit.

### 2026-04-04 22:47 UTC
- Resumed this plan after the playback pass to continue with the next recommended shell seam instead of creating another separate micro-plan.
- Chose browse flow as the next extraction target.
- Confirmed the concrete seam is currently `runSearch(...)` + `onSearchResultSelected(...)`, not yet the deeper details/episodes/sources transition logic.

### 2026-04-04 22:49 UTC
- Added `BrowseFlowCoordinator` to own search execution, result filtering, watched-badge decoration, detail loading, and the transition into details.
- Rewired `MainActivity` to use `SearchFeatureFactory` and `DetailsFeatureFactory` instead of constructing those feature view-models inline.
- Reduced `MainActivity` browse responsibilities to UI state reset/application, loading/status text, and screen rendering.

### 2026-04-04 22:50 UTC
- Ran `./gradlew testDebugUnitTest assembleDebug` successfully after the browse extraction.
- The long-standing non-blocking Kotlin warning in `MainActivity` about the Elvis operator in source diagnostics remains unrelated to this pass.
- Next likely seam remains settings modal/action flow, followed by deeper details/episodes/sources transition cleanup.

### 2026-04-04 22:53 UTC
- Continued the same plan for a narrow settings-focused cleanup rather than starting the full settings modal/auth/update extraction.
- Chose the source-preferences slice specifically because `SourcePreferencesCoordinator` already existed and `MainActivity` was still reimplementing much of that behavior inline.
- Scoped this follow-up narrowly to provider-selection labels, provider toggles, max-size mutations, and reset behavior.

### 2026-04-04 22:55 UTC
- Rewired `MainActivity` to route source-preference label generation and mutations through `SourcePreferencesCoordinator`.
- Removed duplicated provider toggle/save logic from `MainActivity` in favor of the existing coordinator.
- Left auth, update, and modal-page orchestration untouched to avoid broadening the pass beyond the source-preferences seam.

### 2026-04-04 22:56 UTC
- Ran `./gradlew testDebugUnitTest assembleDebug` successfully after the settings cleanup.
- The long-standing non-blocking Kotlin warning in `MainActivity` remains unrelated to this pass.
- Next likely seam remains broader settings modal/auth/update flow, followed by deeper details/episodes/sources transition cleanup.

---

## Scope Changes

### 2026-04-04
- Initial scope established as: document top refactor candidates + implement one focused extraction + update docs honestly.
- Browse-flow extraction was initially kept out of required scope unless playback extraction stayed small.
- After playback landed cleanly, browse-flow extraction was pulled into the same plan as a controlled follow-up.
- A narrow settings source-preferences cleanup was then pulled in as another controlled follow-up.
- Full settings modal/auth/update extraction remains separate follow-up work.
- The deeper details/episodes/sources transition glue was intentionally not folded into the browse extraction to keep this pass controlled.

---

## Session Start

### 2026-04-04 22:39 UTC
Intended task: Create the plan, document top shell refactor candidates, and extract playback launch workflow from `MainActivity`.

### 2026-04-04 22:47 UTC
Intended task: Continue the same plan by extracting the browse flow (`runSearch` and `onSearchResultSelected`) into a focused coordinator.

### 2026-04-04 22:53 UTC
Intended task: Continue the same plan with a narrow settings cleanup that routes source-preferences behavior through `SourcePreferencesCoordinator`.

---

## Definition of Done

This plan is complete for its intended pass when:
- the top refactor candidates are documented in repo docs
- playback launch orchestration has been extracted from `MainActivity` into a focused coordinator
- validation is recorded for the implemented work
- follow-up work is explicitly captured
- `Current Focus` no longer implies unfinished required work
- the file is ready to move out of `in_progress/` without misleading anyone
