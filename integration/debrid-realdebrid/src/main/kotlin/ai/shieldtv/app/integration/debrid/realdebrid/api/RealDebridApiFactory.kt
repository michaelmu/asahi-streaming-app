package ai.shieldtv.app.integration.debrid.realdebrid.api

import ai.shieldtv.app.core.network.http.HttpClientFactory
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenStore
import ai.shieldtv.app.integration.debrid.realdebrid.debug.RealDebridDebugState

object RealDebridApiFactory {
    fun create(tokenStore: RealDebridTokenStore): RealDebridApi {
        RealDebridDebugState.lastApiMode = "http"
        return RealDebridHttpApi(
            httpClient = HttpClientFactory.createDefault(),
            tokenStore = tokenStore
        )
    }
}
