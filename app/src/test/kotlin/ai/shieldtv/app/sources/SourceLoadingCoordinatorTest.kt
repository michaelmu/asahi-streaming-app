package ai.shieldtv.app.sources

import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.Quality
import ai.shieldtv.app.core.model.source.SourceFilters
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.repository.SourceFetchProgress
import ai.shieldtv.app.domain.usecase.sources.FindSourcesUseCase
import ai.shieldtv.app.feature.sources.presentation.SourcesPresenter
import ai.shieldtv.app.feature.sources.presentation.SourcesViewModel
import ai.shieldtv.app.settings.SourcePreferences
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SourceLoadingCoordinatorTest {
    private val dispatcher = StandardTestDispatcher()

    @Test
    fun load_filters_rd_only_sources_when_auth_not_linked_and_reports_progress() = runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
            val owner = TestLifecycleOwner()
            val coordinator = SourceLoadingCoordinator(
                lifecycleScope = owner.lifecycleScope,
                sourcesViewModel = SourcesViewModel(
                    FakeSourcesPresenter(
                        providedSources = listOf(
                            source("rd", DebridService.REAL_DEBRID),
                            source("direct", DebridService.NONE)
                        ),
                        progressEvents = listOf(
                            SourceFetchProgress(
                                providerId = "torrentio",
                                providerDisplayName = "Torrentio",
                                state = SourceFetchProgress.State.STARTED
                            ),
                            SourceFetchProgress(
                                providerId = "torrentio",
                                providerDisplayName = "Torrentio",
                                state = SourceFetchProgress.State.COMPLETED,
                                resultCount = 2
                            )
                        )
                    )
                )
            )

            var startedCalled = false
            val progressSnapshots = mutableListOf<List<SourceFetchProgress>>()
            var result: SourceLoadResult? = null

            coordinator.load(
                request = SourceLoadRequest(
                    mediaRef = mediaRef(),
                    authLinked = false,
                    preferences = SourcePreferences(),
                    availableProviderIds = setOf("test", "torrentio"),
                    filters = SourceFilters(),
                    searchLabel = "The Matrix"
                ),
                onStarted = { startedCalled = true },
                onProgressUpdated = { progressSnapshots += it },
                onCompleted = { result = it }
            )

            advanceUntilIdle()

            assertTrue(startedCalled)
            assertEquals(2, progressSnapshots.size)
            assertEquals(1, result?.sources?.size)
            assertTrue(result?.sources?.all { it.debridService == DebridService.NONE } == true)
            assertEquals(SourceFetchProgress.State.COMPLETED, coordinator.currentProgress().last().state)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun load_keeps_rd_sources_when_auth_is_linked() = runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
            val owner = TestLifecycleOwner()
            val coordinator = SourceLoadingCoordinator(
                lifecycleScope = owner.lifecycleScope,
                sourcesViewModel = SourcesViewModel(
                    FakeSourcesPresenter(
                        providedSources = listOf(
                            source("direct", DebridService.NONE),
                            source("rd", DebridService.REAL_DEBRID)
                        )
                    )
                )
            )

            var result: SourceLoadResult? = null
            coordinator.load(
                request = SourceLoadRequest(
                    mediaRef = mediaRef(),
                    authLinked = true,
                    preferences = SourcePreferences(),
                    availableProviderIds = setOf("test", "torrentio"),
                    filters = SourceFilters(),
                    searchLabel = "The Matrix"
                ),
                onStarted = {},
                onProgressUpdated = {},
                onCompleted = { result = it }
            )

            advanceUntilIdle()

            assertEquals(2, result?.sources?.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun source(id: String, debridService: DebridService): SourceResult {
        return SourceResult(
            id = id,
            mediaRef = mediaRef(),
            providerId = "test",
            providerDisplayName = "Test",
            providerKind = ProviderKind.SCRAPER,
            debridService = debridService,
            sourceSite = "Test",
            url = "https://example.com/$id",
            displayName = id,
            quality = Quality.FHD_1080P,
            cacheStatus = CacheStatus.UNCHECKED
        )
    }

    private fun mediaRef(): MediaRef {
        return MediaRef(
            mediaType = MediaType.MOVIE,
            ids = MediaIds(tmdbId = "603", imdbId = "tt0133093", tvdbId = null),
            title = "The Matrix",
            year = 1999
        )
    }
}

private class FakeSourcesPresenter(
    private val providedSources: List<SourceResult>,
    private val progressEvents: List<SourceFetchProgress> = emptyList()
) : SourcesPresenter(
    FindSourcesUseCase(FakeSourceRepository())
) {
    override suspend fun load(
        request: ai.shieldtv.app.core.model.source.SourceSearchRequest,
        enabledProviderIds: Set<String>,
        onProgress: ((SourceFetchProgress) -> Unit)?
    ): ai.shieldtv.app.feature.sources.ui.SourcesUiState {
        progressEvents.forEach { onProgress?.invoke(it) }
        return ai.shieldtv.app.feature.sources.ui.SourcesUiState(sources = providedSources)
    }
}

private class FakeSourceRepository : ai.shieldtv.app.domain.repository.SourceRepository {
    override suspend fun findSources(
        request: ai.shieldtv.app.core.model.source.SourceSearchRequest,
        enabledProviderIds: Set<String>,
        onProgress: ((SourceFetchProgress) -> Unit)?
    ): List<SourceResult> = emptyList()
}

private class TestLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = registry

    init {
        registry.currentState = Lifecycle.State.STARTED
    }
}
