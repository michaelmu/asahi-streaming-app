package ai.shieldtv.app.integration.debrid.realdebrid.config

object RealDebridConfig {
    private const val CLIENT_ID_ENV = "ASAHI_RD_CLIENT_ID"
    private const val CLIENT_SECRET_ENV = "ASAHI_RD_CLIENT_SECRET"
    private const val ACCESS_TOKEN_ENV = "ASAHI_RD_ACCESS_TOKEN"
    private const val DEFAULT_BOOTSTRAP_CLIENT_ID = "X245A4XAIBGVM"

    fun clientId(): String = System.getenv(CLIENT_ID_ENV)?.takeIf { it.isNotBlank() } ?: DEFAULT_BOOTSTRAP_CLIENT_ID
    fun clientSecret(): String? = System.getenv(CLIENT_SECRET_ENV)?.takeIf { it.isNotBlank() }
    fun accessToken(): String? = System.getenv(ACCESS_TOKEN_ENV)?.takeIf { it.isNotBlank() }
}
