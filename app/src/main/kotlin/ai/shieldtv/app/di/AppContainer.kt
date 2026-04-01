package ai.shieldtv.app.di

import ai.shieldtv.app.domain.usecase.auth.StartRealDebridDeviceFlowUseCase
import ai.shieldtv.app.domain.usecase.details.GetTitleDetailsUseCase
import ai.shieldtv.app.domain.usecase.playback.BuildPlaybackItemUseCase
import ai.shieldtv.app.domain.usecase.search.SearchTitlesUseCase
import ai.shieldtv.app.domain.usecase.sources.FindSourcesUseCase
import ai.shieldtv.app.domain.usecase.sources.ResolveSourceUseCase
import ai.shieldtv.app.integration.debrid.realdebrid.api.FakeRealDebridApi
import ai.shieldtv.app.integration.debrid.realdebrid.mapper.RealDebridAuthMapper
import ai.shieldtv.app.integration.debrid.realdebrid.repository.RealDebridRepositoryImpl
import ai.shieldtv.app.integration.metadata.tmdb.TmdbFactory
import ai.shieldtv.app.integration.metadata.tmdb.api.FakeTmdbApi
import ai.shieldtv.app.integration.metadata.tmdb.mapper.TmdbDetailsMapper
import ai.shieldtv.app.integration.metadata.tmdb.mapper.TmdbSearchMapper
import ai.shieldtv.app.integration.metadata.tmdb.repository.TmdbMetadataRepository
import ai.shieldtv.app.integration.playback.media3.engine.Media3PlaybackEngine
import ai.shieldtv.app.integration.scrapers.normalize.DefaultSourceNormalizer
import ai.shieldtv.app.integration.scrapers.provider.FakeSourceProvider
import ai.shieldtv.app.integration.scrapers.provider.HttpSourceProviderAdapter
import ai.shieldtv.app.integration.scrapers.provider.JsonSourceProviderAdapter
import ai.shieldtv.app.integration.scrapers.provider.ProviderRegistry
import ai.shieldtv.app.integration.scrapers.provider.SourcesFeedFactory
import ai.shieldtv.app.integration.scrapers.provider.sample.SampleTemplateSourceProvider
import ai.shieldtv.app.integration.scrapers.provider.torrentio.TorrentioSourceProvider
import ai.shieldtv.app.integration.scrapers.ranking.DefaultSourceRanker
import ai.shieldtv.app.integration.scrapers.repository.SourceRepositoryImpl

object AppContainer {
    private val tmdbApi by lazy { TmdbFactory.createRealApi() }
    private val fallbackTmdbApi by lazy { FakeTmdbApi() }
    private val tmdbSearchMapper by lazy { TmdbSearchMapper() }
    private val tmdbDetailsMapper by lazy { TmdbDetailsMapper() }
    private val metadataRepository by lazy {
        TmdbMetadataRepository(
            tmdbApi = tmdbApi,
            tmdbSearchMapper = tmdbSearchMapper,
            tmdbDetailsMapper = tmdbDetailsMapper,
            fallbackTmdbApi = fallbackTmdbApi
        )
    }

    private val realDebridApi by lazy { FakeRealDebridApi() }
    private val realDebridAuthMapper by lazy { RealDebridAuthMapper() }
    private val debridRepository by lazy {
        RealDebridRepositoryImpl(
            realDebridApi = realDebridApi,
            realDebridAuthMapper = realDebridAuthMapper
        )
    }

    private val remoteJsonSourceFeed by lazy {
        SourcesFeedFactory.createRemoteJsonSourceFeed()
    }

    private val providerRegistry by lazy {
        ProviderRegistry(
            providers = listOf(
                FakeSourceProvider(),
                FakeSourceProvider(adapter = HttpSourceProviderAdapter()),
                FakeSourceProvider(adapter = JsonSourceProviderAdapter(remoteJsonSourceFeed::load)),
                SampleTemplateSourceProvider(),
                TorrentioSourceProvider()
            )
        )
    }
    private val sourceNormalizer by lazy { DefaultSourceNormalizer() }
    private val sourceRanker by lazy { DefaultSourceRanker() }
    private val sourceRepository by lazy {
        SourceRepositoryImpl(
            providerRegistry = providerRegistry,
            sourceNormalizer = sourceNormalizer,
            sourceRanker = sourceRanker
        )
    }

    val playbackEngine by lazy { Media3PlaybackEngine() }

    val searchTitlesUseCase by lazy {
        SearchTitlesUseCase(metadataRepository)
    }

    val getTitleDetailsUseCase by lazy {
        GetTitleDetailsUseCase(metadataRepository)
    }

    val startRealDebridDeviceFlowUseCase by lazy {
        StartRealDebridDeviceFlowUseCase(debridRepository)
    }

    val findSourcesUseCase by lazy {
        FindSourcesUseCase(sourceRepository)
    }

    val resolveSourceUseCase by lazy {
        ResolveSourceUseCase(debridRepository)
    }

    val buildPlaybackItemUseCase by lazy {
        BuildPlaybackItemUseCase()
    }
}
