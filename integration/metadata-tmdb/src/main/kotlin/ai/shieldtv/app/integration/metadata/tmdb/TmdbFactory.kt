package ai.shieldtv.app.integration.metadata.tmdb

import ai.shieldtv.app.core.network.http.HttpClientFactory
import ai.shieldtv.app.integration.metadata.tmdb.api.RealTmdbApi
import ai.shieldtv.app.integration.metadata.tmdb.api.TmdbApi
import ai.shieldtv.app.integration.metadata.tmdb.config.TmdbConfig

object TmdbFactory {
    fun createRealApi(): TmdbApi {
        return RealTmdbApi(
            apiKeyProvider = { TmdbConfig.apiKeyFromEnv() },
            httpClient = HttpClientFactory.createDefault()
        )
    }
}
