# Shield Streaming App Research

Last updated: 2026-04-01 UTC

## Scope
Research baseline for a TV-first streaming/media app targeting NVIDIA Shield and other Android TV / Google TV devices.

Inspiration/codebases under review:
- `xbmc` (Kodi)
- `stremio-core`
- `FenlightAnonyMouse.github.io`

## Repo Intake Status

### 1) Kodi / XBMC (`xbmc`)
- Very large, mature native media-center codebase.
- Core positioning from README: couch-first home theater software with remote-friendly UX, broad codec/protocol support, metadata/library management, and strong extensibility via skins/add-ons.
- Build system: CMake.
- Documentation includes platform-specific build docs, including Android.

**Why it matters for us**
- Strong reference for Android TV / 10-foot UX patterns.
- Good source for feature benchmarking: library structure, remote navigation, playback affordances, settings hierarchy, protocol support.
- Also a cautionary example of how huge and legacy-heavy a full media-center can become.

**Immediate takeaways**
- Do not copy Kodi’s full surface area at v1.
- Borrow the TV ergonomics, not the entire product scope.
- Use it as a benchmark for playback polish and remote-control interaction.

### 2) Stremio Core (`stremio-core`)
- Rust workspace centered on reusable media-center logic.
- Explicit goals in README:
  - flexibility
  - correctness
  - minimal legacy cruft
- Architecture signals:
  - reusable core crate
  - add-on transport layer
  - state types inspired by Elm architecture
  - message/effects/update model
  - runtime/environment abstraction
- `stremio-core-web` exists as a WASM bridge package for web consumers.

**Why it matters for us**
- Strong architectural inspiration for separating app shell/UI from media-domain logic.
- Add-on-driven source integration model is highly relevant if Mike wants pluggable catalogs/providers.
- Rust core + platform-specific frontend is a credible path for shared logic across future clients.

**Technical notes observed so far**
- Rust edition: 2021.
- MSRV declared: 1.77.
- Release profile tuned for smaller output (`lto = true`, `opt-level = 's'`).
- Depends on serialization, URL/protocol parsing, local search, tracing, official add-ons package, and WASM tooling.
- `stremio-core-web` warns that dev builds may emit sensitive logs — useful reminder for our own debugging policy.

**Immediate takeaways**
- Strong candidate reference for domain-layer architecture.
- Message/effect/runtime pattern may map well to a TV app with async fetch, auth, library sync, and playback state.
- Worth studying whether a Rust core is helpful for our v1 or whether Kotlin-only is faster initially.

### 3) Fenlight repo (`FenlightAnonyMouse.github.io`)
- Mike clarified that **Fenlight is a Kodi plugin whose functionality is desired in the final streaming app**.
- This repo appears to function primarily as a package host/distribution repo, not an app source repo.
- Current visible contents are package artifacts like:
  - `plugin.video.fenlight-2.1.89.zip`
  - `plugin.video.fenlight-2.1.90.zip`
  - `plugin.video.fenlight-2.1.91.zip`
  - `plugin.video.fenlight-2.1.92.zip`
  - `plugin.video.fenlight-2.1.93.zip`
  - `plugin.video.fenlight-2.1.94.zip`
- README points to package hosting URL rather than documenting source architecture.

**Why it matters for us**
- Fenlight is no longer just vague inspiration; it is now a **functional reference target**.
- We should treat it as a source of requirements and workflows we may want to reproduce natively on Android TV.
- Even if this repo is package-oriented, unpacking the plugin artifacts may reveal:
  - user flows
  - provider integration patterns
  - metadata/search behavior
  - playback handoff logic
  - settings and account models

**Immediate takeaways**
- Not a strong source repo for architecture learning in its current form.
- Still very important as a **behavioral/product reference**.
- We should inventory Fenlight features explicitly and decide which ones belong in v1 vs later phases.

## Early Cross-Repo Conclusions

### Product direction
A good Shield app likely should sit **between** Kodi and Stremio in complexity:
- **Less sprawling than Kodi**
- **More opinionated and TV-native than generic mobile streaming apps**
- **Potentially modular/provider-driven like Stremio**

### Architecture direction
Promising options:
1. **Kotlin-first Android TV app**
   - fastest route to a polished Shield-native v1
   - easiest integration with Android playback stack (ExoPlayer/Media3)
   - least cross-platform reuse initially

2. **Rust core + Kotlin TV shell**
   - better long-term reuse and separation of concerns
   - attractive if provider logic/search/catalog state becomes complex
   - higher complexity up front

3. **Pure web/runtime shell on TV**
   - probably wrong for Shield-first unless there is a very strong reason
   - usually loses on TV-native feel, focus behavior, playback integration, and performance polish

### Current opinion
For a first real product push, **Kotlin/Compose for TV or Kotlin + classic Android TV views with Media3** feels more practical than immediately copying Stremio’s Rust-core strategy.
But Stremio is still the best inspiration so far for **domain boundaries** and **plugin/provider architecture**.

## Questions to Answer Next
1. What is the exact v1 feature set?
   - local library?
   - streaming links only?
   - add-on/provider model?
   - accounts/auth?
2. What playback stack should be used on Shield?
   - Media3 / ExoPlayer likely default
3. What UI framework should be used?
   - Compose for TV vs Leanback-style views
4. Do we need cross-platform logic sharing from day one?
5. What are the highest-value UX patterns to borrow from Kodi vs Stremio?

## Fenlight + CocoScrapers Extraction (Real-Debrid / search / streaming)

### High-level finding
Fenlight is not just a UI plugin — it is effectively a **media workflow orchestrator** layered on top of Kodi, debrid providers, metadata services, caches, and an external scraper module (CocoScrapers).

For the standalone Shield app, the key thing to copy is **the workflow**, not the Kodi plugin shape.

### Fenlight functional decomposition
From the extracted package structure, Fenlight is organized into several major concerns:
- `apis/`
  - provider/account integrations: Real-Debrid, Premiumize, AllDebrid, EasyDebrid, TorBox
  - metadata/services: TMDb, Trakt, IMDb, OMDb, etc.
- `caches/`
  - main cache, metadata cache, debrid cache, favorites/lists/settings caches
- `indexers/`
  - browsing and list-building for movies, shows, seasons, episodes, people, cloud views
- `modules/`
  - routing, search, playback, source orchestration, settings, downloader, watched state
- `scrapers/`
  - internal source providers such as RD cloud, Premiumize cloud, folders, Easynews, and external scraper bridge
- `windows/`
  - custom results dialogs, playback progress, settings windows, discover UI

### The actual source-search pipeline
Fenlight’s `Sources` module shows the core pipeline:
1. **Playback request starts** with media context
   - media type
   - TMDb id
   - season/episode if applicable
   - playback mode (autoplay/manual/background/next episode)
2. **Metadata is loaded**
   - movie or show/episode metadata is fetched and normalized
3. **Search info is built**
   - title
   - year
   - aliases
   - TMDb/IMDb/TVDb IDs
   - season/episode info
4. **Source providers are selected**
   - internal providers: RD cloud, PM cloud, AD cloud, TorBox cloud, folders, Easynews
   - external providers: CocoScrapers torrent providers
5. **Providers run concurrently**
   - internal and external scrapers are threaded
6. **Results are normalized**
   - quality
   - size
   - display name
   - hash/url/provider metadata
7. **Debrid cache status is checked**
   - especially for external torrent results
   - cached results are prioritized over uncached ones
8. **Results are filtered and sorted**
   - by resolution, size, audio, codec, HDR/DV/HEVC prefs, provider ranking, cloud preference
9. **User chooses a source or autoplay picks one**
10. **Resolution step turns source into final playable URL**
    - e.g. Real-Debrid magnet resolve or link unrestriction
11. **Playback starts** with resume/next-episode/still-watching logic layered around it

### Real-Debrid specifics extracted
Fenlight’s Real-Debrid integration includes:
- **device-code OAuth flow** with QR code / verification URL / user code
- token refresh and token persistence
- account info lookup
- cache checks for torrent hashes (`instantAvailability`)
- user cloud listing and details
- adding magnets
- selecting files in torrent
- resolving magnet to final downloadable stream URL
- unrestricted link resolution for direct links

Important workflow details:
- For external torrent hits, Fenlight prefers **cached** results.
- For magnet resolution, it:
  1. adds magnet to RD
  2. selects all files
  3. polls active transfer status briefly
  4. inspects candidate files
  5. tries to choose the correct movie/episode file
  6. unrestricts the selected link into a final streamable URL
- It explicitly filters for supported video extensions and tries to avoid junk/extras.
- It can optionally **store resolved items to the debrid cloud**.

### CocoScrapers role
CocoScrapers is basically a **provider pack / source adapter layer**.
Its module loader:
- enumerates provider modules from `sources_cocoscrapers/`
- checks whether each provider is enabled in settings
- returns provider name + source class

This means Fenlight uses CocoScrapers as a pluggable search backend, rather than embedding every torrent/site scraper directly inside the main addon.

### CocoScrapers settings/product implications
The settings file reveals useful product assumptions:
- users can enable/disable individual providers
- there are batch controls for toggling all providers
- undesirable terms/filters are first-class
- foreign audio filtering exists
- some providers have custom auth / custom URL support
- debugging and provider diagnostics matter enough to expose in settings

Default-enabled providers in the sampled settings include some sources like:
- `bitsearch`
- `eztv`
- `torrentdownload`
- `torrentio`
- `torrentquest`

So the product model is clearly:
- multiple scrape providers
- user-tunable provider selection
- aggressive filtering before playback choice

### Key UX/feature behaviors worth preserving in the standalone app
These are the Fenlight behaviors that look most important for a Shield-native version:

#### Must preserve in v1 or near-v1
- Real-Debrid device auth flow
- search by movie/show title and episode context
- concurrent multi-provider source lookup
- cached-vs-uncached awareness
- result normalization and ranking
- source picker UI with quality / size / extra info
- final URL resolution via debrid before playback
- resume playback support

#### Strong candidates for v1.1+
- debrid cloud browsing
- provider-level enable/disable controls
- advanced filters (codec/audio/HDR/DV/size)
- next-episode and autoscrape workflows
- pack handling for season/show torrents
- add magnet to cloud manually

#### Probably later / optional
- every debrid provider Fenlight supports
- full Kodi-style window proliferation
- deep settings parity with every plugin toggle
- Easynews, folders, and all side integrations unless they are core to Mike’s plan

### Architectural lesson for our standalone app
Fenlight’s current architecture is shaped by Kodi constraints:
- router mode strings
- plugin invocations
- window properties for inter-module communication
- custom dialogs layered over Kodi

For the Shield app, we should translate that into cleaner app-native boundaries:
- **UI layer**: TV screens, source picker, playback, search, settings
- **Domain layer**: media search, source orchestration, ranking, filtering, playback decisioning
- **Provider layer**:
  - metadata providers (TMDb/Trakt/etc.)
  - debrid providers (Real-Debrid first)
  - scraper providers (CocoScrapers-inspired adapters)
- **Persistence/cache layer**:
  - auth tokens
  - search history
  - cached source results
  - playback progress / watch state

### Concrete recommendation emerging from this extraction
For Mike’s app, a strong first architecture would be:
- **Android TV native app shell**
- **Media3/ExoPlayer playback**
- **Source orchestration engine modeled after Fenlight’s `Sources` flow**
- **Provider plugin/adapter system inspired by CocoScrapers**
- **Real-Debrid as the first-class account integration**

That gets the useful Fenlight behavior without dragging Kodi’s plugin architecture into the new app.

## Fenlight Result Model + Playback UX Extraction

### Result model observations
Fenlight’s source/result objects are richer than a simple URL list. A useful standalone app should preserve roughly this shape.

Common result fields observed across Fenlight processing/UI:
- `name`
- `display_name`
- `url`
- `hash`
- `provider`
- `scrape_provider`
- `source`
- `quality`
- `size`
- `size_label`
- `extraInfo`
- `cache_provider`
- `debrid`
- optional flags such as package/season-pack/show-pack, seeders, direct-debrid-link, folder/cloud ids

### What the UI actually surfaces
Fenlight’s source selection UI displays and/or filters by:
- provider / debrid service
- source site
- quality tier (4K / 1080p / 720p / SD / prerelease)
- size label
- `extraInfo` tags such as:
  - HDR / Dolby Vision / Hybrid
  - HEVC / AV1 / AVC
  - Atmos / DTS / AAC / channel count
  - WEB / BLURAY / REMUX / PACK / SUBS / MULTI-LANG
- cached vs uncached state
- seeders for uncached items

That means the standalone app should not treat source results as just playback URLs. They need to be modeled as **decision objects** for ranking and UI presentation.

### Suggested standalone result model
Proposed app-native model:

```text
SourceResult
- id
- mediaRef
- titleLabel
- providerId              # torrentio / bitsearch / rd_cloud / etc.
- providerKind            # internal_cloud / external_scraper / direct
- debridService           # realdebrid / none / future others
- sourceSite              # source origin label
- linkType                # magnet / http / cloud / direct
- url
- infoHash
- quality                 # 4k / 1080p / 720p / sd / cam / scr / tele
- sizeBytes
- sizeLabel
- videoFlags[]            # hdr / dv / hybrid / hevc / av1 / remux / bluray / web
- audioFlags[]            # atmos / truehd / dts-hd / aac / 6ch / etc.
- languageFlags[]         # multi-lang / subs / foreign-audio
- packageType             # none / single / season / show
- seeders?
- cacheStatus             # cached / uncached / unchecked / direct
- rankScore
- rawMetadata             # provider-specific payload
```

This is cleaner than Fenlight’s current dict soup and gives the Android app a stable contract.

### Important ranking behavior to preserve
Fenlight’s ranking/filtering logic is not trivial. It includes:
- provider ranking
- resolution ranking
- size ranking
- cache-first behavior
- audio/codec filters
- preferred tags sorted to top
- cloud/folder results optionally sorted to top
- quality caps and total result caps

So the new app needs a **ranking engine**, not just a list merge.

## Fenlight Playback Behavior Worth Reproducing

### Playback pipeline
Fenlight’s playback path does more than “play URL”:
1. resolve a playable URL from the chosen source
2. show progress / resolver UI
3. detect playback success/failure
4. support resume/start over
5. track progress/bookmarks
6. mark watched near completion
7. prepare next-episode behavior for TV content

### TV-appropriate behaviors worth keeping
- resume from previous position
- autoplay / prep next episode
- still-watching checks
- source fallback if first resolution/playback fails
- metadata-rich playback session (title, poster, IDs, artwork)

### Shield-native translation
Kodi-specific playback windows should become Android-TV-native constructs:
- source scraping progress screen/dialog
- source resolver status sheet/dialog
- resume prompt
- next-episode overlay
- playback error/fallback flow

## Proposed v1 Feature Slice

### v1 core scope
The most sensible first slice now looks like:
- Android TV native app shell
- search for movie/show titles
- metadata lookup for titles/episodes
- Real-Debrid account linking via device flow
- provider search through a scraper abstraction layer
- cache-aware source results list
- source picker with quality/size/info tags
- resolve selected source to stream URL
- playback with Media3
- resume/bookmark support

### v1.1 scope
- episode autoplay / next-up
- debrid cloud browsing
- more advanced filters and sorting controls
- provider management UI
- search history
- watch-state sync / Trakt-like integrations

### Later scope
- additional debrid services beyond Real-Debrid
- advanced pack browsing
- discover/home recommendations
- parity with more Fenlight edge-case settings

## Concrete Architecture Recommendation

### Recommendation: native Android TV app, Kotlin-first
Current best recommendation:
- **Kotlin Android app first**
- **Media3 / ExoPlayer** for playback
- **clean modular domain architecture** inspired by Stremio/Fenlight
- **provider adapter system** inspired by CocoScrapers
- **Real-Debrid first**, others later

### Why Kotlin-first beats copying Fenlight/Kodi literally
- Kodi plugin architecture is a workaround for running inside Kodi
- Fenlight uses router strings, global window properties, and plugin callbacks because Kodi forces that shape
- On Android TV we can do this cleanly with typed models, coroutines, proper state flows, and native playback APIs

### Suggested module layout

```text
app-tv/
  ui/
    home/
    search/
    details/
    sources/
    player/
    settings/
  navigation/
  playback/
  auth/

core-domain/
  model/
  usecase/
  ranking/
  filtering/
  playback/

core-data/
  repositories/
  cache/
  database/
  preferences/

integrations-metadata/
  tmdb/
  trakt/           # maybe later

integrations-debrid/
  realdebrid/

integrations-scrapers/
  api/
  cocoscrapers-inspired/
  providers/

core-network/
core-common/
```

### Core domain flows
Important use cases to define explicitly:
- `SearchTitlesUseCase`
- `GetTitleDetailsUseCase`
- `FindSourcesUseCase`
- `RankSourcesUseCase`
- `ResolveSourceUseCase`
- `StartPlaybackUseCase`
- `SavePlaybackProgressUseCase`
- `GetResumePointUseCase`
- `LinkRealDebridDeviceUseCase`
- `GetRealDebridCachedStatusUseCase`

### State management direction
This is where Stremio is useful conceptually.
We likely want:
- unidirectional state flow per screen
- side-effect handling for network/auth/playback operations
- typed result models and reducers/actions/events

In Kotlin terms, that probably means:
- ViewModel + StateFlow
- intents/actions
- reducers/use cases
- repository boundaries

We probably **do not** need Rust on day one unless we decide early that source logic must be shared beyond Android.

## Updated Cross-Repo Read
- **Kodi** = environment/UX reference
- **Fenlight** = product workflow and result-model reference
- **CocoScrapers** = provider adapter/reference
- **Stremio Core** = clean architecture inspiration for domain separation and async state management

## Additional Stremio Repo Review

### `stremio-core-kotlin`
This repo is more relevant than expected.

Observed structure:
- Kotlin Multiplatform library
- Android target
- Rust bridge underneath
- protobuf bridge layer (`stremio-core-protobuf`)
- Android NDK + Rust build integration

What this tells us:
- Stremio’s Kotlin path is **not** a pure Kotlin reimplementation of the core.
- It is a **Kotlin-facing wrapper around Rust core logic**.
- They use protobuf + native libraries to bridge Rust into Kotlin consumers.

Why this matters for Mike’s app:
- It validates that Stremio itself sees value in keeping core logic outside the UI shell.
- But it also confirms the cost: Rust toolchain, NDK integration, bridging complexity, release overhead.

Current conclusion:
- Good architectural inspiration.
- Probably **not** the right move for v1 unless we already know we need shared core logic across multiple frontends.
- Still worth studying for boundary design and how they expose core state into Kotlin.

### `stremio-video`
This repo is also useful, though in a different way.

Observed structure:
- JS abstraction over multiple playback implementations
- different implementations for HTML, Shell, Tizen, webOS, Titan, Vidaa, Chromecast, YouTube, iframe-based playback
- wrappers like:
  - `withStreamingServer`
  - `withHTMLSubtitles`
  - `withVideoParams`

What this tells us:
- Stremio treats playback as a **platform abstraction problem**, not a single player component.
- They separate:
  - implementation selection
  - streaming-server mediation
  - subtitle rendering
  - video parameter adaptation

Why this matters for Mike’s app:
- On Android TV, we likely only need one primary playback implementation (Media3), so we do **not** need this level of player abstraction at first.
- But the layering idea is useful:
  - normalize incoming stream info
  - adapt it to playback requirements
  - optionally mediate through a streaming layer if needed
  - attach subtitles/track metadata as a separate concern

Current conclusion:
- Useful conceptually for **playback pipeline boundaries**.
- Less directly reusable than `stremio-core-kotlin` for Shield-specific implementation.

### `stremio-shell`
This appears to be a native desktop shell around the Stremio web UI/runtime.

Observed signals:
- Qt/C++ shell project
- packaged runtime concerns
- mpv integration
- autoupdater
- development mode that loads a web UI URL
- shell/host orchestration concerns rather than domain logic

Why it matters:
- Confirms Stremio’s pattern of separating:
  - app shell / host runtime
  - playback/runtime integration
  - web/core product logic
- It is useful as a **host-shell architecture reference**, but not a direct template for an Android TV app.

Current conclusion:
- Worth noting, but lower value than `stremio-core-kotlin` and `stremio-video` for the current project.

### `stremio-web`
This one is useful, but mostly for **product structure and app flow**, not for Shield-native implementation details.

Observed structure:
- React/web app
- route-driven app layout: Board, Discover, Search, MetaDetails, Player, Settings, Library, Addons, Calendar
- services layer including Core, Shell, Chromecast
- typed models for stream/meta/library/video entities
- reusable components and platform hooks

Why it matters:
- It shows how Stremio organizes the **product surface** around the core.
- It likely contains useful patterns for:
  - board/home information architecture
  - discover/details/player handoff
  - service separation between UI and backend/core runtime
- It is less useful for Android-TV-native focus handling and playback implementation, but good for planning screen/module boundaries.

Current conclusion:
- Worth studying for **screen architecture, flow design, and service boundaries**.
- Probably more useful for product planning than low-level implementation.

### `media`
This is not a Stremio repo in the same sense; it is the AndroidX Media / Media3 codebase.

Observed structure:
- huge Android media framework repo
- ExoPlayer / Media3 libraries
- cast, datasource, decoder, session, transformer, UI, demo apps
- effectively the playback foundation we would likely rely on

Why it matters:
- Very relevant as a **reference library**, but not something we should read exhaustively.
- The main value is:
  - confirming available Media3 modules
  - finding sample apps/demos for playback/session/TV integration
  - checking supported playback/data-source patterns

Current conclusion:
- Important as a **technical reference and sample source**.
- Not a repo to “study like product architecture”; more a toolkit/documentation/reference base.

### Updated verdict on the extra repos
Most worth deeper study:
1. `stremio-core-kotlin`
2. `stremio-video`
3. `stremio-web`
4. `stremio-shell`

Reference/tooling repo rather than product architecture target:
- `media`

## Updated Architecture View After Extra Stremio Repos
The Stremio ecosystem reinforces a pattern:
- **core logic is separate from UI shell**
- **playback is treated as a separate subsystem**
- **host/runtime concerns are isolated from domain concerns**

That aligns very well with what the Fenlight extraction suggests we should do.

### Refined recommendation
For Mike’s Shield app:
- keep **UI**, **domain/source orchestration**, and **playback** as separate modules
- treat **Real-Debrid + source resolution** as a domain/integration subsystem, not UI logic
- keep playback behind a clean boundary even if v1 only uses Media3
- avoid Rust/NDK complexity until there is a clear multi-platform payoff

## Best Current Opinion
If the goal is a real standalone Shield app with Fenlight-like functionality, the smartest first move is:
1. **build a native Android TV shell**
2. **implement Real-Debrid auth + resolution first**
3. **implement one clean scraper adapter layer**
4. **model source results as first-class rich objects**
5. **treat ranking/filtering as a dedicated engine**
6. **keep playback as a separate subsystem with a clean interface**

That is much more likely to produce a usable app than trying to transplant the Kodi addon whole.

## Next Research Pass
- Inspect Kodi Android-specific docs and app structure.
- Inspect `stremio-core-kotlin` deeper for boundary/state exposure patterns.
- Identify the minimum viable scraper/provider abstraction for a standalone app.
- Turn this research into a concrete implementation plan and milestone breakdown.
