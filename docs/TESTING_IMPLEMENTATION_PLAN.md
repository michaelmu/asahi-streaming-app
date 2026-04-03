# Testing Implementation Plan

This document turns `TESTING_APPROACH.md` into a concrete execution plan.

---

## Phase 1 — High-Value Unit Tests

### 1. Resume logic tests

Create tests for the logic that decides whether resume is available.

Target behaviors:
- no resume when progress is too low
- resume allowed in in-progress range
- no resume when nearly finished
- no resume when title does not match
- no resume when season/episode do not match
- resume allowed for strong title/episode match

Suggested extraction if needed:
- move resume matching logic into a small helper/class if it is too embedded in `MainActivity`

---

### 2. AppState serialization tests

Create tests for:
- `toBundleMap()` / restore round-trip
- recent queries round-trip
- continue-watching round-trip
- selected season/episode round-trip
- safe handling of missing fields

Why first:
- restore bugs already happened
- these tests are cheap and valuable

---

### 3. PlaybackSessionStore tests

Create tests for:
- save/load round-trip
- missing file returns null
- malformed content fails safely
- progress/position/duration fields parse correctly
- optional season/episode values survive save/load

Potential note:
- if Android context/file handling gets in the way, wrap file access for easier testing

---

### 4. Player state mapping tests

Create tests for the mapping between Media3 state inputs and user-facing labels.

Target behaviors:
- ready + playing → `playing`
- ready + not playing → `paused`
- buffering → `buffering`
- ended → `ended`
- idle → `idle`

Suggested extraction if needed:
- move label mapping into a small helper so it can be unit tested directly

---

### 5. Source grouping and label tests

Create tests for:
- cached sources grouped under cached bucket
- direct sources grouped under direct bucket
- uncached/unchecked sources grouped under fallback bucket
- source labels (`BEST`, `CACHED`, `DIRECT`, `FALLBACK`)
- quality labels (`4K`, `1080p`, etc.)

Suggested extraction if needed:
- move grouping/label helpers into testable non-UI helpers

---

## Phase 2 — Lightweight Integration Tests

### 6. Restore fallback behavior

Create tests for restore scenarios such as:
- `PLAYER` destination with no selected source falls back safely
- valid details but invalid player state falls back to details/episodes/sources
- empty restored state falls back to home

This may require extracting restore reconciliation logic from `MainActivity`.

---

### 7. Continue-watching hydration tests

Create tests for:
- persisted playback session in valid progress range populates continue-watching
- low-progress sessions do not populate continue-watching
- near-complete sessions do not populate continue-watching

Suggested extraction if needed:
- move hydration logic into a helper or use-case-like class

---

## Phase 3 — Minimal Smoke Tests

### 8. App launch smoke test

Instrumentation test:
- launch app
- verify activity starts without crashing

### 9. Basic navigation smoke test

Instrumentation test:
- launch app
- move into one or two major screens (home → search/results)
- confirm no immediate crash

### 10. Player shell smoke test

Instrumentation test:
- if a lightweight fake/mocked path is possible, verify player screen can be entered without crashing

Important:
- do not overbuild instrumentation at this stage

---

## Recommended Order of Work

## First batch

Implement these first:
- [ ] Resume logic tests
- [ ] AppState serialization tests
- [ ] PlaybackSessionStore tests
- [ ] Player state mapping tests
- [ ] Source grouping/label tests

## Second batch

Implement next:
- [ ] Restore fallback tests
- [ ] Continue-watching hydration tests

## Third batch

Implement later:
- [ ] App launch smoke test
- [ ] Basic navigation smoke test
- [ ] Player shell smoke test

---

## Refactors That May Be Needed First

Some current logic is embedded in UI/activity classes and may be awkward to test directly.

If needed, extract:
- resume matching into a helper
- restore reconciliation into a helper
- continue-watching hydration into a helper
- player state label mapping into a helper
- source grouping/label formatting into helpers

This is acceptable and desirable if it improves testability.

---

## Suggested Deliverables

### Deliverable 1
A first test PR/commit that adds:
- pure unit tests only
- no major architecture change
- minimal helper extraction where needed

### Deliverable 2
A second test PR/commit that adds:
- restore/session integration tests
- small refactors for testability

### Deliverable 3
A small smoke-test PR/commit that adds:
- launch + navigation sanity tests

---

## Success Criteria

The testing effort is successful when:
- resume logic regressions are caught automatically
- restore bugs are harder to reintroduce
- player state label regressions are caught automatically
- source picker classification bugs are caught automatically
- catastrophic app-launch/navigation breakage has at least minimal smoke coverage

---

## Bottom Line

The right implementation sequence is:

1. protect risky logic with unit tests
2. extract small helpers only where necessary for testability
3. add a tiny smoke layer later

This keeps the test suite useful instead of turning it into drag.
