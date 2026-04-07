# Exec Plan — Android Auto Architecture Spike

Last updated: 2026-04-07 UTC
Status: COMPLETE
Owner: shield-tv-bot + Mike
Location: `docs/exec-plans/complete/2026-04-07-android-auto-architecture-spike.md`
Superseded by: `docs/exec-plans/in_progress/2026-04-07-android-auto-phase-1-implementation.md`

## Goal

Define a practical, implementation-ready architecture for an Android Auto-compatible Asahi surface that reuses the existing app’s media/search/source/playback stack while respecting in-car UX and platform constraints.

This is an architecture/design spike first — not a commitment to immediately implement the full feature.

---

## Why this needs a separate plan

Asahi’s current TV-first UI and interaction model are not directly portable to Android Auto. The Auto version must be a constrained, template-driven companion surface with simplified browse/play flows, deterministic playback behavior, and strict removal of dense source-selection and settings workflows.

This plan exists to:
- prevent “port the TV app to Auto” drift
- force clear product/UX boundaries
- isolate reusable domain pieces from TV-specific presentation logic
- define an MVP that is actually plausible for in-car use

---

## Platform assumptions / constraints

Based on current Android for Cars media guidance and templates:
- Android Auto media experiences are template-driven, not custom-TV-layout-driven
- Media apps are best suited to simple browse/search/play flows
- Complex source-selection, debugging, and setup flows are poor fits for in-car use
- Media playback metadata/session integration is a core requirement
- Video-like/parked experiences are a different category and should not be assumed for the initial Auto MVP

Working assumption for Asahi Auto:
- treat it as a **media companion surface**
- optimize for **resume, favorites, search, latest/next episode, and simple playback start**
- do **not** expose advanced source/provider controls in Auto MVP

---

## Product boundaries

### In scope for Auto MVP
- Continue Watching / Resume
- Favorites
- Recent items / history
- Movies / TV root browse entry points
- Search
- Play movie directly
- Play show via deterministic rule set:
  - resume in-progress episode if available
  - else next unwatched episode if available
  - else latest episode
  - else fallback episode rule
- Auto source selection (no manual source picker)

### Explicitly out of scope for Auto MVP
- Source picker UI
- Provider diagnostics / scraping details
- Real-Debrid link/setup flow in-car
- Advanced settings
- Full details pages mirroring TV UI
- Dense poster browsing / custom layouts
- Anything that requires more than a small number of taps or lots of visual inspection

---

## Architecture overview

### Principle
Build a **separate Auto surface** on top of the existing shared core, rather than adapting the TV presentation layer.

### Existing reusable layers
Re-use where possible:
- `core:model`
- `domain`
- metadata/search integrations
- favorites/history/continue-watching stores
- playback engine/session infrastructure
- source resolution/ranking logic (but wrapped for Auto)

### New layers to add
Introduce Auto-specific orchestration/facade layers:
- `AutoBrowseRepository`
- `AutoPlaybackFacade`
- `AutoSourceSelector`
- `AutoCapabilityPolicy`
- `AutoMediaService` / session integration layer

---

## Proposed module/package structure

### Preferred near-term structure
Keep initial work in the existing app repo and add a dedicated package/module boundary.

Option A (simpler first step):
- `app/src/main/kotlin/ai/shieldtv/app/auto/...`

Option B (cleaner long-term separation):
- `feature/auto/...`

Recommended direction:
- start with **`app/.../auto`** if speed matters
- graduate to **`feature/auto`** once API boundaries stabilize

### Suggested package layout

```text
app/src/main/kotlin/ai/shieldtv/app/auto/
  AutoMediaService.kt
  AutoSessionConnector.kt
  browse/
    AutoBrowseRepository.kt
    AutoBrowseTreeBuilder.kt
    AutoBrowseNode.kt
    AutoSearchHandler.kt
  playback/
    AutoPlaybackFacade.kt
    AutoSourceSelector.kt
    AutoPlaybackPolicy.kt
  model/
    AutoActionHint.kt
    AutoSection.kt
```

---

## Core abstractions

### `AutoBrowseRepository`
Purpose:
- expose a car-safe browse/search model derived from existing Asahi data

Responsibilities:
- build root sections
- return favorites, continue-watching, recents, search results
- flatten TV/domain data into Auto-friendly browse nodes

Possible API:

```kotlin
interface AutoBrowseRepository {
    suspend fun root(): List<AutoBrowseNode>
    suspend fun favorites(mediaType: MediaType?): List<AutoBrowseNode>
    suspend fun recent(mediaType: MediaType?): List<AutoBrowseNode>
    suspend fun continueWatching(): List<AutoBrowseNode>
    suspend fun search(query: String, mediaType: MediaType?): List<AutoBrowseNode>
}
```

### `AutoPlaybackFacade`
Purpose:
- expose simple playback intents for the car surface without leaking source-picker complexity

Responsibilities:
- resolve the correct title/episode target
- request sources
- use `AutoSourceSelector` to pick the best candidate
- start playback / update media session state
- return short actionable failure messages when playback cannot start

Possible API:

```kotlin
interface AutoPlaybackFacade {
    suspend fun playMovie(mediaRef: MediaRef): AutoPlaybackResult
    suspend fun resume(mediaRef: MediaRef): AutoPlaybackResult
    suspend fun playShowDefault(mediaRef: MediaRef): AutoPlaybackResult
    suspend fun playEpisode(mediaRef: MediaRef, season: Int, episode: Int): AutoPlaybackResult
}
```

### `AutoSourceSelector`
Purpose:
- choose the single best source automatically for in-car playback

Responsibilities:
- rank sources deterministically for Auto use
- avoid exposing source choice UI

Suggested rule order:
1. cached source
2. direct source
3. acceptable fallback source (only if product wants it)
4. otherwise fail

Possible API:

```kotlin
interface AutoSourceSelector {
    fun selectBestForAuto(sources: List<SourceResult>): SourceResult?
}
```

### `AutoCapabilityPolicy`
Purpose:
- explicitly block flows that should never appear in Auto

Examples:
- no settings deep links
- no provider toggles
- no manual source selection
- no auth-linking flow in-car

---

## Browse tree / navigation model

### Root nodes
- Continue Watching
- Favorites
- Movies
- TV Shows
- Recent
- Search

### Movies branch
- Favorites
- Recent
- Search Movies
- optionally curated/recommended later

### TV Shows branch
- Favorites
- Continue Watching
- Recent
- Search TV Shows

### Show behavior
Avoid a heavy details page. Prefer action-oriented nodes:
- Resume episode
- Play next episode
- Play latest episode

If a show needs deeper episode selection, defer to a future parked-only flow, not MVP.

---

## Playback rules

### Movie rule
- fetch sources
- select best auto-safe source
- start playback
- if none found, return short failure state

### Show rule
When the user picks a show-level item:
1. if there is an in-progress episode → resume that
2. else if there is a next unwatched episode → play that
3. else if latest season/episode is known → play latest
4. else fallback to a deterministic default episode

### Failure handling
If playback cannot proceed:
- do not open source picker
- do not expose diagnostics
- return a short state like:
  - “No playable source found”
  - “Account setup required on phone/TV”

---

## Android integration plan

### Likely components
- Auto-compatible media service
- media session / metadata publishing
- browse tree provider
- search entry point
- playback state publisher

### Important separation
Keep Auto session/browse integration thin.
It should call shared facades, not encode business logic inside the service/template layer.

---

## State and data reuse

Leverage existing stores where possible:
- favorites
- watch history
- continue watching
- selected playback memory / resume state

Potential additions:
- lightweight `AutoRecentSelectionStore` only if needed for UX polish
- `AutoShowProgressResolver` to compute resume/next/latest show behavior from existing history + playback state

---

## MVP implementation phases

### Phase 1 — Design/API boundary spike
Deliverables:
- package/module skeleton
- interface definitions for Auto facades
- documented playback rules
- documented browse tree
- known gaps list

### Phase 2 — Thin playable prototype
Deliverables:
- Auto media entrypoint/service skeleton
- root browse tree wired to existing favorites/history/continue-watching
- search wired to existing search pipeline
- automatic source selection wired to playback

### Phase 3 — UX hardening
Deliverables:
- clean failure states
- faster load behavior / timeout handling
- voice/search polish
- better show default heuristics

---

## Key technical risks

### 1. Source-resolution latency
In-car playback will feel bad if source lookup takes too long.
Mitigations:
- smarter caching
- precomputed best-source candidates for continue-watching/favorites (future)
- tight timeout/failure rules

### 2. Ambiguous show defaults
“Play this show” is underspecified.
Mitigations:
- define one deterministic rule set and keep it consistent
- document it clearly for product behavior

### 3. Setup/auth dependency
If account linking is required, Auto cannot be the primary setup surface.
Mitigations:
- detect and short-circuit with clear messaging
- point users back to phone/TV for setup

### 4. Product fit / platform compliance
Even if technically feasible, UX must still feel like a valid in-car media experience.
Mitigations:
- keep MVP narrow
- avoid exposing TV-style complexity
- validate with Android Auto guidance and sample apps before deep implementation

---

## Open questions

1. Should Auto support **movies only** in MVP first, then TV shows second?
2. Is uncached playback acceptable in-car, or should Auto be cached/direct-only?
3. Should show-level selection default to **resume**, **next episode**, or **latest episode**?
4. Do we want to support any **parked-only richer browse flow** later?
5. Should Android Auto support remain inside the main app module, or graduate quickly to `feature/auto`?

---

## Recommendation

Strong recommendation:
- treat this as **Asahi Auto**, a companion playback surface
- do **not** attempt to mirror the TV app UX
- prioritize resume/search/favorites/simple play over deep browsing

If implementation starts, begin with:
1. `AutoSourceSelector`
2. `AutoPlaybackFacade`
3. `AutoBrowseRepository`
4. Auto media service/session wiring

That sequence keeps the hard product decisions explicit before UI/framework work takes over.

---

## Completion criteria for this spike

This exec plan is complete when we have:
- a documented Auto product boundary
- a reusable architecture map
- clear MVP scope
- a phased implementation path
- open questions identified before coding begins

---

## Completion note

Completed on 2026-04-07 as a discovery/architecture artifact.
Its open questions were intentionally carried forward into the decision doc, and the implementation-driving plan is now `2026-04-07-android-auto-phase-1-implementation.md`.
This file should remain a background/reference document rather than the active execution driver.
