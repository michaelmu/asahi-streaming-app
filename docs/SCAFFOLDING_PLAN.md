# Shield Streaming App - Scaffolding Plan

Last updated: 2026-04-01 UTC

## Purpose
Translate the architecture and domain contracts into a concrete project/module/package/file plan.

This is the bridge between planning and actual code scaffolding.

---

## 1. Guiding Constraints

### Constraint 1: Start narrow
The initial scaffold should optimize for the first playable vertical slice, not final-system completeness.

### Constraint 2: Keep modules meaningful
Too many modules too early can become theater.
We want enough separation to avoid rot, but not so much that the build becomes annoying before the app exists.

### Constraint 3: Optimize for refactorability
It should be easy to split modules further later.

### Constraint 4: Protect the domain layer
Source orchestration, ranking, resolution, and auth should not get swallowed by UI code.

---

## 2. Recommended Initial Gradle Module Layout

## Phase 1 module set
This is the recommended **starting** module layout.

```text
:app
:core:model
:core:common
:core:network
:core:data
:domain
:feature:search
:feature:details
:feature:sources
:feature:player
:feature:settings
:integration:metadata-tmdb
:integration:debrid-realdebrid
:integration:scrapers
:integration:playback-media3
```

This is enough to preserve architectural clarity without going insane.

## Why not more modules immediately?
Because at the start, these would be overkill:
- separate `feature:home`
- separate `feature:accounts`
- separate `domain:ranking`
- separate `integration:providers:torrentio`
- separate persistence modules for every concern

Those can come later if the app earns them.

---

## 3. What Each Module Should Contain

## `:app`
Responsibilities:
- Android application entrypoint
- activity setup
- navigation host
- DI bootstrapping
- app theme
- TV-specific app wiring
- thin app-level orchestration/coordinator layer connecting feature slices

Should contain:
- `MainActivity`
- app navigation graph
- dependency container / Hilt setup if used
- startup initialization

Should NOT contain:
- scraper logic
- debrid logic
- ranking logic
- metadata API logic

---

## `:core:model`
Responsibilities:
- pure domain-facing shared models and enums

Should contain:
- media models
- source models
- playback models
- auth models
- enums/value classes

This is where most of `DOMAIN_CONTRACTS.md` models would live.

---

## `:core:common`
Responsibilities:
- cross-cutting shared utility code

Should contain:
- result wrappers
- error abstractions
- dispatchers abstraction
- logging interfaces
- constants
- common extension functions

Avoid putting business logic here.

---

## `:core:network`
Responsibilities:
- HTTP and serialization baseline

Should contain:
- Ktor or OkHttp setup
- JSON serialization config
- interceptors
- auth header helpers
- retry and timeout configuration

---

## `:core:data`
Responsibilities:
- persistence and repositories shared infrastructure

Should contain:
- Room database setup
- DataStore setup
- DAO definitions
- local entities
- mappers between persistence models and domain models

Not all repositories have to be fully implemented here, but the local storage foundation lives here.

---

## `:domain`
Responsibilities:
- interfaces + use cases + domain services

Should contain:
- repository interfaces
- provider contracts
- ranker/filter contracts
- use cases
- orchestration services

This is the protected heart of the app.

---

## `:feature:search`
Responsibilities:
- search UI state + search screen

Should contain:
- `SearchViewModel`
- `SearchScreen`
- search UI models
- action/effect classes if used

---

## `:feature:details`
Responsibilities:
- title details screen
- episode list behavior

Should contain:
- `DetailsViewModel`
- `DetailsScreen`
- episode selection handling

---

## `:feature:sources`
Responsibilities:
- source list screen
- source filtering interactions
- source selection flow

Should contain:
- `SourcesViewModel`
- `SourcesScreen`
- source chips/filter UI
- source item components

Important: ranking logic stays in domain, not here.

---

## `:feature:player`
Responsibilities:
- player screen integration
- playback state observation
- resume/start-over prompt handling

Should contain:
- `PlayerViewModel`
- `PlayerScreen`
- player host composable/view

---

## `:feature:settings`
Responsibilities:
- account linking UI
- provider toggles
- playback/source preferences

Should contain:
- `SettingsViewModel`
- `SettingsScreen`
- `AccountSettingsScreen`
- `ProviderSettingsScreen`

---

## `:integration:metadata-tmdb`
Responsibilities:
- TMDb API integration
- mapping TMDb responses into app models

Should contain:
- TMDb API service
- DTOs
- mappers
- `MetadataRepository` implementation

---

## `:integration:debrid-realdebrid`
Responsibilities:
- Real-Debrid API + auth + resolution logic

Should contain:
- API client
- auth flow implementation
- token refresh
- account endpoints
- cache-check endpoints
- magnet resolution logic
- `DebridRepository` implementation

This is one of the most important modules.

---

## `:integration:scrapers`
Responsibilities:
- provider registry
- raw provider adapters
- source normalization
- source repository implementation

Should contain:
- `SourceProvider` implementations
- provider enable/disable resolution
- provider dispatcher/concurrency orchestration
- `SourceNormalizer`
- `SourceRepository` implementation

---

## `:integration:playback-media3`
Responsibilities:
- Media3 playback engine implementation
- player preparation
- track/subtitle integration
- playback state observation

Should contain:
- `PlaybackEngine` implementation
- player factory/wrapper
- Media3 data source integration
- session glue if needed

---

## 4. Suggested Package Layout by Module

Below is a sane initial package structure using base package:

```text
ai.shieldtv.app
```

Adjust later if Mike chooses a final package name.

---

## `:core:model`
```text
ai.shieldtv.app.core.model
  media/
    MediaType.kt
    MediaIds.kt
    MediaRef.kt
    EpisodeRef.kt
    SearchQuery.kt
    SearchResult.kt
    TitleDetails.kt
    SeasonSummary.kt
    EpisodeSummary.kt
  source/
    Quality.kt
    CacheStatus.kt
    ProviderKind.kt
    DebridService.kt
    LinkType.kt
    PackageType.kt
    PlaybackMode.kt
    VideoFlag.kt
    AudioFlag.kt
    SourceFilters.kt
    SourceSearchRequest.kt
    SourceResult.kt
    RankedSources.kt
    SourceResolutionRequest.kt
    ResolvedSubtitle.kt
    ResolvedStream.kt
  auth/
    DeviceCodeFlow.kt
    RealDebridAccount.kt
    RealDebridAuthState.kt
    CacheCheckRequest.kt
    CacheCheckResult.kt
  playback/
    ResumeInfo.kt
    PlaybackItem.kt
    PlaybackProgress.kt
    PlaybackState.kt
```

---

## `:core:common`
```text
ai.shieldtv.app.core.common
  result/
    AppResult.kt
    AppError.kt
  coroutine/
    AppDispatchers.kt
  logging/
    AppLogger.kt
  util/
    TimeUtils.kt
    SizeFormatter.kt
    StringSanitizer.kt
```

---

## `:core:network`
```text
ai.shieldtv.app.core.network
  client/
    HttpClientFactory.kt
  json/
    JsonFactory.kt
  auth/
    AuthHeaderProvider.kt
  model/
    NetworkError.kt
```

---

## `:core:data`
```text
ai.shieldtv.app.core.data
  db/
    AppDatabase.kt
    DaoProvider.kt
  datastore/
    PreferencesStore.kt
  progress/
    PlaybackProgressEntity.kt
    PlaybackProgressDao.kt
    PlaybackProgressMapper.kt
  search/
    SearchHistoryEntity.kt
    SearchHistoryDao.kt
  settings/
    ProviderPreferenceEntity.kt
    FilterPreferenceEntity.kt
```

---

## `:domain`
```text
ai.shieldtv.app.domain
  repository/
    MetadataRepository.kt
    SourceRepository.kt
    SourcePreferencesRepository.kt
    DebridRepository.kt
    PlaybackProgressRepository.kt
    SearchHistoryRepository.kt
  provider/
    SourceProvider.kt
    RawProviderSource.kt
    SourceNormalizer.kt
  source/
    ranking/
      SourceRanker.kt
      SourceScorer.kt
      SourceFilterEngine.kt
    service/
      SourceOrchestrator.kt
  playback/
    PlaybackEngine.kt
    StreamResolver.kt
  usecase/
    search/
      SearchTitlesUseCase.kt
    details/
      GetTitleDetailsUseCase.kt
    sources/
      FindSourcesUseCase.kt
      ResolveSourceUseCase.kt
      BuildSourceSearchRequestUseCase.kt
    auth/
      StartRealDebridDeviceFlowUseCase.kt
      PollRealDebridDeviceFlowUseCase.kt
    playback/
      BuildPlaybackItemUseCase.kt
      SavePlaybackProgressUseCase.kt
```

---

## `:feature:search`
```text
ai.shieldtv.app.feature.search
  ui/
    SearchScreen.kt
    SearchActions.kt
    SearchUiState.kt
  presentation/
    SearchViewModel.kt
  component/
    SearchBar.kt
    SearchResultRow.kt
    RecentQueryRow.kt
```

---

## `:feature:details`
```text
ai.shieldtv.app.feature.details
  ui/
    DetailsScreen.kt
    DetailsUiState.kt
  presentation/
    DetailsViewModel.kt
  component/
    TitleHeader.kt
    EpisodeList.kt
    SeasonTabs.kt
```

---

## `:feature:sources`
```text
ai.shieldtv.app.feature.sources
  ui/
    SourcesScreen.kt
    SourcesUiState.kt
    SourceFiltersUiState.kt
  presentation/
    SourcesViewModel.kt
  component/
    SourceRow.kt
    QualityFilterChips.kt
    ProviderFilterChips.kt
    SourceInfoPanel.kt
```

---

## `:feature:player`
```text
ai.shieldtv.app.feature.player
  ui/
    PlayerScreen.kt
    PlayerUiState.kt
    ResumePromptState.kt
  presentation/
    PlayerViewModel.kt
  component/
    PlayerHost.kt
    ResumePromptDialog.kt
```

---

## `:feature:settings`
```text
ai.shieldtv.app.feature.settings
  ui/
    SettingsScreen.kt
    AccountSettingsScreen.kt
    ProviderSettingsScreen.kt
    PlaybackSettingsScreen.kt
    AccountUiState.kt
  presentation/
    SettingsViewModel.kt
    AccountSettingsViewModel.kt
  component/
    ProviderToggleRow.kt
    DeviceCodePanel.kt
```

---

## `:integration:metadata-tmdb`
```text
ai.shieldtv.app.integration.metadata.tmdb
  api/
    TmdbApi.kt
  dto/
    SearchResponseDto.kt
    DetailsResponseDto.kt
    EpisodeDto.kt
  mapper/
    TmdbSearchMapper.kt
    TmdbDetailsMapper.kt
  repository/
    TmdbMetadataRepository.kt
```

---

## `:integration:debrid-realdebrid`
```text
ai.shieldtv.app.integration.debrid.realdebrid
  api/
    RealDebridApi.kt
  dto/
    DeviceCodeDto.kt
    TokenDto.kt
    AccountDto.kt
    InstantAvailabilityDto.kt
    TorrentInfoDto.kt
    UnrestrictLinkDto.kt
  auth/
    RealDebridTokenStore.kt
    RealDebridAuthManager.kt
  resolver/
    RealDebridFileSelector.kt
    RealDebridStreamResolver.kt
  repository/
    RealDebridRepositoryImpl.kt
  mapper/
    RealDebridMapper.kt
```

---

## `:integration:scrapers`
```text
ai.shieldtv.app.integration.scrapers
  provider/
    ProviderRegistry.kt
    ProviderSettingsResolver.kt
  raw/
    torrentio/
      TorrentioProvider.kt
    sample/
      SampleProvider.kt
  normalize/
    DefaultSourceNormalizer.kt
    ReleaseInfoParser.kt
  repository/
    SourceRepositoryImpl.kt
  ranking/
    DefaultSourceRanker.kt
    DefaultSourceScorer.kt
    DefaultSourceFilterEngine.kt
```

---

## `:integration:playback-media3`
```text
ai.shieldtv.app.integration.playback.media3
  engine/
    Media3PlaybackEngine.kt
    Media3PlayerFactory.kt
  mapper/
    PlaybackItemMapper.kt
  session/
    PlaybackSessionManager.kt
```

---

## 5. Minimal File Creation Order

If scaffolding by hand, this is the sensible order.

## Step 1 - shared foundations
Create:
- `:core:model`
- `:core:common`
- `:domain`

Populate first with:
- enums
- models
- repository interfaces
- provider/playback/ranker interfaces

## Step 2 - app shell + one screen loop
Create:
- `:app`
- `:feature:search`
- `:feature:details`

This gives visible movement quickly.

## Step 3 - integrations
Create:
- `:integration:metadata-tmdb`
- `:integration:debrid-realdebrid`
- `:integration:scrapers`
- `:integration:playback-media3`

## Step 4 - source + player features
Create:
- `:feature:sources`
- `:feature:player`
- `:feature:settings`

---

## 6. Minimum Viable Vertical Slice File Set

If we want the smallest meaningful implementation slice, these are the most important files.

## Must-have models
- `MediaRef.kt`
- `SearchResult.kt`
- `TitleDetails.kt`
- `SourceSearchRequest.kt`
- `SourceResult.kt`
- `SourceResolutionRequest.kt`
- `ResolvedStream.kt`
- `PlaybackItem.kt`
- `RealDebridAuthState.kt`
- `DeviceCodeFlow.kt`

## Must-have interfaces
- `MetadataRepository.kt`
- `SourceRepository.kt`
- `DebridRepository.kt`
- `PlaybackEngine.kt`
- `SourceProvider.kt`
- `SourceRanker.kt`

## Must-have use cases
- `SearchTitlesUseCase.kt`
- `GetTitleDetailsUseCase.kt`
- `FindSourcesUseCase.kt`
- `ResolveSourceUseCase.kt`
- `StartRealDebridDeviceFlowUseCase.kt`
- `BuildPlaybackItemUseCase.kt`

## Must-have UI pieces
- `SearchViewModel.kt`
- `SearchScreen.kt`
- `DetailsViewModel.kt`
- `DetailsScreen.kt`
- `SourcesViewModel.kt`
- `SourcesScreen.kt`
- `PlayerViewModel.kt`
- `PlayerScreen.kt`
- `SettingsViewModel.kt`
- `AccountSettingsScreen.kt`

## Must-have implementations
- `TmdbMetadataRepository.kt`
- `RealDebridRepositoryImpl.kt`
- `SourceRepositoryImpl.kt`
- `DefaultSourceNormalizer.kt`
- `DefaultSourceRanker.kt`
- `Media3PlaybackEngine.kt`

---

## 7. Dependency Direction Rules

These rules matter a lot.

## Allowed direction
- `:app` depends on features, domain, integration modules
- features depend on domain + core modules
- integrations depend on domain + core modules
- domain depends on core modules
- core modules depend on nothing or other lower-level core modules

## Disallowed direction
- domain depending on feature modules
- domain depending on integration implementations
- core:model depending on Android framework
- feature modules directly calling network APIs
- feature modules directly constructing Real-Debrid API clients

---

## 8. UI Technology Recommendation

For the TV UI layer:
- use **Kotlin + Jetpack Compose** if Mike wants a modern stack and is okay with TV focus work
- or use **classic Android TV/Leanback-style views** if speed/stability on TV focus is more important than modernity

### Current recommendation
Still slightly leaning toward:
- **Compose for app structure/screens**, if TV focus handling is acceptable
- but stay pragmatic: if TV focus gets annoying fast, fall back to view-based components where needed

This is one of the few places where implementation testing should beat theory.

---

## 9. DI Recommendation

A simple DI approach is fine at first.

### Good options
- Hilt if Mike wants standard Android DI
- lightweight manual dependency wiring if we want less magic early

### Current opinion
For a real app of this shape, **Hilt is probably worth it**.
There will be enough repositories/use cases/engines that manual wiring will get annoying.

---

## 10. Testing Plan at Scaffold Time

Do not wait too long to add tests around source logic.

## Unit test first targets
- `ReleaseInfoParser`
- `DefaultSourceNormalizer`
- `DefaultSourceFilterEngine`
- `DefaultSourceScorer`
- `DefaultSourceRanker`
- `RealDebridFileSelector`

## Why
These are the areas most likely to become brittle and painful if only tested manually.

---

## 11. Recommended Next Action After This Document

At this point, the next logical deliverable is:

### Option A - Implementation Roadmap
A task-by-task execution plan for building the app in order.

### Option B - Actual Scaffolding
Generate the initial project/module directory structure and starter files.

### My recommendation
Do **Option A first**, very briefly, then move straight into actual scaffolding.

Reason:
- the architecture is now clear enough
- the file layout is defined enough
- the only missing piece is execution order

Once that roadmap exists, scaffolding can happen without thrashing.

## 12. Implementation Progress Note

As the repo is scaffolded, prioritize these qualities:
- contracts before concrete logic
- compile-friendly shapes before deep feature work
- minimal manual wiring before committing to a DI framework
- keeping docs synchronized when code introduces a simpler interim shape
