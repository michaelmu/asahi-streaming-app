# Shield Streaming App - Architecture Plan

Last updated: 2026-04-01 UTC

## Goal
Build a standalone Android TV / Google TV / NVIDIA Shield app that reproduces the most valuable Fenlight workflows natively:
- title search
- metadata browsing
- source scraping
- Real-Debrid-backed source resolution
- TV-friendly playback
- resume/progress handling

This document turns the repo research into an actionable architecture direction.

---

## 1. Product Thesis

The app should be:
- **more focused than Kodi**
- **more native to Android TV than Stremio Web/Shell**
- **feature-driven by Fenlight’s workflow**
- **modular enough to grow into multiple providers/debrid services later**

In practice, that means:
- don’t build a universal media center at v1
- don’t port a Kodi plugin architecture into Android
- do build a clean source orchestration engine with a good TV shell

---

## 2. Primary References and What They Teach Us

### Kodi / XBMC
Use for:
- 10-foot UX expectations
- remote navigation behavior
- playback ergonomics
- settings density cautionary tale

### Fenlight
Use for:
- functional target behavior
- source orchestration workflow
- result ranking/filtering concepts
- debrid-backed source resolution
- playback and next-episode flow expectations

### CocoScrapers
Use for:
- scraper/provider abstraction ideas
- per-provider enable/disable model
- modular source-provider loading concept

### Stremio Core / Core Kotlin / Web / Video
Use for:
- separation of core logic from shell
- state/action/effect thinking
- service boundaries
- playback subsystem boundaries
- screen and product surface organization

### AndroidX Media / Media3
Use for:
- actual playback foundation
- sessions, player integration, subtitles, tracks, data sources
- reference demos and implementation details

---

## 3. Recommended Technology Direction

## Decision
**Build v1 as a native Kotlin Android TV app using Media3.**

### Why
This is the shortest path to:
- real Shield-native performance
- sane remote/focus behavior
- clean playback integration
- good maintainability
- less toolchain pain than Rust+NDK on day one

### Not recommended for v1
- porting Fenlight directly
- embedding a web shell as the primary app architecture
- introducing Rust core too early unless multi-platform reuse becomes urgent

### Revisit later if needed
A Rust/shared core may become worth it if:
- Android + desktop + web clients must share source logic
- ranking/filtering/resolution logic becomes large and duplicated
- we want provider logic portability as a strategic goal

---

## 4. Recommended High-Level Architecture

```text
TV App UI
  ↓
Presentation Layer
  ↓
Domain Layer
  ↓
Data / Integration Layer
  ├─ Metadata providers
  ├─ Scraper providers
  ├─ Debrid providers
  ├─ Playback gateway
  └─ Persistence/cache
```

### Principle
UI should never directly know how:
- Real-Debrid auth works
- scraping providers are loaded
- hashes are checked
- links are resolved
- ranking is computed

That all belongs below the screen layer.

---

## 5. Proposed Module Breakdown

## `app-tv`
Android TV application module.

Contains:
- navigation
- activity/entrypoints
- DI wiring
- app theme
- platform bootstrapping

## `feature-home`
Home / board / continue-watching / entry navigation.

## `feature-search`
Search screens and search result flows.

## `feature-details`
Movie/show/season/episode details.

## `feature-sources`
Source loading, source list, filtering, source selection.

## `feature-player`
Playback screen integration, resume prompts, playback overlays.

## `feature-settings`
Account linking, provider toggles, playback preferences, debug tools.

## `core-model`
Typed domain models used across the app.

## `core-domain`
Use cases, orchestration, ranking, filtering, resolution policies.

## `core-data`
Repositories, persistence, database, preferences, cache.

## `integration-metadata`
TMDb first, others later.

## `integration-debrid`
Real-Debrid first.

## `integration-scrapers`
Scraper/provider adapter layer inspired by CocoScrapers.

## `integration-playback`
Media3 player gateway and playback session support.

## `core-network`
HTTP clients, interceptors, serialization, retry policy.

## `core-common`
Utilities, logging, result wrappers, dispatchers, constants.

---

## 6. Core Domain Models

These should be explicit and typed from the start.

## Media reference models

### `MediaRef`
Represents a movie/show/season/episode target.

Possible fields:
- id
- mediaType
- tmdbId
- imdbId?
- title
- year?
- posterUrl?

### `EpisodeRef`
- showTmdbId
- seasonNumber
- episodeNumber
- episodeTitle?
- airDate?

## Search + metadata models

### `SearchQuery`
- text
- mediaTypeFilter?

### `SearchResult`
- mediaRef
- title
- subtitle
- poster
- badges

### `TitleDetails`
- metadata
- artwork
- seasons/episodes
- cast
- runtime
- genres
- playback eligibility hints

## Source models

### `SourceSearchRequest`
- mediaRef
- aliases
- title
- year
- season?
- episode?
- episodeTitle?
- imdbId?
- tvdbId?
- playbackMode
- filters

### `SourceResult`
Core source-selection object.

Suggested fields:
- id
- mediaRef
- providerId
- providerKind
- debridService
- sourceSite
- linkType
- url
- infoHash?
- displayName
- quality
- sizeBytes?
- sizeLabel?
- videoFlags
- audioFlags
- languageFlags
- packageType
- seeders?
- cacheStatus
- score?
- rawMetadata

### `SourceResolutionRequest`
- sourceResult
- mediaRef
- resolutionPolicy

### `ResolvedStream`
- url
- headers?
- mimeType?
- subtitles?
- title
- streamType
- originalSource

## Account/auth models

### `RealDebridAuthState`
- isLinked
- username?
- expiresAt?
- authInProgress
- error?

### `DeviceCodeFlow`
- verificationUrl
- userCode
- qrPayload
- expiresIn
- pollInterval

## Playback models

### `PlaybackItem`
- mediaRef
- title
- subtitle
- artwork
- resolvedStream
- resumePositionMs?

### `PlaybackProgress`
- mediaRef
- positionMs
- durationMs
- completed
- updatedAt

---

## 7. Key Use Cases

These are the business actions the app should center around.

## Search and browse
- `SearchTitlesUseCase`
- `GetTitleDetailsUseCase`
- `GetEpisodesUseCase`
- `GetContinueWatchingUseCase`

## Sources
- `BuildSourceSearchRequestUseCase`
- `FindSourcesUseCase`
- `RankSourcesUseCase`
- `FilterSourcesUseCase`
- `ResolveSourceUseCase`

## Debrid
- `StartRealDebridDeviceAuthUseCase`
- `PollRealDebridDeviceAuthUseCase`
- `GetRealDebridAccountUseCase`
- `GetCachedStatusUseCase`
- `ResolveMagnetWithRealDebridUseCase`
- `UnrestrictLinkUseCase`

## Playback
- `BuildPlaybackItemUseCase`
- `StartPlaybackUseCase`
- `GetResumePointUseCase`
- `SavePlaybackProgressUseCase`
- `MarkWatchedUseCase`
- `PrepareNextEpisodeUseCase`

## Settings/providers
- `GetEnabledProvidersUseCase`
- `SetProviderEnabledUseCase`
- `GetSourcePreferencesUseCase`
- `UpdateSourcePreferencesUseCase`

---

## 8. Scraper Provider Architecture

This is one of the most important decisions.

## Goal
Replicate the *benefit* of CocoScrapers without reproducing Python/Kodi plugin mechanics.

## Proposed shape
Define a provider contract like:

```text
interface SourceProvider {
  val id: String
  val kind: ProviderKind
  suspend fun search(request: SourceSearchRequest): List<RawProviderSource>
}
```

Then create adapters that map raw provider output into `SourceResult`.

## Important design rules
- provider code should not know about Android UI
- provider code should not know about Media3
- provider output should be normalized centrally
- provider enable/disable should be configuration-driven
- provider failures should be isolated, not fatal to the whole search

## v1 provider strategy
Keep it tight.
Do **not** try to support everything Fenlight/CocoScrapers supports at once.

Good v1 approach:
- Real-Debrid cloud source provider
- 1-3 external scraper providers / aggregator adapters
- maybe Torrentio-like provider if useful
- provider registry with toggle support

---

## 9. Real-Debrid Integration Plan

## v1 requirements
- device authorization flow
- token persistence and refresh
- account status display
- cached-status checks for hashes
- magnet resolution to playable link
- direct link unrestriction

## Suggested layering

### `RealDebridApi`
Raw HTTP API wrapper.

### `RealDebridRepository`
Token management, auth state, account info, cache checks, resolution methods.

### `RealDebridResolver`
Higher-level logic:
- add magnet
- select files
- wait/poll as needed
- choose best matching file
- unrestrict final link

## Important rule
Keep torrent/file selection policy **separate** from raw RD API calls.
That selection logic will evolve and should be testable.

---

## 10. Ranking and Filtering Engine

Fenlight proves this is not optional.

## Engine responsibilities
- cache-first prioritization
- quality ranking
- provider preference ranking
- size-based ordering
- codec/audio preference boosts
- exclusion filters
- per-quality caps
- total result caps
- uncached fallback handling

## Recommendation
Implement ranking as a dedicated domain component, not screen code.

Example pieces:
- `SourceScorer`
- `SourceFilterEngine`
- `SourceSortPolicy`
- `SourcePreferenceRepository`

This logic should be unit tested heavily.

---

## 11. Playback Subsystem

## v1 playback stack
- Media3 ExoPlayer
- subtitle support
- track selection support
- resume position
- playback progress persistence
- watch completion detection

## Clean boundary
Define a playback gateway so the rest of the app depends on an interface, not directly on ExoPlayer internals.

Example:

```text
interface PlaybackEngine {
  suspend fun prepare(item: PlaybackItem)
  fun play()
  fun pause()
  fun stop()
  fun observeState(): Flow<PlaybackState>
}
```

## Why this matters
Even if v1 only uses Media3, this separation keeps:
- screen logic simpler
- testing easier
- future player changes isolated

## TV-specific playback UX to include
- resume prompt
- visible loading / resolving states
- graceful playback failure message
- source fallback path
- maybe next-up later, not necessarily v1 day one

---

## 12. Screen / Navigation Plan

Borrowing from Stremio Web’s product structure, but adapted for TV:

## Screens
- Home / Board
- Search
- Details
- Episode Picker
- Sources
- Player
- Settings
- Accounts
- Providers

## Suggested v1 navigation sequence
1. Search title
2. Open details screen
3. Choose movie / episode
4. Open sources screen
5. Pick source
6. Resolve stream
7. Play
8. Save progress

## Home screen can be simple at first
No need to build a huge discovery surface immediately.
Start with:
- continue watching
- recent searches
- maybe trending/popular rows later

---

## 13. State Management Recommendation

Stremio’s architecture strongly suggests separating actions, state, and transport effects.

For this app, a sane Kotlin-native version would be:
- ViewModel per feature
- immutable screen state
- actions/intents from UI
- use-case execution in domain layer
- StateFlow for observation
- one-off effects for navigation/snackbars/dialogs

## Good fit per feature
- `SearchViewModel`
- `DetailsViewModel`
- `SourcesViewModel`
- `PlayerViewModel`
- `SettingsViewModel`

## Avoid
- putting orchestration logic in composables/fragments
- passing giant mutable maps around like plugin params
- coupling source resolution directly to screen code

---

## 14. Persistence and Cache Plan

## Persistent stores needed early
- Real-Debrid tokens
- linked account state
- search history
- provider settings
- source filtering preferences
- playback progress / bookmarks
- continue-watching items

## Cache layers needed
- metadata cache
- source result cache
- debrid cache status cache
- resolved stream short-term cache (if useful/safe)

## Suggested implementation
- Room for durable structured data
- DataStore for settings/preferences
- in-memory cache for active session work

---

## 15. Suggested v1 Milestones

## Milestone 0 - foundation
- project skeleton
- module structure
- navigation shell
- theme/focus baseline for Android TV
- network stack
- persistence baseline

## Milestone 1 - metadata vertical slice
- TMDb-backed search
- details screen for movie/show
- episode browsing

## Milestone 2 - Real-Debrid account slice
- device auth flow
- token refresh/persistence
- account info/settings screen

## Milestone 3 - source orchestration slice
- provider contract
- one or two providers
- result normalization
- ranking/filtering engine
- source list UI

## Milestone 4 - playable vertical slice
- resolve source through RD
- pass resolved stream into Media3
- playback UI
- resume/progress tracking

## Milestone 5 - polish
- continue watching
- provider toggles
- better filters
- error handling
- diagnostics/logging

---

## 16. Biggest Risks

## Risk 1 - too much scope too early
Kodi/Fenlight both tempt feature explosion.
Countermeasure: protect v1 hard.

## Risk 2 - scraper instability
Providers break.
Countermeasure: isolate adapters and fail gracefully.

## Risk 3 - tangled source logic
If source ranking/resolution leaks into UI, the app will rot fast.
Countermeasure: keep a strong domain layer.

## Risk 4 - premature shared-core complexity
Rust/NDK/protobuf bridging could slow the project down badly.
Countermeasure: stay Kotlin-first until pain proves otherwise.

## Risk 5 - playback edge cases
Media playback on TV devices always has weird corners.
Countermeasure: use Media3, test on Shield early, keep playback isolated.

---

## 17. Best Current Recommendation

If starting implementation soon, the smartest path is:
1. create a Kotlin Android TV app shell
2. define the core typed models now
3. implement TMDb search/details
4. implement Real-Debrid auth and resolution
5. implement a minimal provider system
6. build a source list + Media3 playback vertical slice

That gives the fastest route to a real, testable product.

## 18. Implementation Note

As implementation begins, the preferred approach is **incremental scaffold-first delivery**:
- keep the module graph coherent
- add contracts before concrete feature logic
- prefer stub implementations over premature complexity
- update docs when implementation uncovers a structural adjustment

This reduces thrash while the project is still becoming real.
