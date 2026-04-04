# Asahi Execution Plan — Device Hardening and Shell Cleanup

Last updated: 2026-04-04 UTC
Status: IN_PROGRESS
Owner: shield-tv-bot + Mike
Location: `docs/exec-plans/in_progress/2026-04-04-device-hardening-and-shell-cleanup.md`
Supersedes: `docs/exec-plans/complete/2026-04-04-next-pass-ranking-orchestration.md`

## Purpose

This is the next living execution plan for Asahi after the ranking/orchestration pass was completed.

This pass is focused on two practical follow-ups:
- **device hardening** for real Android TV / NVIDIA Shield behavior
- **remaining shell cleanup** where `MainActivity` and adjacent app-shell logic are still doing more than they should

This is intentionally not an architecture vanity pass.
It is meant to improve real reliability on device while continuing to reduce app-shell fragility.

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
- scope changes when priorities shift
- validation notes after completed work

### Completion rule
A task is only `DONE` when:
- the code landed
- relevant validation happened and was recorded
- follow-up work is captured if needed

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

**Current phase:** Phase B — remaining shell cleanup

**Immediate target:** continue breaking up `MainActivity` by extracting the source/settings preferences workflow into a focused coordinator.

**Why this now:**
The previous plan materially improved the app’s internal architecture and source pipeline.
With the latest Shield/UI work landed, the best proactive next step is to keep shrinking app-shell glue in `MainActivity` without forcing a huge architectural rewrite.

---

## Background / Review Summary

The prior pass delivered:
- auth/update coordinator extraction
- composable source ranking
- ranking explanations
- provenance-aware dedupe
- incremental source loading UX
- provider health diagnostics
- clarified active resume persistence
- selective typed errors

A real on-device issue then surfaced on NVIDIA Shield:
- `URLEncoder.encode(..., Charset)` caused Real-Debrid device-flow failure at runtime

That led to a targeted Android-compatibility sweep and fixes for:
- Android-safe URL encoding overloads
- Android-safe URL decoding overloads

This new plan exists because the next work is no longer “ranking/orchestration.”
It is now “device hardening + remaining shell cleanup.”

---

# Phase A — Device/runtime hardening

## A1. Android/Shield compatibility hardening
Status: TODO
Priority: High

### Goal
Catch and eliminate device/runtime compatibility issues before they become user-facing failures on Shield.

### Why this matters
A single JVM-vs-Android incompatibility already broke Real-Debrid auth on device.
This class of problem is costly because it often looks like API failure when it is actually runtime incompatibility.

### Proposed sub-steps
- [TODO] record completed compatibility fixes in this plan for auditability
- [TODO] continue targeted sweeps for runtime-sensitive APIs when device issues appear
- [TODO] add/retain small compatibility-safe helpers where repeated platform-sensitive behavior exists
- [TODO] improve error/debug output so on-device failures are easier to classify

### Validation
- Shield/device runtime issues become easier to diagnose quickly
- no regressions in debug/unit build validation

---

## A2. Live provider/runtime reliability follow-up
Status: TODO
Priority: Medium

### Goal
Reduce confusion caused by provider/network flakiness vs actual code regressions.

### Why this matters
At least one live provider test already showed external instability.
That is normal for scraper/provider ecosystems, but it should be made easier to distinguish from app regressions.

### Proposed sub-steps
- [TODO] review current live test flake points
- [TODO] improve diagnostics or guarding where failures are clearly environmental
- [TODO] avoid letting flaky live tests masquerade as product regressions

### Validation
- provider/runtime failures are easier to classify as environmental vs code issues

---

# Phase B — Remaining shell cleanup

## B1. Continue breaking up `MainActivity`
Status: IN_PROGRESS
Priority: High

### Goal
Finish the highest-value remaining app-shell cleanup without forcing a giant store/effect-loop rewrite.

### Why this matters
`MainActivity` is still the runtime gravity well, even after the previous extraction work.
The biggest remaining maintainability risk is still concentrated there.

### Proposed sub-steps
- [DONE] extract modal presentation helper/state wrapper if it meaningfully reduces repetition
- [DONE] move playback error message formatting out of `MainActivity`
- [IN_PROGRESS] reduce remaining workflow glue where obvious seams already exist
- [TODO] leave `MainActivity` primarily as shell host + renderer + event wiring

### Validation
- no focus/back-stack regressions
- no modal regressions
- behavior remains stable through existing flows

---

## B2. Reassess whether an app-level state/effect loop is justified now
Status: DEFERRED
Priority: Medium

### Notes
This remains a plausible long-term direction.
Do not do it automatically.
Only pull it in if the shell cleanup exposes a genuinely clean/store-worthy boundary.

---

# Phase C — Optional follow-up work

## C1. Broader DI rethink
Status: OPTIONAL
Priority: Low

### Notes
Still optional.
Only worth doing if testing/component wiring pain justifies the churn.

## C2. Ranking strategy profiles
Status: OPTIONAL
Priority: Medium

### Notes
Now technically feasible thanks to ranking rules/explanations.
Still not urgent unless there is a clear product reason to expose profiles.

---

## Recommended Order

1. A1 Android/Shield compatibility hardening
2. A2 Live provider/runtime reliability follow-up
3. B1 Continue breaking up `MainActivity`
4. Reassess whether B2 or optional C-items are justified after that

---

## Open Questions / Decisions Needed

### Q1. Should we proactively replace every maybe-suspicious Java API, or continue targeted hardening only when there is concrete evidence?
Current recommendation:
Prefer targeted hardening with focused sweeps when a concrete device/runtime symptom appears.
Avoid speculative churn unless a pattern is clearly risky.

### Q2. Should flaky live provider tests be moved further out of routine validation paths?
Current recommendation:
Maybe, but only after understanding which failures are genuinely environmental vs indicating parser/runtime instability.

### Q3. Is the remaining `MainActivity` cleanup worth a full app-store/effect-loop pivot?
Current recommendation:
Not yet. Finish the smaller shell extractions first and reassess.

---

## Risks / Watchouts

- overreacting to one device/runtime bug can create unnecessary churn
- flaky live provider behavior can waste time if treated like deterministic failures
- shell cleanup can still break TV-specific focus/modal behavior in subtle ways
- optional architecture work can creep back in unless deliberately contained

---

## Progress Log

### 2026-04-04 16:40 UTC
- Created a fresh follow-up plan after the ranking/orchestration pass reached a clean stopping point.
- Scoped the next work around device/runtime hardening and remaining shell cleanup.
- No implementation work has been completed under this plan yet.

### 2026-04-04 18:24 UTC
- Resumed the plan from the shell-cleanup side instead of forcing more speculative device hardening work.
- Completed a small `MainActivity` cleanup slice by moving playback error/status formatting into `PlaybackStatusFormatter`.
- Validation: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` passed.

### 2026-04-04 18:27 UTC
- Completed the next `MainActivity` cleanup slice by extracting modal attach/detach lifecycle handling into `ModalHost`.
- `MainActivity` no longer directly owns the overlay-host add/remove bookkeeping for active modal views.
- Kept behavior intentionally unchanged while reducing activity-level UI plumbing.
- Validation: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` passed.

### 2026-04-04 18:30 UTC
- Continued reducing workflow glue in `MainActivity` by extracting repeated info-modal presentation behavior into `InfoModalPresenter` + `InfoModalSpec`.
- `MainActivity` still chooses modal semantics, but no longer hand-assembles the overlay popup workflow path itself.
- Kept behavior stable while shrinking activity-level orchestration noise.
- Validation: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` passed.

### 2026-04-04 18:34 UTC
- Continued the shell cleanup by extracting source/settings preference mutation and label-building logic into `SourcePreferencesCoordinator`.
- `MainActivity` now delegates provider toggling, bulk provider state changes, reset behavior, and source-preference label building instead of owning that preference mutation logic directly.
- Kept modal/view construction in `MainActivity` for now to keep the extraction low-risk and incremental.
- Validation: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` passed.

---

## Scope Changes

### 2026-04-04
- New plan created because the prior next-pass plan was complete for its intended scope.
- Device/runtime hardening was promoted after a real on-device Shield compatibility failure surfaced during Real-Debrid auth.
- Later resumed the plan via shell cleanup, since recent Shield/UI work reduced immediate uncertainty and made `MainActivity` cleanup the cleaner next move.
- Shell cleanup is currently being approached in small low-risk slices rather than a broad rewrite.
- Modal workflow glue is being peeled away incrementally so behavior stays stable while `MainActivity` gets thinner.
- Settings/source-preferences workflow is now also being peeled away incrementally, starting with mutation/summary logic before view-layer extraction.

---

## Session Start

### 2026-04-04 18:33 UTC
Intended task: Continue the active device/shell plan by extracting the source/settings preferences workflow from `MainActivity`.

---

## Definition of Done

This plan is complete for its intended pass when:
- accepted items are marked `DONE`, `DEFERRED`, `OPTIONAL`, or removed
- validation is recorded for completed work
- follow-up work is explicitly captured
