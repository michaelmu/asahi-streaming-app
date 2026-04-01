package ai.shieldtv.app.di

import ai.shieldtv.app.domain.usecase.auth.StartRealDebridDeviceFlowUseCase
import ai.shieldtv.app.domain.usecase.details.GetTitleDetailsUseCase
import ai.shieldtv.app.domain.usecase.search.SearchTitlesUseCase
import ai.shieldtv.app.domain.usecase.sources.FindSourcesUseCase
import ai.shieldtv.app.integration.debrid.realdebrid.api.FakeRealDebridApi
import ai.shieldtv.app.integration.debrid.realdebrid.mapper.RealDebridAuthMapper
import ai.shieldtv.app.integration.debrid.realdebrid.repository.RealDebridRepositoryImpl
import ai.shieldtv.app.integration.metadata.tmdb.api.FakeTmdbApi
import ai.shieldtv.app.integration.metadata.tmdb.mapper.TmdbDetailsMapper
import ai.shieldtv.app.integration.metadata.tmdb.mapper.TmdbSearchMapper
import ai.shieldtv.app.integration.metadata.tmdb.repository.TmdbMetadataRepository
import ai.shieldtv.app.integration.scrapers.normalize.DefaultSourceNormalizer
import ai.shieldtv.app.integration.scrapers.provider.FakeSourceProvider
import ai.shieldtv.app.integration.scrapers.provider.ProviderRegistry
import ai.shieldtv.app.integration.scrapers.ranking.DefaultSourceRanker
import ai.shieldtv.app.integration.scrapers.repository.SourceRepositoryImpl

object AppContainer {
    private val tmdbApi by lazy { FakeTmdbApi() }
    private val tmdbSearchMapper by lazy { TmdbSearchMapper() }
    private val tmdbDetailsMapper by lazy { TmdbDetailsMapper() }
    private val metadataRepository by lazy {
        TmdbMetadataRepository(
            tmdbApi = tmdbApi,
            tmdbSearchMapper = tmdbSearchMapper,
            tmdbDetailsMapper = tmdbDetailsMapper
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

    private val providerRegistry by lazy {
        ProviderRegistry(providers = listOf(FakeSourceProvider()))
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
}
