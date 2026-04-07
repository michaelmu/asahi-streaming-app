# Decision Doc — Android Auto Direction for Asahi

Last updated: 2026-04-07 UTC
Status: ACTIVE_REFERENCE
Owner: shield-tv-bot + Mike
Location: `docs/exec-plans/in_progress/2026-04-07-android-auto-decision-doc.md`
Drives: `docs/exec-plans/in_progress/2026-04-07-android-auto-phase-1-implementation.md`

## Purpose

Turn the Android Auto architecture spike into a concrete set of product and technical decisions so implementation can start without drifting into “port the TV app to the car.”

This document answers the open questions from:
- `docs/exec-plans/in_progress/2026-04-07-android-auto-architecture-spike.md`

---

## Executive summary

**Decision:** Build an **Asahi Auto companion** focused on safe, low-friction playback actions, not a full browse/source-management interface.

The first implementation should be a narrow **media-app MVP** that prioritizes:
- Resume
- Favorites
- Search
- Continue Watching
- Simple movie playback
- Simple show playback via deterministic default rules

It should explicitly avoid:
- source picking
- advanced provider controls
- auth/setup in-car
- TV-style deep browsing
- any flow that depends on users visually comparing lots of options

---

## Final decisions

## 1. Should Auto support movies only first, then TV shows?

**Decision: No. Support both movies and TV shows in MVP, but with heavily constrained TV behavior.**

### Why
Movies alone would simplify playback, but it would undercut a major real-world usage pattern for Asahi: resuming or continuing shows. Android Auto is actually a strong fit for:
- “resume my show”
- “play next episode”
- “continue what I started”

### Constraint
Do **not** support full TV season/episode exploration in MVP.
TV support in MVP should be limited to:
- Resume current episode
- Play next episode
- Play latest episode
- Search result → deterministic show action

### Implementation effect
MVP root can include:
- Continue Watching
- Favorites
- Movies
- TV Shows
- Recent
- Search

But TV show nodes should resolve into action-oriented playback, not deep browsing trees.

---

## 2. Is uncached playback acceptable in-car?

**Decision: No for MVP. Auto should be cached/direct only by default.**

### Why
In-car playback should be:
- fast
- predictable
- low-friction
- low-failure

Uncached flows are more likely to:
- take longer
- fail more often
- require user choice/inspection
- create awkward edge cases better handled on TV/phone

### Rule
For Auto MVP, source selection order should be:
1. cached
2. direct
3. fail

Do **not** auto-fall through to messy uncached options in MVP.

### User-facing result
If nothing cached/direct is available, return a concise failure state like:
- “No ready-to-play source found”
- “Try again later or use the TV app for more options”

### Implementation effect
`AutoSourceSelector` should be stricter than the TV source picker.
This is intentional.

---

## 3. Should show-level selection default to resume, next episode, or latest episode?

**Decision: Use this deterministic priority order:**
1. **Resume in-progress episode**
2. **Next unwatched episode**
3. **Latest episode**
4. **Fallback to S01E01 only if no better state exists**

### Why
This best matches likely user intent in the car.
If someone taps a show in Auto, they usually want continuation, not manual browsing.

### Notes
- “Resume” should use existing playback memory / continue-watching state if possible
- “Next unwatched” should use watch history and episode metadata
- “Latest” is a good backup for current shows
- `S01E01` is only a last resort

### Implementation effect
Create a dedicated resolver, e.g.:
- `AutoShowProgressResolver`

Possible API:

```kotlin
interface AutoShowProgressResolver {
    suspend fun resolveDefaultEpisode(show: MediaRef): EpisodeTarget?
}
```

---

## 4. Do we want parked-only richer browse flow later?

**Decision: Maybe later, but explicitly not in MVP.**

### Why
A parked-only mode could eventually justify:
- richer show exploration
- more metadata
- maybe limited episode lists

But introducing that now increases:
- implementation complexity
- product ambiguity
- testing surface
- compliance risk

### Recommendation
Keep the future door open conceptually, but don’t architect MVP around it.
Design MVP so richer parked-only capabilities can be layered in later without changing the core playback abstractions.

### Implementation effect
Do not create a parked/passenger branch yet.
Just avoid making the MVP abstractions too rigid.

---

## 5. Should Auto support remain inside the main app module or move quickly to `feature/auto`?

**Decision: Start inside the main app under a strict package boundary, then graduate to `feature/auto` if/when the surface becomes substantial.**

### Why
Near-term speed matters more than perfect module purity.
A full new module too early adds overhead before APIs are stable.

### Recommendation
Start with:
- `app/src/main/kotlin/ai/shieldtv/app/auto/...`

But keep the boundaries disciplined so extraction later is easy.

### Rule
Auto code should depend on shared domain/integration layers, not on TV renderers or MainActivity-specific UI state.

### Graduation trigger
Move to `feature/auto` once any of these happen:
- multiple Auto-specific packages/services grow substantially
- Auto-specific tests/config become nontrivial
- Auto code starts needing its own build/dependency policy

---

## Product definition for MVP

## User value statement

“Asahi Auto lets users safely resume and start playback from favorites, recents, and search without dealing with source selection or TV-style browsing.”

## MVP capabilities
- Continue Watching
- Favorites
- Recent
- Search
- Play movie
- Resume or continue TV show
- Cached/direct-only playback selection
- concise failure states

## Non-goals
- Full parity with TV app
- provider management
- deep settings
- auth linking
- source comparison
- episode-by-episode long browsing flows

---

## Technical decisions

## A. Auto source policy is separate from TV source policy

**Decision:** Create a dedicated `AutoSourceSelector` instead of reusing the full TV ranking/picker behavior directly.

### Why
TV and Auto have different UX requirements.
TV can tolerate inspection and manual overrides. Auto cannot.

### Policy
- prefer cached
- then direct
- reject uncached in MVP
- fail cleanly if no safe option exists

---

## B. Auto playback should go through a dedicated facade

**Decision:** Introduce `AutoPlaybackFacade` rather than calling the current playback launch flow directly from Auto integration code.

### Why
This keeps Auto-specific rules centralized:
- source policy
- error messaging
- show default episode rules
- no-source fallback behavior

### Suggested responsibilities
- resolve media target
- fetch/select source
- launch playback
- publish concise success/failure result

---

## C. Auto browse should be a read-optimized projection over existing stores

**Decision:** Add `AutoBrowseRepository` that projects existing app data into a car-safe browse tree.

### Sources it should use
- favorites store
- history store
- continue watching store
- search pipeline
- metadata/details lookup when needed

### Why
This avoids coupling Auto tree generation to TV UI state.

---

## D. Setup failures should route users out of car flows

**Decision:** If Real-Debrid or another prerequisite is missing, Auto should fail fast with a short message and not attempt setup in-car.

### Example outcomes
- “Finish account setup on your TV or phone”
- “No ready-to-play source found”

### Why
In-car setup is the wrong environment for that complexity.

---

## Recommended implementation order

## Phase 1 — Core decision-aligned scaffolding
1. Create `app/.../auto/` package boundary
2. Define:
   - `AutoBrowseRepository`
   - `AutoPlaybackFacade`
   - `AutoSourceSelector`
   - `AutoShowProgressResolver`
3. Encode playback rules in tests before wiring UI/service

## Phase 2 — Playback policy prototype
1. Implement `AutoSourceSelector`
2. Implement movie playback path
3. Implement show default resolver
4. Implement concise failure-result model

## Phase 3 — Browse/service integration
1. Add media service/session wiring
2. Add root browse nodes
3. Add favorites/continue-watching/recent/search
4. Wire search result → play action

## Phase 4 — Hardening
1. latency/failure handling
2. metadata/session polish
3. voice/search behavior tuning
4. compliance review against Android Auto media guidance

---

## Required test cases

Before calling MVP viable, test at least:

### Movie playback
- cached source available → plays
- direct source available, no cached → plays
- only uncached sources available → fails cleanly
- no sources → fails cleanly

### Show playback
- in-progress episode exists → resumes correct episode
- no in-progress, next unwatched exists → plays next episode
- no progress, latest episode known → plays latest
- minimal metadata only → falls back deterministically

### Setup/auth
- RD not linked → short setup-required message
- provider/source issue → short failure message, no source picker

### Browse/search
- favorites populate
- continue watching populates
- search results map to simple playable actions

---

## Recommended wording / framing

Use this internal framing going forward:

> **Asahi Auto is a playback-first companion surface, not a full browsing or source-management app.**

That sentence should drive implementation choices whenever scope starts to drift.

---

## Final recommendation

Proceed with an **Auto MVP that supports both movies and TV shows**, but with the following hard lines:
- cached/direct only
- no manual source picker
- deterministic show continuation rules
- no setup/settings in-car
- package boundary inside `app/.../auto` first

This gives Asahi the best shot at becoming genuinely useful in Android Auto without turning the project into a second full UI stack too early.

---

## Usage note

This file is intentionally being kept as an active reference in `in_progress/` because it records the locked product/technical decisions for the Android Auto work.
It is not the execution driver by itself; active implementation should be tracked in `2026-04-07-android-auto-phase-1-implementation.md`.
