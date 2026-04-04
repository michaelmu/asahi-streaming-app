# App Shell Review — 2026-04-04

This note captures the current architectural read on Asahi’s app shell after inspecting the real repo state on 2026-04-04.

It exists so future sessions do not need to rediscover the same conclusions from scratch.

## Overall verdict

The repo is in good shape overall.
The strongest architecture is in the domain/integration/source pipeline layers.
At the time this review was first written, the primary architectural risk was workflow concentration inside `app/src/main/kotlin/ai/shieldtv/app/MainActivity.kt`.

That cleanup pass has now been executed.
See `docs/APP_SHELL_STATUS_2026-04-04.md` for the post-refactor status.

This was **not** a rewrite case.
It was a **continue-peeling-the-shell** case, and that work materially reduced the original risk.

## What is working well

- multi-module split is real and useful (`core`, `domain`, `feature`, `integration`, `app`)
- source/provider pipeline is the strongest subsystem:
  - provider registry
  - normalization
  - ranking
  - dedupe
  - cache marking
  - diagnostics
- signed release APK pipeline already exists
- recent work already moved in the right direction via focused coordinators/helpers such as:
  - `RealDebridAuthCoordinator`
  - `UpdateUiCoordinator`
  - `SourceLoadingCoordinator`
  - `SourcePreferencesCoordinator`
  - `ModalHost`

## Main architectural weakness

`MainActivity` is still the app-shell gravity well.
It currently owns or directly coordinates too many concerns:

- shell layout / fullscreen mode switching
- screen routing and back behavior
- search → details → sources flow glue
- playback launch and persistence flow glue
- modal lifecycle and many modal workflows
- settings action handling
- update flow UI handling
- Real-Debrid auth UI handling
- debug/status formatting

The issue is not that the code is impossible to work with.
The issue is that too many unrelated product changes still converge in one file.

## Top 5 next refactor targets

Ranked by expected payoff:

### 1. Playback launch and playback-session workflow
**Why it ranks first:**
This is one of the cleanest high-value seams in `MainActivity`.
It currently mixes:
- auth gating
- resume position choice
- player preparation
- failure handling
- session persistence
- continue-watching updates
- watch-history recording
- transition into the player destination

**Recommended direction:**
Extract a focused `PlaybackLaunchCoordinator` that returns structured outcomes instead of burying rendering/navigation inside it.

---

### 2. Search/details/sources browse workflow
**Why it matters:**
The activity still owns a lot of browse progression logic:
- search execution
- search result selection
- details loading
- transition to source lookup

**Recommended direction:**
Extract a browse-flow coordinator after playback launch is cleaned up.

---

### 3. Settings modal/action workflow
**Why it matters:**
Provider toggles, size pickers, auth actions, and update actions are still very imperative and activity-centric.
This is manageable now, but likely to keep growing.

**Recommended direction:**
Pull settings actions and modal-page logic into a focused coordinator/presenter layer without changing the current UI model all at once.

---

### 4. Navigation/back-stack transition policy
**Why it matters:**
Back behavior currently lives inline in the activity with destination-specific branching.
That makes regressions more likely as flows multiply.

**Recommended direction:**
Centralize app-destination back/navigation policy once the larger workflow seams are smaller.

---

### 5. Shell state restoration and resume reconciliation
**Why it matters:**
`AppState` is broad, and restore/reconcile behavior is currently pragmatic rather than deeply modeled.
It works, but is a likely future source of edge-case bugs.

**Recommended direction:**
Revisit after the bigger workflow extractions, not before.

## What not to do yet

- do not do a full app-wide state/effect-loop rewrite yet
- do not introduce DI framework churn just to look cleaner
- do not broaden provider scope aggressively at the expense of reliability and diagnostics

## Immediate recommendation

This document should now be treated as the pre-refactor review snapshot.

For the post-pass state and remaining rough edges, use:
- `docs/APP_SHELL_STATUS_2026-04-04.md`
- `docs/exec-plans/complete/2026-04-04-app-shell-flow-extraction.md`
