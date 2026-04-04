package ai.shieldtv.app.feature.player.presentation

import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.playback.PlaybackItem
import ai.shieldtv.app.core.model.playback.PlaybackState
import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.ProviderKind
import ai.shieldtv.app.core.model.source.Quality
import ai.shieldtv.app.core.model.source.ResolvedStream
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.playback.PlaybackEngine
import ai.shieldtv.app.domain.repository.DebridRepository
import ai.shieldtv.app.domain.usecase.playback.BuildPlaybackItemUseCase
import ai.shieldtv.app.domain.usecase.sources.ResolveSourceUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPresenterTest {
    @Test
    fun prepare_returns_typed_resolve_failure() = runBlocking {
        val presenter = PlayerPresenter(
            resolveSourceUseCase = ResolveSourceUseCase(FailingDebridRepository()),
            buildPlaybackItemUseCase = BuildPlaybackItemUseCase(),
            playbackEngine = FakePlaybackEngine()
        )

        val state = presenter.prepare(source())

        assertFalse(state.prepared)
        assertEquals("ResolveFailed", state.errorType)
    }

    @Test
    fun prepare_returns_typed_prepare_failure() = runBlocking {
        val resolved = ResolvedStream(url = "https://example.com/video.mkv", source = source())
        val presenter = PlayerPresenter(
            resolveSourceUseCase = ResolveSourceUseCase(SuccessDebridRepository(resolved)),
            buildPlaybackItemUseCase = BuildPlaybackItemUseCase(),
            playbackEngine = object : PlaybackEngine {
                override suspend fun prepare(item: PlaybackItem, startPositionMs: Long) {
                    throw IllegalStateException("prepare failed")
                }
                override fun play() = Unit
                override fun pause() = Unit
                override fun stop() = Unit
                override fun release() = Unit
                override fun getCurrentItem(): PlaybackItem? = null
                override fun getCurrentUrl(): String? = null
                override fun observeState(): Flow<PlaybackState> = emptyFlow()
            }
        )

        val state = presenter.prepare(source())

        assertFalse(state.prepared)
        assertEquals("PrepareFailed", state.errorType)
    }

    @Test
    fun prepare_succeeds_without_error_type() = runBlocking {
        val resolved = ResolvedStream(url = "https://example.com/video.mkv", source = source())
        val presenter = PlayerPresenter(
            resolveSourceUseCase = ResolveSourceUseCase(SuccessDebridRepository(resolved)),
            buildPlaybackItemUseCase = BuildPlaybackItemUseCase(),
            playbackEngine = FakePlaybackEngine()
        )

        val state = presenter.prepare(source())

        assertTrue(state.prepared)
        assertNull(state.errorType)
    }

    private fun source(): SourceResult = SourceResult(
        id = "test",
        mediaRef = MediaRef(
            mediaType = MediaType.MOVIE,
            ids = MediaIds(tmdbId = "1", imdbId = "tt1", tvdbId = null),
            title = "Movie",
            year = 2024
        ),
        providerId = "test",
        providerDisplayName = "Test",
        providerKind = ProviderKind.SCRAPER,
        debridService = DebridService.NONE,
        sourceSite = "Test",
        url = "https://example.com/source",
        displayName = "Movie.2024.1080p",
        quality = Quality.FHD_1080P,
        cacheStatus = CacheStatus.CACHED
    )
}

private class FakePlaybackEngine : PlaybackEngine {
    override suspend fun prepare(item: PlaybackItem, startPositionMs: Long) = Unit
    override fun play() = Unit
    override fun pause() = Unit
    override fun stop() = Unit
    override fun release() = Unit
    override fun getCurrentItem(): PlaybackItem? = null
    override fun getCurrentUrl(): String? = null
    override fun observeState(): Flow<PlaybackState> = emptyFlow()
}

private class FailingDebridRepository : DebridRepository {
    override suspend fun getAuthState(): RealDebridAuthState = RealDebridAuthState(isLinked = false)
    override suspend fun startDeviceFlow(): DeviceCodeFlow = throw UnsupportedOperationException()
    override suspend fun pollDeviceFlow(flow: DeviceCodeFlow): RealDebridAuthState = throw UnsupportedOperationException()
    override suspend fun resolve(source: SourceResult, seasonNumber: Int?, episodeNumber: Int?): ResolvedStream {
        error("resolve failed")
    }
}

private class SuccessDebridRepository(
    private val resolvedStream: ResolvedStream
) : DebridRepository {
    override suspend fun getAuthState(): RealDebridAuthState = RealDebridAuthState(isLinked = false)
    override suspend fun startDeviceFlow(): DeviceCodeFlow = throw UnsupportedOperationException()
    override suspend fun pollDeviceFlow(flow: DeviceCodeFlow): RealDebridAuthState = throw UnsupportedOperationException()
    override suspend fun resolve(source: SourceResult, seasonNumber: Int?, episodeNumber: Int?): ResolvedStream = resolvedStream
}
