package ai.shieldtv.app.integration.metadata.tmdb.config

object TmdbConfig {
    private const val ENV_KEY = "TMDB_API_KEY"

    fun apiKeyFromEnv(): String? {
        return System.getenv(ENV_KEY)?.takeIf { it.isNotBlank() }
    }
}
