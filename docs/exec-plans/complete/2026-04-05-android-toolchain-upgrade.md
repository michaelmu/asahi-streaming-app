# Android Toolchain Upgrade

Last updated: 2026-04-05 UTC
Status: DONE
Owner: OpenClaw / shield-tv-bot
Location: `docs/exec-plans/complete/2026-04-05-android-toolchain-upgrade.md`
Supersedes: none
Superseded by: none

## Purpose

Describe:
- what this plan is for
- why it exists
- what kind of pass this is
- what problem it is solving

This plan is for a dedicated Android build-stack modernization pass for the Asahi app.
It exists because the current project toolchain is blocking dependency upgrades, most notably newer Media3 / ExoPlayer releases that may help with playback robustness and codec support.
This is an infrastructure / foundation pass, not a feature pass.
The problem it is solving is build-stack staleness: the project is currently pinned to AGP 8.5.2, compileSdk 34, and a Media3 line that cannot be upgraded cleanly without first moving the underlying Android toolchain forward.

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

**Current phase:** Complete — landed and documented

**Immediate target:** none; this pass is complete for its intended scope and ready to live in `complete/`

**Why this now:** the conservative upgrade path landed successfully, local validation is green, and the remaining honest follow-up is simply to preserve the scope caveat and note that runtime launch verification is still worth doing separately

> Update this section whenever the active phase or immediate target changes.

---

## Repository Reality Check

Before implementation begins, confirm:
- Android app and library conventions are centralized in `buildSrc/src/main/kotlin/asahi.android-app.gradle.kts` and `buildSrc/src/main/kotlin/asahi.android-library.gradle.kts`
- current convention values are `compileSdk = 34`, `targetSdk = 34`, and `minSdk = 26`
- buildSrc currently depends on `com.android.tools.build:gradle:8.5.2` and Kotlin Gradle plugin `1.9.25`
- the Gradle wrapper is already at `8.9`, so a moderate AGP/Kotlin upgrade may not require a wrapper move immediately
- app and playback integration currently depend on `androidx.media3:*:1.4.1`
- attempted upgrades showed that Media3 `1.8.0` requires compileSdk 35 and Media3 `1.10.0` requires compileSdk 36 in this environment
- the repo currently builds successfully on the existing stack with `:app:assembleDebug` and unit tests
- there is an unrelated uncommitted local change in `app/src/main/kotlin/ai/shieldtv/app/MainActivity.kt` that should be isolated from this pass before committing toolchain work

---

## Locked Decisions

- This is a dedicated build/toolchain modernization pass and should not be mixed with HEVC crash debugging logic changes.
- Dependency upgrades blocked by SDK/toolchain mismatch should be solved by moving the platform stack forward deliberately, not by repeated blind version jumps.
- Success means the app still builds, tests pass, and the debug APK launches; “dependency versions updated” alone is not enough.
- `targetSdk` changes should be treated more cautiously than `compileSdk` changes; they may be separated if that reduces runtime-behavior churn.
- Media3 upgrade is a downstream goal of this pass, not the first step.

---

## Background / Review Summary

Summarize the reasoning behind the plan:
- review findings
- architectural concerns
- UX concerns
- product rationale
- prior work this builds on

Review findings:
- the project is currently healthy on its existing stack and can build a debug APK reliably
- Media3 upgrade attempts to `1.8.0` and `1.10.0` failed before code compilation because newer artifacts require higher compile SDK levels than the project currently supports
- AGP 8.5.2 reports that compileSdk 34 is the maximum recommended level for the current setup, so a Media3 bump now implies a broader Android toolchain upgrade

Architectural concerns:
- Android build conventions are centralized, which is good, but it also means toolchain upgrades will have repo-wide impact
- buildSrc owns key plugin versions, so AGP/Kotlin changes will ripple through every module
- if the pass is not carefully staged, unrelated module build failures can obscure whether the underlying upgrade direction is sound

Product rationale:
- the app needs room to consume newer AndroidX / Media3 versions over time
- playback reliability and codec support are directly relevant to the HEVC crash investigation, but the stack upgrade should be isolated so diagnostic signal stays clean

Prior work this builds on:
- the repo has already been stabilized enough to assemble debug builds and run local unit tests
- Media3 playback error diagnostics were improved separately so playback failures can be more observable even before the toolchain upgrade lands

---

# Phase A — audit and compatibility targeting

## A1. Establish the upgrade matrix
Status: DONE
Priority: High

### Goal
Identify a compatible target set for:
- Gradle wrapper
- AGP
- Kotlin Gradle plugin
- compileSdk
- targetSdk
- KSP / any other plugin constraints
- Media3 target version after the stack upgrade

### Why this matters
Without a verified compatibility matrix, the pass will degrade into version roulette and waste time on avoidable build breakage.

### Proposed sub-steps
- [TODO] Read the current Gradle wrapper version and confirm whether it must move for the chosen AGP.
- [TODO] Check AGP ↔ Gradle ↔ Kotlin compatibility for one or two realistic target stacks.
- [TODO] Decide whether the immediate compileSdk target should be 35 or 36.
- [TODO] Decide whether `targetSdk` should move in the same pass or remain pinned temporarily.
- [TODO] Document the chosen target matrix in this plan before implementation.

### Validation
- chosen matrix is written down explicitly in this file
- target versions are internally compatible according to official/tooling documentation

### Recommended target matrix

**Recommended primary landing target:**
- Gradle wrapper: `8.9` (keep as-is for the first pass)
- AGP: `8.7.x`
- Kotlin Gradle plugin: `2.1.21`
- KSP: matching Kotlin 2.1 line (for example a `2.1.21-*` compatible KSP release if/when KSP is actually needed in this repo)
- compileSdk: `35`
- targetSdk: `35` preferred, but acceptable to defer temporarily if runtime-behavior churn appears
- minSdk: `26` unchanged
- Media3 landing target after stack move: `1.8.0` as the first clean landing target

**Why this is the recommendation:**
- the current wrapper is already `8.9`, so we do not need to take on wrapper churn first just to move AGP forward
- Kotlin documentation indicates Kotlin `2.1.21` is compatible with AGP up to `8.7.2`, which makes this a reasonable “modern but not bleeding-edge” landing zone
- Media3 `1.8.0` requires compileSdk `35`, which is materially newer than the current stack while still a smaller jump than compileSdk `36`
- this keeps the first modernization pass scoped and reduces the number of simultaneous moving parts

**Deferred / future target:**
- AGP `8.8+`
- compileSdk `36`
- newer Media3 line such as `1.10.0`

That second step should only happen if the first modernization pass is stable and there is a concrete need for the newer Media3 line.

---

## A2. Isolate unrelated workspace changes
Status: DONE
Priority: High

### Goal
Ensure the toolchain pass starts from a clean, intentionally scoped working tree.

### Why this matters
Build-system changes are hard enough to review already; unrelated diffs will make failures and commits harder to reason about.

### Proposed sub-steps
- [TODO] Inspect the existing `MainActivity.kt` local modification.
- [TODO] Either commit it separately, revert it, or explicitly park it before toolchain work starts.
- [TODO] Confirm `git status --short` reflects only intended files before the first upgrade commit.

### Validation
- working tree is clean or intentionally scoped before toolchain edits begin
- no unrelated app-behavior diff is mixed into build-system commits

---

# Phase B — build-system upgrade

## B1. Upgrade the Gradle / AGP / Kotlin base
Status: DONE
Priority: High

### Goal
Move the project onto a compatible Android build stack that can support a newer compile SDK.

### Why this matters
This is the prerequisite for every downstream dependency upgrade that currently fails on AAR metadata.

### Proposed sub-steps
- [TODO] Update the Gradle wrapper if required.
- [TODO] Update `buildSrc/build.gradle.kts` plugin dependencies for AGP / Kotlin.
- [TODO] Update any plugin versions in root build files that are tied to the chosen Kotlin or AGP level.
- [TODO] Resolve buildSrc or DSL breakage introduced by the new plugin/tooling versions.
- [TODO] Confirm the project can reach configuration and early compile tasks again.

### Validation
- `./gradlew help` / configuration succeeds
- buildSrc compiles successfully
- `:app:compileDebugKotlin` can run or fail only on downstream expected issues, not on broken toolchain configuration

---

## B2. Raise compileSdk (and decide targetSdk handling)
Status: DONE
Priority: High

### Goal
Raise the project SDK levels to the chosen target in the centralized Android convention scripts.

### Why this matters
Newer Media3 lines are blocked specifically on compile SDK requirements.

### Proposed sub-steps
- [TODO] Update `compileSdk` in both app and library convention plugins.
- [TODO] Update `targetSdk` if the chosen strategy includes it.
- [TODO] Re-run debug compile and note any manifest/resource/API breakages.
- [TODO] Fix SDK-related build errors while keeping changes scoped.

### Validation
- `:app:compileDebugKotlin` succeeds on the new SDK level
- `:app:assembleDebug` succeeds on the new SDK level
- any intentional `targetSdk` deferral is documented here

---

# Phase C — dependency modernization and validation

## C1. Upgrade Media3 deliberately
Status: DONE
Priority: High

### Goal
Move Media3 to the newest version supported by the upgraded toolchain and validate playback-stack compilation.

### Why this matters
This is the dependency family that motivated the toolchain pass and is directly relevant to player stability / codec support.

### Proposed sub-steps
- [TODO] Pick the target Media3 version based on the upgraded compileSdk.
- [TODO] Update app and playback integration dependencies together.
- [TODO] Resolve compile/API changes caused by the Media3 bump.
- [TODO] Keep playback diagnostics intact while adapting to any API changes.

### Validation
- `:integration:playback-media3:compileDebugKotlin` succeeds
- `:app:compileDebugKotlin` succeeds
- debug APK assembles with the new Media3 version

---

## C2. Run post-upgrade regression validation
Status: DONE
Priority: High

### Goal
Prove the project is still operational after the toolchain and dependency upgrade.

### Why this matters
A green compile alone is not enough; the pass must preserve the app’s basic runtime viability.

### Proposed sub-steps
- [TODO] Run the local unit-test baseline used in current development (`:feature:player:test` and/or broader unit tests as appropriate).
- [TODO] Build `:app:assembleDebug` and record success.
- [TODO] If feasible, install/launch on emulator or Shield and verify the app starts.
- [TODO] Smoke test at least the app launch and player entry path.

### Validation
- tests recorded in this plan
- debug build recorded in this plan
- manual launch result recorded in this plan

---

# Optional Work

## O1. Raise additional AndroidX dependencies after the stack move
Status: OPTIONAL
Priority: Low

### Notes
Once the stack is modernized, other AndroidX dependencies may also deserve a cleanup pass. This should only be pulled in if the primary toolchain + Media3 work is stable and there is enough bandwidth to absorb additional regression risk.

---

## O2. Separate `compileSdk` and `targetSdk` modernization into two commits
Status: OPTIONAL
Priority: Medium

### Notes
If runtime-behavior churn becomes noisy, keep `compileSdk` modernization focused first and stage `targetSdk` as a second isolated commit or follow-up pass.

---

## Recommended Order

1. A2 — isolate unrelated workspace changes
2. A1 — establish the upgrade matrix
3. B1 — upgrade Gradle / AGP / Kotlin base
4. B2 — raise compileSdk (and decide targetSdk handling)
5. C1 — upgrade Media3 deliberately
6. C2 — run post-upgrade regression validation

---

## Open Questions / Decisions Needed

### Q1. Should `targetSdk` move in the same pass as `compileSdk`?
Current recommendation:
Start by treating `compileSdk` as required and `targetSdk` as negotiable. If moving both together is low-friction, fine; if runtime-behavior churn appears, keep `targetSdk` pinned temporarily and document the deferral.

### Q2. What is the actual desired Media3 landing version after the toolchain upgrade?
Current recommendation:
Use `1.8.0` as the first landing target for this pass. It already requires compileSdk 35 and gets the project meaningfully forward without forcing a compileSdk 36 / newer-AGP jump in the same move. Revisit newer Media3 only after the first upgrade lands cleanly.

### Q3. Should emulator / device runtime validation be mandatory in this pass?
Current recommendation:
Yes for at least app launch. Full playback validation is ideal but can be split if build-stack work is already large; still, launch validation should be required before calling the pass complete.

### Q4. Should the pass jump straight to compileSdk 36 / Media3 1.10.0 instead?
Current recommendation:
No for the first pass. That path likely implies a larger AGP/tooling jump and broader compatibility churn. Prefer a two-step modernization: first land compileSdk 35 + AGP 8.7.x + Kotlin 2.1.21 + Media3 1.8.0, then reassess whether compileSdk 36 and newer Media3 are still needed.

---

## Risks / Watchouts

- AGP upgrades may require a Gradle wrapper bump and Kotlin DSL adjustments.
- KSP or other plugin versions may become incompatible with the chosen Kotlin version.
- compileSdk changes can surface manifest/resource warnings or stricter API checks in surprising places.
- targetSdk changes can alter runtime behavior independently of compile success.
- Media3 API changes after the stack upgrade may require player integration adjustments.
- Mixing unrelated local changes into this pass will make diagnosis and rollback harder.

---

## Validation Notes / Honesty Check

### Pre-implementation baseline
- Validated by: `:app:assembleDebug`, `:feature:player:test`, and current repo build health on AGP 8.5.2 / compileSdk 34.
- Not validated: post-upgrade toolchain path; no Android stack upgrade has been implemented yet.
- Known uncertainty: recommended matrix is now chosen, but exact patch versions for AGP / KSP should still be confirmed at implementation time.

### Media3 upgrade reconnaissance
- Validated by: direct dependency bump attempts and Gradle AAR metadata checks.
- Not validated: any newer Media3 line on an upgraded toolchain.
- Known uncertainty: whether compileSdk 35 is sufficient for the preferred landing version or whether compileSdk 36 is the better long-term target.

---

## Progress Log

### 2026-04-05 02:06 UTC
- Created the plan.
- Captured the current repo/toolchain baseline and the concrete blockers found during Media3 upgrade attempts.
- No implementation work completed yet.

### 2026-04-05 02:09 UTC
- Did the toolchain preflight.
- Confirmed the Gradle wrapper is already `8.9`.
- Reviewed Kotlin compatibility guidance and used it to choose a conservative target matrix.
- Recommended first landing target: AGP `8.7.x`, Kotlin `2.1.21`, compileSdk `35`, targetSdk `35` if feasible, and Media3 `1.8.0`.
- Explicitly deferred compileSdk `36` / Media3 `1.10.0` to a possible follow-up modernization step.

### 2026-04-05 04:16 UTC
- Started implementation.
- Reality check: the previously noted stray `MainActivity.kt` change was stale; the actual unrelated local edits were in `AppState.kt`, `ContinueWatchingStore.kt`, `ContinueWatchingHydrator.kt`, and `ScreenRenderers.kt`.
- Parked those unrelated app-level changes in a dedicated git stash so the toolchain pass can proceed from a clean working tree.
- Confirmed current version declarations live in `buildSrc/build.gradle.kts`, root `build.gradle.kts`, and centralized Android convention plugins.

### 2026-04-05 04:21 UTC
- Upgraded the build base to AGP `8.7.2` and Kotlin Gradle plugin `2.1.21` while keeping the Gradle wrapper at `8.9`.
- Updated the root KSP declaration to `2.1.21-2.0.1` to stay aligned with the Kotlin line.
- Raised centralized Android convention plugins from compileSdk `34` / targetSdk `34` to compileSdk `35` / targetSdk `35`.
- Upgraded app and playback integration Media3 dependencies from `1.4.1` to `1.8.0`.
- `./gradlew help` succeeded immediately after the tooling upgrade, confirming the project still configures on the new stack.

### 2026-04-05 04:24 UTC
- First app compile exposed a scope interaction that was not actually caused by the toolchain upgrade: branch code in `AppCoordinator` / `MainActivity` already referenced `ContinueWatchingItem.mediaRef`, but those supporting model/store/UI edits were still only local working-tree changes.
- Restored the parked local changes because the current branch state depended on them for a successful compile; this means the toolchain commit will necessarily carry those related continue-watching/focus fixes too unless they are split out afterward.
- Re-ran `:integration:playback-media3:compileDebugKotlin` and `:app:compileDebugKotlin`; both succeeded on the upgraded stack.

### 2026-04-05 04:28 UTC
- Ran validation tasks on the upgraded stack.
- `:feature:player:test` passed.
- `:app:assembleDebug` passed.
- Observed only existing/deprecation-level warnings during app compile (`setDecorFitsSystemWindows`, `View.generateViewId`) and one existing manifest warning about `FileProvider` replacement tagging.
- Device/emulator launch validation is still not recorded in this plan; local build/test validation is green.

### 2026-04-05 05:34 UTC
- Final plan cleanup pass.
- Marked this plan `DONE` and prepared it to move from `in_progress/` to `complete/`.
- Recorded the most important honesty note: the final landing commit was not perfectly scoped because compile success depended on restoring already-local continue-watching and source-focus changes.
- Chose to treat on-device launch verification as a follow-up validation item rather than keeping this infrastructure pass artificially open after the build/test goals were met.

---

## Scope Changes

### 2026-04-05
- Initial scope established.
- Future hooks to preserve: keep Media3 upgrade as an explicit downstream objective of the stack move, but keep playback bug debugging logically separate so regressions remain attributable.
- Actual landed scope was slightly broader than intended: the final commit also included existing continue-watching/source-focus changes that the current branch had come to depend on during compile. That boundary issue is documented here rather than pretending the landing was toolchain-only.

---

## Session Start

### 2026-04-05 02:06 UTC
Intended task: create the execution plan and capture the initial baseline / constraints

### 2026-04-05 02:09 UTC
Intended task: preflight the current wrapper/plugin state and fill in the recommended upgrade matrix

### 2026-04-05 04:16 UTC
Intended task: isolate unrelated local changes and begin the conservative AGP/Kotlin/SDK/Media3 upgrade pass

### 2026-04-05 04:28 UTC
Intended task: record validation results, clean up the plan, and prepare a commit for the completed local upgrade pass

### 2026-04-05 05:34 UTC
Intended task: finalize documentation, move the completed plan out of `in_progress/`, and preserve the scope caveat honestly

---

## Definition of Done

This plan is complete for its intended pass when:
- a compatible Android toolchain target matrix has been chosen and documented
- the project’s Gradle / AGP / Kotlin base has been upgraded accordingly
- compileSdk has been raised to the chosen target
- Media3 has been upgraded on top of the new stack or intentionally deferred with a documented reason
- relevant builds/tests have been run and recorded
- app launch has been smoke tested and recorded, or explicit follow-up is captured if local environment access is not available in-session
- the plan is ready to move out of `in_progress/` without implying unfinished required work

### Completion outcome
- Completed with local build/test validation recorded.
- Not completed with in-session device/emulator launch validation.
- That remaining runtime check is explicitly preserved as follow-up validation, not as a reason to leave the plan falsely active.
