package ai.shieldtv.app.integration.metadata.tmdb

import ai.shieldtv.app.core.network.http.HttpClientFactory
import ai.shieldtv.app.integration.metadata.tmdb.api.RealTmdbApi
import ai.shieldtv.app.integration.metadata.tmdb.api.TmdbApi
import ai.shieldtv.app.integration.metadata.tmdb.config.TmdbConfig

object TmdbFactory {
    fun createRealApi(apiKeyProvider: () -> String?): TmdbApi {
        return RealTmdbApi(
            apiKeyProvider = apiKeyProvider,
            httpClient = HttpClientFactory.createDefault()
        )
    }

    fun createEnvBackedApi(): TmdbApi {
        return createRealApi(apiKeyProvider = { TmdbConfig.apiKeyFromEnv() })
    }
}
