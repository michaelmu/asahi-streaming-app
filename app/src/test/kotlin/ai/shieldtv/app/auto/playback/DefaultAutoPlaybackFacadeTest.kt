package ai.shieldtv.app.auto.playback

import ai.shieldtv.app.auto.model.AutoPlaybackResult
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import ai.shieldtv.app.core.model.media.EpisodeSummary
import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.media.TitleDetails
import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.Quality
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.repository.DebridRepository
import ai.shieldtv.app.domain.repository.MetadataRepository
import ai.shieldtv.app.domain.repository.SourceFetchProgress
import ai.shieldtv.app.domain.repository.SourceRepository
import ai.shieldtv.app.domain.usecase.auth.GetRealDebridAuthStateUseCase
import ai.shieldtv.app.domain.usecase.details.GetTitleDetailsUseCase
import ai.shieldtv.app.domain.usecase.sources.FindSourcesUseCase
import ai.shieldtv.app.playback.PlaybackMemoryStoreBase
import ai.shieldtv.app.settings.SourcePreferencesStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DefaultAutoPlaybackFacadeTest {

    @Test
    fun playMovie_prefersCachedOrDirectSourceForAuto() = runBlocking {
        val movieRef = movieRef("Arrival")
        val facade = buildFacade(
            authState = RealDebridAuthState(isLinked = true),
            sourceResults = listOf(
                source(movieRef, id = "rd-uncached", cacheStatus = CacheStatus.UNCACHED, debridService = DebridService.REAL_DEBRID),
                source(movieRef, id = "direct", cacheStatus = CacheStatus.DIRECT, debridService = DebridService.NONE),
                source(movieRef, id = "cached", cacheStatus = CacheStatus.CACHED, debridService = DebridService.REAL_DEBRID)
            )
        )

        val result = facade.playMovie(movieRef)

        assertTrue(result is AutoPlaybackResult.Ready)
        assertEquals("cached", (result as AutoPlaybackResult.Ready).source.id)
    }

    @Test
    fun playMovie_blocksWhenOnlyRealDebridSourcesExistAndAuthMissing() = runBlocking {
        val movieRef = movieRef("Heat")
        val facade = buildFacade(
            authState = RealDebridAuthState(isLinked = false),
            sourceResults = listOf(
                source(movieRef, id = "uncached-rd", cacheStatus = CacheStatus.UNCACHED, debridService = DebridService.REAL_DEBRID)
            )
        )

        val result = facade.playMovie(movieRef)

        assertTrue(result is AutoPlaybackResult.Blocked)
        assertEquals("Link Real-Debrid to play this item in Auto.", (result as AutoPlaybackResult.Blocked).userMessage)
    }

    @Test
    fun playShowDefault_prefersResumeEpisodeThenSelectsPlayableSource() = runBlocking {
        val showRef = showRef("Severance", tmdbId = "95396")
        val playbackMemoryFile = tempFile("auto-playback-memory")
        val playbackMemoryStore = PlaybackMemoryStoreBase(playbackMemoryFile)
        playbackMemoryStore.record(
            mediaRef = showRef,
            seasonNumber = 1,
            episodeNumber = 3,
            source = source(showRef, id = "remembered", cacheStatus = CacheStatus.CACHED, debridService = DebridService.REAL_DEBRID),
            positionMs = 120_000,
            durationMs = 2_400_000,
            progressPercent = 15
        )
        val watchedEpisodeKeys: (MediaIds) -> Set<String> = { emptySet() }
        val facade = buildFacade(
            authState = RealDebridAuthState(isLinked = true),
            titleDetails = TitleDetails(
                mediaRef = showRef,
                seasonCount = 1,
                episodeCount = 4,
                episodesBySeason = mapOf(
                    1 to listOf(
                        EpisodeSummary(1, 1, "Good News About Hell"),
                        EpisodeSummary(1, 2, "Half Loop"),
                        EpisodeSummary(1, 3, "In Perpetuity"),
                        EpisodeSummary(1, 4, "The You You Are")
                    )
                )
            ),
            sourceResults = listOf(
                source(showRef, id = "episode-cached", cacheStatus = CacheStatus.CACHED, debridService = DebridService.REAL_DEBRID, seasonNumber = 1, episodeNumber = 3)
            ),
            playbackMemoryStore = playbackMemoryStore,
            watchedEpisodeKeys = watchedEpisodeKeys
        )

        val result = facade.playShowDefault(showRef)

        assertTrue(result is AutoPlaybackResult.Ready)
        val ready = result as AutoPlaybackResult.Ready
        assertEquals(1, ready.source.seasonNumber)
        assertEquals(3, ready.source.episodeNumber)
    }

    private fun buildFacade(
        authState: RealDebridAuthState,
        sourceResults: List<SourceResult>,
        titleDetails: TitleDetails? = null,
        playbackMemoryStore: PlaybackMemoryStoreBase = PlaybackMemoryStoreBase(tempFile("auto-playback-memory-default")),
        watchedEpisodeKeys: (MediaIds) -> Set<String> = { emptySet() }
    ): DefaultAutoPlaybackFacade {
        val debridRepository = object : DebridRepository by unsupportedDebridRepository() {
            override suspend fun getAuthState(): RealDebridAuthState = authState
        }
        val metadataRepository = object : MetadataRepository by unsupportedMetadataRepository() {
            override suspend fun getTitleDetails(mediaRef: MediaRef): TitleDetails {
                return titleDetails ?: TitleDetails(mediaRef = mediaRef)
            }
        }
        val sourceRepository = object : SourceRepository by unsupportedSourceRepository() {
            override suspend fun findSources(
                request: ai.shieldtv.app.core.model.source.SourceSearchRequest,
                enabledProviderIds: Set<String>,
                onProgress: ((SourceFetchProgress) -> Unit)?,
                onIncrementalResults: ((ai.shieldtv.app.domain.repository.IncrementalSourceResult) -> Unit)?
            ): List<SourceResult> {
                return sourceResults.filter { source ->
                    source.mediaRef == request.mediaRef &&
                        source.seasonNumber == request.seasonNumber &&
                        source.episodeNumber == request.episodeNumber
                }
            }
        }

        val context = org.robolectric.RuntimeEnvironment.getApplication()
        context.getSharedPreferences("source_preferences", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        val sourcePreferencesStore = SourcePreferencesStore(context)

        return DefaultAutoPlaybackFacade(
            getRealDebridAuthStateUseCase = GetRealDebridAuthStateUseCase(debridRepository),
            getTitleDetailsUseCase = GetTitleDetailsUseCase(metadataRepository),
            findSourcesUseCase = FindSourcesUseCase(sourceRepository),
            sourceSelector = DefaultAutoSourceSelector(),
            showProgressResolver = DefaultAutoShowProgressResolver(),
            playbackMemoryStore = playbackMemoryStore,
            watchedEpisodeKeys = watchedEpisodeKeys,
            sourcePreferencesStore = sourcePreferencesStore,
            availableProviderIds = { setOf("provider") }
        )
    }

    private fun movieRef(title: String) = MediaRef(
        mediaType = MediaType.MOVIE,
        ids = MediaIds(tmdbId = "11", imdbId = null, tvdbId = null),
        title = title,
        year = 2020
    )

    private fun showRef(title: String, tmdbId: String) = MediaRef(
        mediaType = MediaType.SHOW,
        ids = MediaIds(tmdbId = tmdbId, imdbId = null, tvdbId = null),
        title = title,
        year = 2022
    )

    private fun source(
        mediaRef: MediaRef,
        id: String,
        cacheStatus: CacheStatus,
        debridService: DebridService,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ) = SourceResult(
        id = id,
        mediaRef = mediaRef,
        providerId = "provider",
        providerDisplayName = "Provider",
        providerKind = ProviderKind.SCRAPER,
        debridService = debridService,
        sourceSite = "example",
        url = "https://example.com/$id",
        displayName = id,
        quality = Quality.UNKNOWN,
        cacheStatus = cacheStatus,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber
    )

    private fun tempFile(prefix: String): File {
        val dir = createTempDir(prefix = prefix)
        return File(dir, "data.txt")
    }

}

private fun unsupportedDebridRepository(): DebridRepository = object : DebridRepository {
    override suspend fun getAuthState(): RealDebridAuthState = error("unused")
    override suspend fun startDeviceFlow() = error("unused")
    override suspend fun pollDeviceFlow(flow: ai.shieldtv.app.core.model.auth.DeviceCodeFlow): RealDebridAuthState = error("unused")
    override suspend fun resolve(source: SourceResult, seasonNumber: Int?, episodeNumber: Int?) = error("unused")
}

private fun unsupportedMetadataRepository(): MetadataRepository = object : MetadataRepository {
    override suspend fun search(query: String) = error("unused")
    override suspend fun getTitleDetails(mediaRef: MediaRef): TitleDetails = error("unused")
}

private fun unsupportedSourceRepository(): SourceRepository = object : SourceRepository {
    override suspend fun findSources(
        request: ai.shieldtv.app.core.model.source.SourceSearchRequest,
        enabledProviderIds: Set<String>,
        onProgress: ((SourceFetchProgress) -> Unit)?,
        onIncrementalResults: ((ai.shieldtv.app.domain.repository.IncrementalSourceResult) -> Unit)?
    ): List<SourceResult> = error("unused")
}
