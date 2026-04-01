package ai.shieldtv.app.integration.scrapers.provider

object SourcesConfig {
    private const val ENV_KEY = "ASAHI_SOURCE_FEED_URL"

    fun remoteFeedUrl(): String? {
        return System.getenv(ENV_KEY)?.takeIf { it.isNotBlank() }
    }
}
