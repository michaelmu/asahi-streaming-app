package ai.shieldtv.app.integration.scrapers.provider

import ai.shieldtv.app.core.network.http.HttpClientFactory

object SourcesFeedFactory {
    fun createRemoteJsonSourceFeed(): RemoteJsonSourceFeed {
        return RemoteJsonSourceFeed(
            baseUrlProvider = { SourcesConfig.remoteFeedUrl() },
            httpClient = HttpClientFactory.createDefault()
        )
    }
}
