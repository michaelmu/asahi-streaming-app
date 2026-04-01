# Shield Streaming App - Domain Contracts

Last updated: 2026-04-01 UTC

## Purpose
Define the first-pass domain models and interfaces for the standalone Shield app.

This document is intentionally implementation-oriented.
It should make it easier to scaffold the project without inventing architecture on the fly.

Note: as scaffolding progresses, some contracts may be introduced in a minimal form first and then expanded toward the fuller shapes described here.

---

## 1. Design Principles

### 1. Typed over dynamic
Fenlight works with large dict-like payloads because Kodi/Python/plugin ergonomics push it that way.
We should not repeat that.

### 2. Separate raw provider payloads from app-level models
Scraper outputs will vary wildly.
Normalize them at the integration boundary.

### 3. UI consumes stable app contracts
Screens should depend on app models, not API responses.

### 4. Resolution and playback are separate steps
A source result is not yet a playable stream.

### 5. Debrid is a subsystem, not a screen concern
Auth, cache check, and resolution belong below UI.

---

## 2. Core Enums / Value Types

## `MediaType`
```kotlin
enum class MediaType {
    MOVIE,
    SHOW,
    SEASON,
    EPISODE
}
```

## `Quality`
```kotlin
enum class Quality {
    UHD_4K,
    FHD_1080P,
    HD_720P,
    SD,
    SCR,
    CAM,
    TELE,
    UNKNOWN
}
```

## `CacheStatus`
```kotlin
enum class CacheStatus {
    CACHED,
    UNCACHED,
    UNCHECKED,
    DIRECT
}
```

## `ProviderKind`
```kotlin
enum class ProviderKind {
    SCRAPER,
    DEBRID_CLOUD,
    DIRECT,
    AGGREGATOR
}
```

## `DebridService`
```kotlin
enum class DebridService {
    REAL_DEBRID,
    NONE
}
```

## `LinkType`
```kotlin
enum class LinkType {
    MAGNET,
    HTTP,
    HTTPS,
    CLOUD,
    DIRECT
}
```

## `PackageType`
```kotlin
enum class PackageType {
    NONE,
    SINGLE,
    SEASON_PACK,
    SHOW_PACK
}
```

## `PlaybackMode`
```kotlin
enum class PlaybackMode {
    MANUAL,
    AUTOPLAY,
    RESUME,
    NEXT_EPISODE
}
```

## `VideoFlag`
```kotlin
enum class VideoFlag {
    HDR,
    DOLBY_VISION,
    HYBRID_HDR_DV,
    HEVC,
    AV1,
    AVC,
    REMUX,
    BLURAY,
    WEB,
    IMAX,
    UPSCALED,
    SUBS,
    MULTI_LANG
}
```

## `AudioFlag`
```kotlin
enum class AudioFlag {
    ATMOS,
    TRUEHD,
    DTS_X,
    DTS_HD,
    DTS,
    DD_PLUS,
    DD,
    AAC,
    OPUS,
    MP3,
    CH_2,
    CH_6,
    CH_7,
    CH_8
}
```

---

## 3. Media Models

## `MediaIds`
```kotlin
data class MediaIds(
    val tmdbId: String?,
    val imdbId: String?,
    val tvdbId: String?
)
```

## `MediaRef`
```kotlin
data class MediaRef(
    val mediaType: MediaType,
    val ids: MediaIds,
    val title: String,
    val originalTitle: String? = null,
    val year: Int? = null
)
```

## `EpisodeRef`
```kotlin
data class EpisodeRef(
    val show: MediaRef,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episodeTitle: String? = null
)
```

## `SearchQuery`
```kotlin
data class SearchQuery(
    val text: String,
    val mediaType: MediaType? = null
)
```

## `SearchResult`
```kotlin
data class SearchResult(
    val mediaRef: MediaRef,
    val subtitle: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val badges: List<String> = emptyList()
)
```

## `TitleDetails`
```kotlin
data class TitleDetails(
    val mediaRef: MediaRef,
    val overview: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val logoUrl: String?,
    val genres: List<String>,
    val runtimeMinutes: Int?,
    val rating: Double?,
    val releaseDate: String?,
    val seasons: List<SeasonSummary> = emptyList(),
    val episodes: List<EpisodeSummary> = emptyList()
)
```

## `SeasonSummary`
```kotlin
data class SeasonSummary(
    val seasonNumber: Int,
    val title: String?,
    val episodeCount: Int?,
    val posterUrl: String?
)
```

## `EpisodeSummary`
```kotlin
data class EpisodeSummary(
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String?,
    val overview: String?,
    val thumbnailUrl: String?,
    val airDate: String?
)
```

---

## 4. Source Models

## `SourceSearchRequest`
```kotlin
data class SourceSearchRequest(
    val mediaRef: MediaRef,
    val playbackMode: PlaybackMode,
    val aliases: List<String> = emptyList(),
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val totalSeasons: Int? = null,
    val preferredYear: Int? = null,
    val filters: SourceFilters = SourceFilters()
)
```

## `SourceFilters`
```kotlin
data class SourceFilters(
    val allowedQualities: Set<Quality> = emptySet(),
    val excludedVideoFlags: Set<VideoFlag> = emptySet(),
    val excludedAudioFlags: Set<AudioFlag> = emptySet(),
    val minSizeBytes: Long? = null,
    val maxSizeBytes: Long? = null,
    val requireCachedOnly: Boolean = false
)
```

## `SourceResult`
```kotlin
data class SourceResult(
    val id: String,
    val mediaRef: MediaRef,
    val providerId: String,
    val providerDisplayName: String,
    val providerKind: ProviderKind,
    val debridService: DebridService,
    val sourceSite: String?,
    val linkType: LinkType,
    val url: String,
    val infoHash: String? = null,
    val displayName: String,
    val quality: Quality,
    val sizeBytes: Long? = null,
    val sizeLabel: String? = null,
    val videoFlags: Set<VideoFlag> = emptySet(),
    val audioFlags: Set<AudioFlag> = emptySet(),
    val languageTags: Set<String> = emptySet(),
    val packageType: PackageType = PackageType.NONE,
    val seeders: Int? = null,
    val cacheStatus: CacheStatus,
    val score: Double? = null,
    val rawMetadata: Map<String, String> = emptyMap()
)
```

## `RankedSources`
```kotlin
data class RankedSources(
    val primary: List<SourceResult>,
    val fallback: List<SourceResult>
)
```

## `SourceResolutionRequest`
```kotlin
data class SourceResolutionRequest(
    val source: SourceResult,
    val mediaRef: MediaRef,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val storeToDebridCloud: Boolean = false
)
```

## `ResolvedSubtitle`
```kotlin
data class ResolvedSubtitle(
    val url: String,
    val language: String?,
    val mimeType: String?
)
```

## `ResolvedStream`
```kotlin
data class ResolvedStream(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val mimeType: String? = null,
    val subtitles: List<ResolvedSubtitle> = emptyList(),
    val source: SourceResult
)
```

---

## 5. Real-Debrid Models

## `DeviceCodeFlow`
```kotlin
data class DeviceCodeFlow(
    val verificationUrl: String,
    val userCode: String,
    val qrCodeUrl: String?,
    val expiresInSeconds: Int,
    val pollIntervalSeconds: Int
)
```

## `RealDebridAccount`
```kotlin
data class RealDebridAccount(
    val username: String,
    val email: String? = null,
    val isPremium: Boolean,
    val expirationDate: String? = null
)
```

## `RealDebridAuthState`
```kotlin
data class RealDebridAuthState(
    val isLinked: Boolean,
    val account: RealDebridAccount? = null,
    val authInProgress: Boolean = false,
    val lastError: String? = null
)
```

## `CacheCheckRequest`
```kotlin
data class CacheCheckRequest(
    val infoHashes: List<String>
)
```

## `CacheCheckResult`
```kotlin
data class CacheCheckResult(
    val cachedHashes: Set<String>,
    val uncheckedHashes: Set<String> = emptySet()
)
```

---

## 6. Playback Models

## `ResumeInfo`
```kotlin
data class ResumeInfo(
    val positionMs: Long,
    val durationMs: Long,
    val percentWatched: Float
)
```

## `PlaybackItem`
```kotlin
data class PlaybackItem(
    val mediaRef: MediaRef,
    val title: String,
    val subtitle: String? = null,
    val artworkUrl: String? = null,
    val stream: ResolvedStream,
    val resumeInfo: ResumeInfo? = null
)
```

## `PlaybackProgress`
```kotlin
data class PlaybackProgress(
    val mediaRef: MediaRef,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtEpochMs: Long,
    val completed: Boolean
)
```

## `PlaybackState`
```kotlin
data class PlaybackState(
    val isBuffering: Boolean,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val errorMessage: String? = null
)
```

---

## 7. Repository Interfaces

## `MetadataRepository`
```kotlin
interface MetadataRepository {
    suspend fun search(query: SearchQuery): List<SearchResult>
    suspend fun getTitleDetails(mediaRef: MediaRef): TitleDetails
    suspend fun getEpisodes(show: MediaRef, seasonNumber: Int): List<EpisodeSummary>
}
```

## `SourceRepository`
```kotlin
interface SourceRepository {
    suspend fun findSources(request: SourceSearchRequest): List<SourceResult>
}
```

## `SourcePreferencesRepository`
```kotlin
interface SourcePreferencesRepository {
    suspend fun getFilters(): SourceFilters
    suspend fun saveFilters(filters: SourceFilters)
    suspend fun getEnabledProviderIds(): Set<String>
    suspend fun setProviderEnabled(providerId: String, enabled: Boolean)
}
```

## `DebridRepository`
```kotlin
interface DebridRepository {
    suspend fun getAuthState(): RealDebridAuthState
    suspend fun startDeviceFlow(): DeviceCodeFlow
    suspend fun pollDeviceFlow(flow: DeviceCodeFlow): RealDebridAuthState
    suspend fun unlink()
    suspend fun getAccount(): RealDebridAccount
    suspend fun checkCached(request: CacheCheckRequest): CacheCheckResult
    suspend fun resolve(request: SourceResolutionRequest): ResolvedStream
}
```

## `PlaybackProgressRepository`
```kotlin
interface PlaybackProgressRepository {
    suspend fun getResumeInfo(mediaRef: MediaRef, seasonNumber: Int? = null, episodeNumber: Int? = null): ResumeInfo?
    suspend fun saveProgress(progress: PlaybackProgress)
    suspend fun markCompleted(mediaRef: MediaRef, seasonNumber: Int? = null, episodeNumber: Int? = null)
}
```

## `SearchHistoryRepository`
```kotlin
interface SearchHistoryRepository {
    suspend fun getRecentQueries(): List<String>
    suspend fun addQuery(query: String)
    suspend fun clear()
}
```

---

## 8. Provider Contracts

## `SourceProvider`
```kotlin
interface SourceProvider {
    val id: String
    val displayName: String
    val kind: ProviderKind

    suspend fun search(request: SourceSearchRequest): List<RawProviderSource>
}
```

## `RawProviderSource`
```kotlin
data class RawProviderSource(
    val providerId: String,
    val title: String,
    val url: String,
    val infoHash: String? = null,
    val sizeBytes: Long? = null,
    val seeders: Int? = null,
    val extra: Map<String, String> = emptyMap()
)
```

## `SourceNormalizer`
```kotlin
interface SourceNormalizer {
    fun normalize(
        request: SourceSearchRequest,
        provider: SourceProvider,
        raw: RawProviderSource
    ): SourceResult
}
```

---

## 9. Ranking / Filtering Contracts

## `SourceRanker`
```kotlin
interface SourceRanker {
    fun rank(sources: List<SourceResult>, filters: SourceFilters): RankedSources
}
```

## `SourceScorer`
```kotlin
interface SourceScorer {
    fun score(source: SourceResult, filters: SourceFilters): Double
}
```

## `SourceFilterEngine`
```kotlin
interface SourceFilterEngine {
    fun apply(sources: List<SourceResult>, filters: SourceFilters): List<SourceResult>
}
```

---

## 10. Playback Contracts

## `PlaybackEngine`
```kotlin
interface PlaybackEngine {
    suspend fun prepare(item: PlaybackItem)
    fun play()
    fun pause()
    fun stop()
    fun observeState(): kotlinx.coroutines.flow.Flow<PlaybackState>
}
```

## `StreamResolver`
```kotlin
interface StreamResolver {
    suspend fun resolve(request: SourceResolutionRequest): ResolvedStream
}
```

---

## 11. Core Use Case Signatures

## Metadata
```kotlin
class SearchTitlesUseCase(
    private val metadataRepository: MetadataRepository,
    private val searchHistoryRepository: SearchHistoryRepository
)

class GetTitleDetailsUseCase(
    private val metadataRepository: MetadataRepository
)
```

## Sources
```kotlin
class FindSourcesUseCase(
    private val sourceRepository: SourceRepository,
    private val sourcePreferencesRepository: SourcePreferencesRepository,
    private val sourceRanker: SourceRanker
)

class ResolveSourceUseCase(
    private val debridRepository: DebridRepository
)
```

## Debrid
```kotlin
class StartRealDebridDeviceFlowUseCase(
    private val debridRepository: DebridRepository
)

class PollRealDebridDeviceFlowUseCase(
    private val debridRepository: DebridRepository
)
```

## Playback
```kotlin
class BuildPlaybackItemUseCase(
    private val playbackProgressRepository: PlaybackProgressRepository
)

class SavePlaybackProgressUseCase(
    private val playbackProgressRepository: PlaybackProgressRepository
)
```

---

## 12. Screen State Contracts (v1)

## `SearchUiState`
```kotlin
data class SearchUiState(
    val query: String = "",
    val recentQueries: List<String> = emptyList(),
    val loading: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val error: String? = null
)
```

## `DetailsUiState`
```kotlin
data class DetailsUiState(
    val loading: Boolean = false,
    val details: TitleDetails? = null,
    val error: String? = null
)
```

## `SourcesUiState`
```kotlin
data class SourcesUiState(
    val loading: Boolean = false,
    val title: String = "",
    val sources: List<SourceResult> = emptyList(),
    val fallbackSources: List<SourceResult> = emptyList(),
    val filters: SourceFilters = SourceFilters(),
    val error: String? = null
)
```

## `PlayerUiState`
```kotlin
data class PlayerUiState(
    val loading: Boolean = false,
    val playbackState: PlaybackState? = null,
    val error: String? = null
)
```

## `AccountUiState`
```kotlin
data class AccountUiState(
    val loading: Boolean = false,
    val authState: RealDebridAuthState = RealDebridAuthState(isLinked = false),
    val deviceFlow: DeviceCodeFlow? = null,
    val error: String? = null
)
```

---

## 13. v1 Boundary Decisions

### Keep in v1
- TMDb metadata
- Real-Debrid only
- limited provider set
- source ranking/filtering basics
- Media3 playback
- bookmarks/resume

### Defer
- multiple debrid services
- Trakt sync
- full discover/home ecosystem
- advanced next-episode logic
- cloud browsing parity
- Rust shared core

---

## 14. Best Next Step After This Document

Now that these contracts exist, the next sensible move is:
1. convert them into a **repo/module scaffolding plan**
2. identify the **minimum set of actual Kotlin files/packages**
3. then scaffold the app project around these interfaces

That would turn planning into implementation without losing architectural clarity.
