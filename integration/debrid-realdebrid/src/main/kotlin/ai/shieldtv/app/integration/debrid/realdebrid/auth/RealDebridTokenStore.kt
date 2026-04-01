package ai.shieldtv.app.integration.debrid.realdebrid.auth

data class RealDebridTokens(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresInSeconds: Int? = null
)

class RealDebridTokenStore {
    private var tokens: RealDebridTokens? = null

    fun save(tokens: RealDebridTokens) {
        this.tokens = tokens
    }

    fun get(): RealDebridTokens? = tokens

    fun clear() {
        tokens = null
    }
}
