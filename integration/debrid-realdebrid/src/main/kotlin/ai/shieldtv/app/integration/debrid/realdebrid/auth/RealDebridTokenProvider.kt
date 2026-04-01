package ai.shieldtv.app.integration.debrid.realdebrid.auth

fun interface RealDebridTokenProvider {
    fun getAccessToken(): String?
}
