# Exec Plan — Android Auto Phase 1 Implementation

Last updated: 2026-04-07 UTC
Status: IN_PROGRESS
Owner: shield-tv-bot + Mike
Location: `docs/exec-plans/in_progress/2026-04-07-android-auto-phase-1-implementation.md`
Supersedes: `docs/exec-plans/complete/2026-04-07-android-auto-architecture-spike.md`
References: `docs/exec-plans/in_progress/2026-04-07-android-auto-decision-doc.md`

## Goal

Complete the first implementation phase for Asahi Auto by creating the initial Auto package boundary, core interfaces, domain-facing facades, and rule scaffolding needed for a future Android Auto media surface.

This phase does **not** aim to ship a user-visible Android Auto app yet. It exists to lock in the right architecture before service/template integration work starts.

---

## Current Focus

**Current phase:** Phase 1 — boundary and rule scaffolding

**Immediate target:** create the `app/.../auto` package boundary and land the first pure, testable Auto playback-rule abstractions (`AutoSourceSelector` and `AutoShowProgressResolver`).

**Why this now:**
The architecture spike already did its job, and the decision doc locked the product boundaries. The next honest active step is implementation scaffolding, not more exploratory planning.

---

## Phase 1 scope

### In scope
- create `app/.../auto` package boundary
- define core Auto-facing abstractions
- encode playback rules in code + tests
- isolate Auto source-selection behavior from TV behavior
- isolate Auto show-default behavior from TV UI behavior
- create minimal result models for Auto browse/playback outcomes

### Out of scope
- full Android Auto media service wiring
- manifest/service registration for a real car surface
- DHU/Android Auto template integration
- user-facing TV UI changes
- parked-mode flows
- source picker or advanced browse tree UI

---

## Why this phase matters

Without this phase, implementation is likely to drift into one of two bad paths:
1. leaking TV/MainActivity state directly into Auto logic
2. trying to wire Android Auto service/template code before the playback/browse rules are stable

Phase 1 avoids both by establishing a clean internal API first.

---

## Target package structure

Create the following packages under:
- `app/src/main/kotlin/ai/shieldtv/app/auto/`

### Packages

```text
auto/
  model/
  browse/
  playback/
```

### Initial files to create

```text
app/src/main/kotlin/ai/shieldtv/app/auto/model/AutoActionHint.kt
app/src/main/kotlin/ai/shieldtv/app/auto/model/AutoBrowseNode.kt
app/src/main/kotlin/ai/shieldtv/app/auto/model/AutoPlaybackResult.kt
app/src/main/kotlin/ai/shieldtv/app/auto/model/AutoSourcePolicy.kt

app/src/main/kotlin/ai/shieldtv/app/auto/browse/AutoBrowseRepository.kt

app/src/main/kotlin/ai/shieldtv/app/auto/playback/AutoPlaybackFacade.kt
app/src/main/kotlin/ai/shieldtv/app/auto/playback/AutoSourceSelector.kt
app/src/main/kotlin/ai/shieldtv/app/auto/playback/AutoShowProgressResolver.kt
app/src/main/kotlin/ai/shieldtv/app/auto/playback/DefaultAutoSourceSelector.kt
```

### Test files to create

```text
app/src/test/kotlin/ai/shieldtv/app/auto/playback/DefaultAutoSourceSelectorTest.kt
app/src/test/kotlin/ai/shieldtv/app/auto/playback/AutoShowProgressResolverTest.kt
```

Optional if useful in this phase:

```text
app/src/test/kotlin/ai/shieldtv/app/auto/model/AutoBrowseNodeTest.kt
```

---

## Core interfaces and models

## 1. `AutoActionHint`

Purpose:
- label the intended action for a node in a car-safe way

Suggested enum values:
- `RESUME`
- `PLAY_MOVIE`
- `PLAY_SHOW_DEFAULT`
- `PLAY_EPISODE`
- `OPEN_COLLECTION`
- `SEARCH`

This does not need to be exhaustive yet.

---

## 2. `AutoBrowseNode`

Purpose:
- normalized browse-tree item for future Auto integration

Suggested shape:

```kotlin
data class AutoBrowseNode(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val browsable: Boolean,
    val playable: Boolean,
    val mediaRef: MediaRef? = null,
    val artworkUrl: String? = null,
    val actionHint: AutoActionHint? = null
)
```

Rule:
- keep it UI-framework-neutral
- no TV `View`/renderer assumptions

---

## 3. `AutoPlaybackResult`

Purpose:
- report success/failure from Auto playback requests without leaking UI concerns

Suggested sealed model:

```kotlin
sealed interface AutoPlaybackResult {
    data class Ready(val source: SourceResult) : AutoPlaybackResult
    data class Blocked(val userMessage: String) : AutoPlaybackResult
    data class Failed(val userMessage: String) : AutoPlaybackResult
}
```

May later include telemetry/diagnostic fields, but not in Phase 1 unless truly needed.

---

## 4. `AutoSourcePolicy`

Purpose:
- encode the MVP playback-source policy

Suggested enum/object:
- cached only
- cached then direct

For Phase 1, simplest choice:

```kotlin
enum class AutoSourcePolicy {
    CACHED_THEN_DIRECT
}
```

This leaves room to add stricter or experimental variants later.

---

## 5. `AutoBrowseRepository`

Purpose:
- future-facing repository contract for Auto-safe browse/search data

Phase 1 task:
- define the interface only
- do **not** fully implement tree generation yet unless it’s trivial

Suggested methods:

```kotlin
interface AutoBrowseRepository {
    suspend fun root(): List<AutoBrowseNode>
    suspend fun favorites(mediaType: MediaType?): List<AutoBrowseNode>
    suspend fun recent(mediaType: MediaType?): List<AutoBrowseNode>
    suspend fun continueWatching(): List<AutoBrowseNode>
    suspend fun search(query: String, mediaType: MediaType?): List<AutoBrowseNode>
}
```

---

## 6. `AutoPlaybackFacade`

Purpose:
- future-facing contract for all Auto playback requests

Phase 1 task:
- define the interface only, unless a thin placeholder implementation is helpful for tests

Suggested methods:

```kotlin
interface AutoPlaybackFacade {
    suspend fun playMovie(mediaRef: MediaRef): AutoPlaybackResult
    suspend fun resume(mediaRef: MediaRef): AutoPlaybackResult
    suspend fun playShowDefault(mediaRef: MediaRef): AutoPlaybackResult
    suspend fun playEpisode(mediaRef: MediaRef, seasonNumber: Int, episodeNumber: Int): AutoPlaybackResult
}
```

---

## 7. `AutoSourceSelector`

Purpose:
- select the best source for Auto according to MVP policy

Phase 1 task:
- implement this for real
- test it thoroughly

Suggested interface:

```kotlin
interface AutoSourceSelector {
    fun selectBestForAuto(sources: List<SourceResult>): SourceResult?
}
```

### `DefaultAutoSourceSelector`

Behavior for Phase 1:
1. first cached source wins
2. if none cached, first direct source wins
3. otherwise return null

Important:
- do not return uncached/unchecked sources in MVP policy
- do not embed UI logic or messaging here

---

## 8. `AutoShowProgressResolver`

Purpose:
- determine the default episode target for a show under Auto rules

Phase 1 task:
- define and implement pure rule logic, ideally without depending on Android UI

Suggested model:

```kotlin
data class EpisodeTarget(
    val seasonNumber: Int,
    val episodeNumber: Int
)

interface AutoShowProgressResolver {
    fun resolveDefaultEpisode(...): EpisodeTarget?
}
```

### Rule order to encode
1. resume in-progress episode
2. next unwatched episode
3. latest episode
4. fallback `S01E01`

For Phase 1, it is acceptable to start with a pure input-based resolver that accepts:
- known resume target
- watched episode keys / state
- available seasons/episodes metadata

This can later be wrapped by an app/service layer that pulls the real data from stores.

---

## Implementation sequence

## Step 1 — Create package skeleton and pure models
- add `auto/model/*`
- keep models framework-neutral
- no references to TV renderers or `MainActivity`

## Step 2 — Add pure interfaces
- `AutoBrowseRepository`
- `AutoPlaybackFacade`
- `AutoSourceSelector`
- `AutoShowProgressResolver`

At this point, code should compile with no behavior yet.

## Step 3 — Implement `DefaultAutoSourceSelector`
- use existing `SourceResult` / `CacheStatus`
- ensure cached/direct-only selection
- add unit tests first or immediately after

## Step 4 — Implement `AutoShowProgressResolver`
- begin with pure algorithmic resolver
- encode the decision order from the decision doc
- add test coverage for ambiguous and sparse-data cases

## Step 5 — Add minimal composition notes / placeholder wiring
- if helpful, add comments or TODOs describing where the real `favorites`, `history`, `continue watching`, and playback launch flows will plug in later
- do not force a premature service implementation

---

## Validation

### Required checks
- `./gradlew testDebugUnitTest assembleDebug`

### Required unit tests

#### `DefaultAutoSourceSelectorTest`
Must cover:
- cached beats direct
- direct chosen when no cached exists
- uncached-only returns null
- empty list returns null
- mixed list with cached later in the list still selects the first cached match according to intended policy

#### `AutoShowProgressResolverTest`
Must cover:
- resume target exists → chosen
- no resume, next unwatched exists → chosen
- no progress, latest episode exists → chosen
- only minimal metadata exists → `S01E01`
- sparse seasons/episodes still produce deterministic output

---

## Coding rules for this phase

- No Android `View` dependencies in Auto models/interfaces
- No direct dependency on `MainActivity` from Auto packages
- No TV renderer imports in Auto code
- No source-picker UI assumptions in selector/resolver logic
- Keep algorithms pure/testable where possible
- Avoid building the full browse tree implementation until contracts are stable

---

## Nice-to-haves if time remains

If Phase 1 lands smoothly, optionally add:
- a tiny `DefaultAutoBrowseNodeFactory` helper for mapping favorites/history/search results into `AutoBrowseNode`
- a lightweight internal README or package comment under `auto/` documenting the Auto boundary

Do not let these delay core abstractions/tests.

---

## Stop point

Stop this phase when all of the following are true:
- Auto package boundary exists
- core interfaces/models compile
- `DefaultAutoSourceSelector` implemented and tested
- `AutoShowProgressResolver` implemented and tested
- no Android Auto service/template integration has been started yet

That is the correct handoff point into Phase 2.

---

## Deliverables

At completion, the repo should contain:
- initial `auto/` package structure
- source-selection logic for Auto MVP
- show-default rule logic for Auto MVP
- tests proving the core decision rules
- a clean base for future Auto service/browse integration

---

## Progress Log

### 2026-04-07 18:05 UTC
- Promoted this file to the single active execution driver for Android Auto implementation work.
- Kept the separate decision doc as a locked-reference companion and moved the earlier architecture spike out of the active plan set.
- No code implementation completed under this plan yet.

---

## Session Start

### 2026-04-07 18:05 UTC
Intended task: use this file as the sole active implementation plan for the Android Auto phase-1 scaffolding pass

---

## Follow-up phase

The next plan after this should be something like:
- `2026-04-07-android-auto-phase-2-service-and-browse-skeleton.md`

That phase would wire:
- media service/session shell
- initial browse roots
- minimal playback handoff using these new abstractions
