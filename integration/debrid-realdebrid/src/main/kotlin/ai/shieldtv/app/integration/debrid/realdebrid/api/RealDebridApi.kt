package ai.shieldtv.app.integration.debrid.realdebrid.api

import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridCredentialResponse
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenResponse

interface RealDebridApi {
    suspend fun startDeviceFlow(): DeviceFlowResponse
    suspend fun getDeviceCredentials(deviceCode: String): RealDebridCredentialResponse?
    suspend fun exchangeDeviceCredentialsForToken(
        deviceCode: String,
        clientId: String,
        clientSecret: String
    ): RealDebridTokenResponse?
    suspend fun instantAvailability(infoHashes: List<String>): String
}

data class DeviceFlowResponse(
    val deviceCode: String,
    val verificationUrl: String,
    val userCode: String,
    val expiresInSeconds: Int,
    val pollIntervalSeconds: Int
)
