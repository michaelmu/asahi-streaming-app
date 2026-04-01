package ai.shieldtv.app.integration.debrid.realdebrid.auth

data class RealDebridDeviceCodeResponse(
    val deviceCode: String,
    val userCode: String,
    val verificationUrl: String,
    val expiresInSeconds: Int,
    val intervalSeconds: Int
)

data class RealDebridCredentialResponse(
    val clientId: String,
    val clientSecret: String
)

data class RealDebridTokenResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresInSeconds: Int? = null
)
