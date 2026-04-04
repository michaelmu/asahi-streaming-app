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

**Current phase:** Phase A — device/runtime hardening

**Immediate target:** confirm and continue Android/Shield compatibility hardening after the Real-Debrid auth runtime issue, then tackle the remaining `MainActivity` shell cleanup from the previous pass.

**Why this now:**
The previous plan materially improved the app’s internal architecture and source pipeline.
The next highest-value work is now:
- reduce device/runtime surprises on Shield
- tighten app-shell reliability where state and modal orchestration remain too concentrated

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
Status: TODO
Priority: High

### Goal
Finish the highest-value remaining app-shell cleanup without forcing a giant store/effect-loop rewrite.

### Why this matters
`MainActivity` is still the runtime gravity well, even after the previous extraction work.
The biggest remaining maintainability risk is still concentrated there.

### Proposed sub-steps
- [TODO] extract modal presentation helper/state wrapper if it meaningfully reduces repetition
- [TODO] move playback error message formatting out of `MainActivity`
- [TODO] reduce remaining workflow glue where obvious seams already exist
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

---

## Scope Changes

### 2026-04-04
- New plan created because the prior next-pass plan was complete for its intended scope.
- Device/runtime hardening was promoted after a real on-device Shield compatibility failure surfaced during Real-Debrid auth.

---

## Session Start

### 2026-04-04 16:40 UTC
Intended task: Close out the previous plan and create a new in-progress plan focused on device hardening and remaining shell cleanup.

---

## Definition of Done

This plan is complete for its intended pass when:
- accepted items are marked `DONE`, `DEFERRED`, `OPTIONAL`, or removed
- validation is recorded for completed work
- follow-up work is explicitly captured
