package ai.shieldtv.app.di

import android.content.Context
import ai.shieldtv.app.BuildConfig
import ai.shieldtv.app.domain.usecase.auth.GetRealDebridAuthStateUseCase
import ai.shieldtv.app.domain.usecase.auth.PollRealDebridDeviceFlowUseCase
import ai.shieldtv.app.domain.usecase.auth.StartRealDebridDeviceFlowUseCase
import ai.shieldtv.app.domain.usecase.details.GetTitleDetailsUseCase
import ai.shieldtv.app.domain.usecase.playback.BuildPlaybackItemUseCase
import ai.shieldtv.app.domain.usecase.search.SearchTitlesUseCase
import ai.shieldtv.app.domain.usecase.sources.FindSourcesUseCase
import ai.shieldtv.app.domain.usecase.sources.ResolveSourceUseCase
import ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridApiFactory
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenProvider
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenStore
import ai.shieldtv.app.integration.debrid.realdebrid.mapper.RealDebridAuthMapper
import ai.shieldtv.app.integration.debrid.realdebrid.repository.RealDebridCacheRepository
import ai.shieldtv.app.integration.debrid.realdebrid.repository.RealDebridRepositoryImpl
import ai.shieldtv.app.integration.metadata.tmdb.TmdbFactory
import ai.shieldtv.app.integration.metadata.tmdb.api.FakeTmdbApi
import ai.shieldtv.app.integration.metadata.tmdb.mapper.TmdbDetailsMapper
import ai.shieldtv.app.integration.metadata.tmdb.mapper.TmdbSearchMapper
import ai.shieldtv.app.integration.metadata.tmdb.mapper.TmdbSeasonMapper
import ai.shieldtv.app.integration.metadata.tmdb.repository.TmdbMetadataRepository
import ai.shieldtv.app.integration.playback.media3.engine.Media3PlaybackEngine
import ai.shieldtv.app.integration.scrapers.normalize.DefaultSourceNormalizer
import ai.shieldtv.app.integration.scrapers.provider.ProviderRegistry
import ai.shieldtv.app.integration.scrapers.provider.bitmagnet.BitmagnetConfig
import ai.shieldtv.app.integration.scrapers.provider.bitmagnet.BitmagnetSourceProvider
import ai.shieldtv.app.integration.scrapers.provider.bitsearch.BitSearchConfig
import ai.shieldtv.app.integration.scrapers.provider.bitsearch.BitSearchSourceProvider
import ai.shieldtv.app.integration.scrapers.provider.comet.CometConfig
import ai.shieldtv.app.integration.scrapers.provider.knaben.KnabenConfig
import ai.shieldtv.app.integration.scrapers.provider.knaben.KnabenSourceProvider
import ai.shieldtv.app.integration.scrapers.provider.comet.CometSourceProvider
import ai.shieldtv.app.integration.scrapers.provider.torrentio.TorrentioConfig
import ai.shieldtv.app.integration.scrapers.provider.torrentio.TorrentioSourceProvider
import ai.shieldtv.app.integration.scrapers.provider.torz.TorzConfig
import ai.shieldtv.app.integration.scrapers.provider.torz.TorzSourceProvider
import ai.shieldtv.app.integration.scrapers.provider.zilean.ZileanConfig
import ai.shieldtv.app.integration.scrapers.provider.zilean.ZileanSourceProvider
import ai.shieldtv.app.integration.scrapers.ranking.DefaultSourceRanker
import ai.shieldtv.app.integration.scrapers.ranking.RealDebridSourceCacheMarker
import ai.shieldtv.app.settings.SourcePreferencesStore
import ai.shieldtv.app.integration.scrapers.repository.SourceRepositoryImpl
import ai.shieldtv.app.playback.PlaybackSessionStore
import java.io.File

object AppContainer {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun requireContext(): Context {
        return checkNotNull(appContext) { "AppContainer not initialized" }
    }

    private val tmdbApi by lazy {
        TmdbFactory.createRealApi {
            BuildConfig.TMDB_API_KEY.takeIf { it.isNotBlank() }
        }
    }
    private val fallbackTmdbApi by lazy { FakeTmdbApi() }
    private val tmdbSearchMapper by lazy { TmdbSearchMapper() }
    private val tmdbDetailsMapper by lazy { TmdbDetailsMapper() }
    private val tmdbSeasonMapper by lazy { TmdbSeasonMapper() }
    private val metadataRepository by lazy {
        TmdbMetadataRepository(
            tmdbApi = tmdbApi,
            tmdbSearchMapper = tmdbSearchMapper,
            tmdbDetailsMapper = tmdbDetailsMapper,
            tmdbSeasonMapper = tmdbSeasonMapper,
            fallbackTmdbApi = fallbackTmdbApi
        )
    }

    private val realDebridAuthMapper by lazy { RealDebridAuthMapper() }
    private val realDebridTokenStore by lazy {
        RealDebridTokenStore {
            File(requireContext().filesDir, "realdebrid/rd-tokens.txt")
        }
    }
    private val realDebridTokenProvider by lazy {
        RealDebridTokenProvider { realDebridTokenStore.get()?.accessToken }
    }
    private val realDebridApi by lazy { RealDebridApiFactory.create(realDebridTokenStore) }
    private val debridRepository by lazy {
        RealDebridRepositoryImpl(
            realDebridApi = realDebridApi,
            realDebridAuthMapper = realDebridAuthMapper,
            tokenStore = realDebridTokenStore
        )
    }
    private val debridCacheRepository by lazy { RealDebridCacheRepository(realDebridApi) }

    private val providerRegistry by lazy {
        val providers = buildList {
            if (TorrentioConfig.isEnabled()) {
                add(TorrentioSourceProvider(realDebridTokenProvider))
            }
            if (CometConfig.isEnabled()) {
                add(CometSourceProvider(realDebridTokenProvider))
            }
            if (BitSearchConfig.isEnabled()) {
                add(BitSearchSourceProvider())
            }
            if (BitmagnetConfig.isEnabled()) {
                add(BitmagnetSourceProvider())
            }
            if (KnabenConfig.isEnabled()) {
                add(KnabenSourceProvider())
            }
            if (ZileanConfig.isEnabled()) {
                add(ZileanSourceProvider())
            }
            if (TorzConfig.isEnabled()) {
                add(TorzSourceProvider())
            }
        }
        ProviderRegistry(providers = providers)
    }
    private val sourceNormalizer by lazy { DefaultSourceNormalizer() }
    private val sourceRanker by lazy { DefaultSourceRanker() }
    private val sourceCacheMarker by lazy { RealDebridSourceCacheMarker(debridCacheRepository) }
    val sourcePreferencesStore by lazy {
        SourcePreferencesStore(requireContext())
    }

    fun availableProviderIds(): List<String> = providerRegistry.allProviders().map { it.id }
    fun availableProviderLabels(): Map<String, String> = providerRegistry.allProviders().associate { it.id to it.displayName }

    private val sourceRepository by lazy {
        SourceRepositoryImpl(
            providerRegistry = providerRegistry,
            sourceNormalizer = sourceNormalizer,
            sourceRanker = sourceRanker,
            sourceCacheMarker = sourceCacheMarker
        )
    }

    val playbackEngine by lazy { Media3PlaybackEngine() }

    val playbackSessionStore by lazy {
        PlaybackSessionStore(requireContext())
    }

    val searchTitlesUseCase by lazy {
        SearchTitlesUseCase(metadataRepository)
    }

    val getTitleDetailsUseCase by lazy {
        GetTitleDetailsUseCase(metadataRepository)
    }

    val getRealDebridAuthStateUseCase by lazy {
        GetRealDebridAuthStateUseCase(debridRepository)
    }

    val startRealDebridDeviceFlowUseCase by lazy {
        StartRealDebridDeviceFlowUseCase(debridRepository)
    }

    val pollRealDebridDeviceFlowUseCase by lazy {
        PollRealDebridDeviceFlowUseCase(debridRepository)
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

    fun clearRealDebridAuth() {
        realDebridTokenStore.clear()
    }

    fun realDebridTokenStoreDebugPath(): String = realDebridTokenStore.debugFilePath()
}
