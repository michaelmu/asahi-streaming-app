package ai.shieldtv.app.integration.debrid.realdebrid.config

object RealDebridConfig {
    private const val CLIENT_ID_ENV = "ASAHI_RD_CLIENT_ID"
    private const val CLIENT_SECRET_ENV = "ASAHI_RD_CLIENT_SECRET"

    fun clientId(): String? = System.getenv(CLIENT_ID_ENV)?.takeIf { it.isNotBlank() }
    fun clientSecret(): String? = System.getenv(CLIENT_SECRET_ENV)?.takeIf { it.isNotBlank() }
}
