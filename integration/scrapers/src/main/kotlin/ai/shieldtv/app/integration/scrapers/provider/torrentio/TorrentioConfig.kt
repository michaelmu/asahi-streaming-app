package ai.shieldtv.app.integration.scrapers.provider.torrentio

object TorrentioConfig {
    private const val ENABLED_ENV = "ASAHI_TORRENTIO_ENABLED"
    private const val BASE_URL_ENV = "ASAHI_TORRENTIO_BASE_URL"

    fun isEnabled(): Boolean {
        return System.getenv(ENABLED_ENV)?.equals("true", ignoreCase = true) == true
    }

    fun baseUrl(): String {
        return System.getenv(BASE_URL_ENV)?.takeIf { it.isNotBlank() }
            ?: "https://torrentio.strem.fun"
    }
}
