# Asahi Execution Plan — App Shell Flow Extraction

Last updated: 2026-04-04 UTC
Status: COMPLETE
Owner: shield-tv-bot + Mike
Location: `docs/exec-plans/complete/2026-04-04-app-shell-flow-extraction.md`
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

**Immediate target:** none — the source-loading UI lifecycle extraction and hygiene sweep are complete.

**Why this now:**
This plan now covers the major shell reductions requested in this pass. Remaining work is no longer about a giant `MainActivity` knot so much as normal ongoing product work and smaller maintenance decisions.

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

### Settings shell coordination follow-up
- Validated by: repo inspection showing provider summaries, history/favorites status labels, auth reset/start calls, and update checks were still spread through `MainActivity`
- Not validated: behavior after centralizing those entry points yet in this session
- Known uncertainty: update install branching, auth result modals, and provider/size picker page composition still remain activity-owned UI workflow

### After broader settings-shell coordination
- Validated by: successful `./gradlew testDebugUnitTest assembleDebug` after introducing `SettingsCoordinator` and rewiring settings summaries plus auth/update entry points through it
- Not validated: manual settings navigation on device in this session
- Known uncertainty: most settings modal composition and branching still lives in `MainActivity`, so this pass reduces shell sprawl but does not finish settings extraction

### Details/episodes/sources transition follow-up
- Validated by: repo inspection showing `onBrowseEpisodes`, `onFindSources`, `onEpisodeSelected`, and `onEpisodePlay` still encoded navigation/auth/source-loading decisions inline in `MainActivity`
- Not validated: behavior after extraction yet in this session
- Known uncertainty: actual source-loading lifecycle and progress UI remain in `MainActivity` + `SourceLoadingCoordinator`, so this pass only targets transition policy

### After details/episodes/sources transition cleanup
- Validated by: successful `./gradlew testDebugUnitTest assembleDebug` after introducing `DetailsNavigationCoordinator` and rewiring details/episodes source-transition callbacks through it
- Not validated: manual source lookup flow on device in this session
- Known uncertainty: source-loading progress UI, diagnostics, and completion handling still live in `MainActivity`

### Back-stack/navigation follow-up
- Validated by: repo inspection showing `handleBackPress()` still encoded destination-specific back behavior inline in `MainActivity`
- Not validated: back-navigation behavior after extraction yet in this session
- Known uncertainty: this pass only centralizes policy; it does not introduce a richer navigator/history stack

### After back-stack/navigation cleanup
- Validated by: successful `./gradlew testDebugUnitTest assembleDebug` after introducing `BackNavigationCoordinator` and routing back behavior through it
- Not validated: manual controller back-navigation on device in this session
- Known uncertainty: app navigation is still state-based rather than history-stack-based, so this remains explicit policy rather than generalized navigation infrastructure

### Settings modal composition follow-up
- Validated by: repo inspection showing source-preferences modal-page building still lived inline in `MainActivity`
- Not validated: behavior after modal-spec extraction yet in this session
- Known uncertainty: auth/update-specific modal branching still remains activity-owned after this pass

### After source-preferences modal composition cleanup
- Validated by: successful `./gradlew testDebugUnitTest assembleDebug` after introducing `SettingsModalCoordinator` and routing size/provider/reset modal specs through it
- Not validated: manual settings modal navigation on device in this session
- Known uncertainty: auth/update modals and some generic modal helpers still remain in `MainActivity`

### Auth/update modal branching follow-up
- Validated by: repo inspection showing Real-Debrid flow success/failure/timeout modals and update check/install modals still built inline in `MainActivity`
- Not validated: behavior after modal-spec extraction yet in this session
- Known uncertainty: generic helper plumbing like `showInfoModal(...)`, QR-code view creation, and update side effects still remain activity-owned

### After auth/update modal branching cleanup
- Validated by: successful `./gradlew testDebugUnitTest assembleDebug` after extending `SettingsModalCoordinator` and routing Real-Debrid/update modal specs through it
- Not validated: manual auth/update flow interaction on device in this session
- Known uncertainty: final shell cleanup opportunities are now more about helper consolidation than large missing workflow coordinators

### Final source-loading + hygiene sweep
- Validated by: repo inspection showing source-loading UI application/progress modal behavior still lived inline in `MainActivity`, plus leftover dead helper methods remained from earlier extractions
- Not validated: behavior after the final sweep yet in this session
- Known uncertainty: this pass aims for cleanup and consolidation, not another deep architectural shift

### After source-loading + hygiene sweep
- Validated by: successful `./gradlew testDebugUnitTest assembleDebug` after introducing `SourceLoadUiCoordinator`, routing source-loading UI state/progress handling through it, and removing dead helper methods
- Not validated: manual source-loading flow on device in this session
- Known uncertainty: generic app-shell helper methods still exist, but there is no longer an obvious large workflow seam left from the original audit list

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

### 2026-04-04 22:57 UTC
- Continued the same plan with a broader but still controlled settings-shell pass.
- Added an app-level `SettingsCoordinator` to centralize provider/source-preference summaries, favorites/history status labels, Real-Debrid auth reset/start entry points, and update-check entry points.
- Intentionally left modal composition and update-install/auth-result branching in `MainActivity` to avoid collapsing too many UI responsibilities into one extraction.

### 2026-04-04 22:59 UTC
- Ran `./gradlew testDebugUnitTest assembleDebug` successfully after the broader settings-shell coordination pass.
- The long-standing non-blocking Kotlin warning in `MainActivity` remains unrelated to this work.
- The clearest remaining shell seams are now settings modal/action workflow and details/episodes/sources transition cleanup.

### 2026-04-04 23:01 UTC
- Continued the same plan with the details/episodes/sources transition seam.
- Added `DetailsNavigationCoordinator` to own details-to-episodes navigation and the auth-gated decision about whether episode/movie source lookup should proceed.
- Kept actual source loading, progress UI, and diagnostics in the existing `loadSourcesFor(...)` path and `SourceLoadingCoordinator`.

### 2026-04-04 23:02 UTC
- Ran `./gradlew testDebugUnitTest assembleDebug` successfully after the transition cleanup.
- The long-standing non-blocking Kotlin warning in `MainActivity` remains unrelated to this work.
- The clearest remaining shell seams are now settings modal/action workflow and back-stack/navigation policy.

### 2026-04-04 23:11 UTC
- Continued the same plan with back-stack/navigation policy cleanup.
- Added `BackNavigationCoordinator` to own destination-specific back behavior and to signal whether playback should be stopped on back out of player.
- Kept navigation state mutation in `AppCoordinator` and kept rendering/playback side effects in `MainActivity`.

### 2026-04-04 23:12 UTC
- Ran `./gradlew testDebugUnitTest assembleDebug` successfully after the back-navigation cleanup.
- The long-standing non-blocking Kotlin warning in `MainActivity` remains unrelated to this work.
- The biggest remaining shell seam is now settings modal/action composition, with source-loading UI lifecycle as a possible later cleanup.

### 2026-04-04 23:13 UTC
- Continued the same plan with a focused settings modal-composition pass.
- Added `SettingsModalCoordinator` to build modal specs for movie/TV size pickers, provider-selection pages, provider-selection actions, and reset confirmation.
- Kept auth/update-specific modal flows in `MainActivity` for now to avoid broadening the pass too far.

### 2026-04-04 23:14 UTC
- Ran `./gradlew testDebugUnitTest assembleDebug` successfully after the settings modal-composition cleanup.
- The long-standing non-blocking Kotlin warning in `MainActivity` remains unrelated to this work.
- Remaining shell work is now mostly auth/update settings modal branching plus any later optional source-loading UI lifecycle cleanup.

### 2026-04-04 23:17 UTC
- Continued the same plan with the auth/update modal-branching pass.
- Extended `SettingsModalCoordinator` to build modal specs for Real-Debrid link failures, link flow display, successful linking, timeout handling, update check failure, update availability, and update install result states.
- Rewired `MainActivity` to consume those modal specs while keeping external side effects (launching intents, downloads, QR view creation) in the activity.

### 2026-04-04 23:18 UTC
- Fixed a build break caused by using the wrong `AppUpdateInfo` import path inside `SettingsModalCoordinator`.
- Re-ran `./gradlew testDebugUnitTest assembleDebug` successfully after the fix.
- The long-standing non-blocking Kotlin warning in `MainActivity` remains unrelated to this work.

### 2026-04-04 23:22 UTC
- Continued the same plan with the final source-loading UI lifecycle extraction and hygiene sweep.
- Added `SourceLoadUiCoordinator` to centralize source search label construction, shell-state application for source loading, progress modal spec creation, and diagnostics/status message composition.
- Removed old dead local helper methods left behind by earlier settings modal extraction work.
- Fixed the ranking-diagnostics Elvis warning by removing a no-longer-valid nullable fallback path.

### 2026-04-04 23:24 UTC
- Fixed a build break caused by a missing `SourceLoadUiCoordinator` import in `MainActivity`.
- Re-ran `./gradlew testDebugUnitTest assembleDebug` successfully after the import fix.
- The earlier ranking-diagnostics warning is gone; only the unrelated Android manifest warning remains during build.

---

## Scope Changes

### 2026-04-04
- Initial scope established as: document top refactor candidates + implement one focused extraction + update docs honestly.
- Browse-flow extraction was initially kept out of required scope unless playback extraction stayed small.
- After playback landed cleanly, browse-flow extraction was pulled into the same plan as a controlled follow-up.
- A narrow settings source-preferences cleanup was then pulled in as another controlled follow-up.
- A broader settings-shell coordination pass was then pulled in to centralize summaries and auth/update entry points without yet extracting modal UI composition.
- A details/episodes/sources transition pass was then pulled in to reduce inline navigation/auth decision logic in `MainActivity`.
- A back-stack/navigation policy pass was then pulled in to reduce remaining inline shell policy.
- A partial settings modal-composition pass was then pulled in for source-preferences modal pages.
- An auth/update modal-branching pass was then pulled in to reduce the remaining settings-shell branching.
- A final source-loading UI lifecycle + hygiene sweep was then pulled in to finish the most obvious remaining activity-level sprawl.
- Actual deep source-loading domain orchestration remained in `SourceLoadingCoordinator`; only shell/UI concerns moved.
- No richer navigation stack/history system was introduced in the back-navigation extraction.

---

## Session Start

### 2026-04-04 22:39 UTC
Intended task: Create the plan, document top shell refactor candidates, and extract playback launch workflow from `MainActivity`.

### 2026-04-04 22:47 UTC
Intended task: Continue the same plan by extracting the browse flow (`runSearch` and `onSearchResultSelected`) into a focused coordinator.

### 2026-04-04 22:53 UTC
Intended task: Continue the same plan with a narrow settings cleanup that routes source-preferences behavior through `SourcePreferencesCoordinator`.

### 2026-04-04 22:57 UTC
Intended task: Continue the same plan with a broader settings-shell coordinator for summaries and auth/update entry points.

### 2026-04-04 23:01 UTC
Intended task: Continue the same plan with a details/episodes/sources transition coordinator.

### 2026-04-04 23:11 UTC
Intended task: Continue the same plan with a back-navigation coordinator.

### 2026-04-04 23:13 UTC
Intended task: Continue the same plan with a settings modal coordinator for source-preferences modal composition.

### 2026-04-04 23:17 UTC
Intended task: Continue the same plan with auth/update modal branching cleanup.

### 2026-04-04 23:22 UTC
Intended task: Continue the same plan with source-loading UI lifecycle extraction and a final hygiene sweep.

---

## Definition of Done

This plan is complete for its intended pass when:
- the top refactor candidates are documented in repo docs
- playback launch orchestration has been extracted from `MainActivity` into a focused coordinator
- validation is recorded for the implemented work
- follow-up work is explicitly captured
- `Current Focus` no longer implies unfinished required work
- the file is ready to move out of `in_progress/` without misleading anyone
