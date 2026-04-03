# Testing Approach

## Purpose

Asahi is moving from prototype behavior into a more stateful TV application.
That means regressions are becoming more expensive, especially around:

- playback session continuity
- resume behavior
- state restoration
- source picker logic
- player state labeling
- navigation between major screens

This document describes the overall testing strategy.

---

## Testing Philosophy

The goal is not to build a giant fragile UI test suite.

Instead, the best approach is:

1. **unit test the stateful logic that breaks easily**
2. **smoke test the highest-value app flows**
3. **avoid expensive visual/snapshot testing until the UI stabilizes more**

In other words:
- protect important logic first
- keep the test suite fast
- avoid tests that become maintenance overhead faster than they provide value

---

## What to Prioritize

## 1. Pure logic and state tests

These are the best return on effort.

Target areas:
- resume eligibility logic
- playback state label mapping
- source grouping/label mapping
- AppState serialization/restoration
- playback session store round-trips

Why these are valuable:
- they are easy to write
- they run quickly
- they catch bugs that are hard to spot visually
- they protect flows that are already known to be fragile

---

## 2. Persistence and restore tests

Asahi already had restore/rotation issues.
That means persistence behavior deserves dedicated tests.

Target areas:
- bundle serialization
- playback session persistence
- malformed/partial persisted state
- fallback behavior when restored state is incomplete

Why this matters:
- restore bugs are common and frustrating
- users experience them as random breakage
- these tests are still relatively cheap

---

## 3. Small smoke/instrumentation tests

Instrumentation should be used sparingly and strategically.

Good smoke tests:
- app launches without crashing
- home screen renders
- navigating into search/results/details/player shell does not immediately fail

Why keep these small:
- Android UI tests are slower and more brittle
- at this stage they should only protect catastrophic flow breakage

---

## What Not to Prioritize Yet

### Full screenshot/golden tests
Not yet.
The UI is still changing too quickly.

### Deep playback instrumentation
Not yet.
Exo/Media3 playback tests can get expensive and flaky quickly.

### Giant end-to-end emulator suites
Not yet.
They cost a lot and often fail for reasons unrelated to real app regressions.

---

## Recommended Test Layers

## Layer 1 — Unit tests (highest priority)

Write unit tests for:
- resume matching logic
- progress threshold logic
- player state label mapping
- source grouping / source badge logic
- `AppState` serialization round-trips
- playback session store save/load behavior

These should become the main regression shield.

---

## Layer 2 — Lightweight integration tests

Write small integration-style tests for:
- session restore fallback behavior
- continue-watching hydration behavior
- player state transitions at the presenter/use-case layer where possible

These are more useful than UI tests when the bug source is application logic.

---

## Layer 3 — Smoke instrumentation tests

Keep this small.

Suggested smoke coverage:
- app launches
- home screen appears
- navigation into one or two critical screens does not crash

Good rule:
- prefer 2–5 trustworthy smoke tests over 25 flaky ones

---

## Highest-Risk Areas to Protect

### Playback / resume
- persisted session loading
- resume prompt eligibility
- title/episode matching
- start-over vs resume behavior

### Player state mapping
- `playing`
- `paused`
- `buffering`
- `ended`
- `idle`

### App restoration
- player destination restore
- fallback from invalid restore state
- selected media/details/sources continuity

### Source picker logic
- cached/direct/fallback grouping
- ranking labels
- quality label formatting

---

## Recommended Initial Test Set

If we only add a first useful wave, it should be:

1. resume eligibility tests
2. playback session store tests
3. `AppState` bundle round-trip tests
4. player state label mapping tests
5. source grouping/label tests

This would immediately improve confidence without creating a giant test burden.

---

## Suggested Tooling Direction

### Unit tests
- JUnit
- existing Kotlin/JVM test setup if available
- keep logic testable without Android framework where possible

### Android-dependent persistence tests
- use Robolectric only if needed
- otherwise isolate file/persistence code enough for local JVM tests

### Instrumentation tests
- AndroidX test / Espresso only for a very small smoke layer

---

## Test Design Principles

### Keep logic extractable
If logic is hard to test, move it into small pure functions or helper classes.

### Prefer deterministic tests
Avoid tests that depend on timing, network, or real playback whenever possible.

### Name tests around behavior
Examples:
- `resume_not_offered_for_near_completed_video`
- `restore_falls_back_to_sources_when_player_state_is_invalid`
- `cached_4k_source_is_labeled_best`

### Use smoke tests only where they add confidence
Do not use UI tests to compensate for untestable business logic.

---

## Bottom Line

The best testing strategy for Asahi right now is:

- **unit test the risky logic**
- **integration test restore/session behavior selectively**
- **use only a tiny number of smoke tests**

That gives us the best balance of:
- confidence
- speed
- maintainability
- practical value during rapid iteration
