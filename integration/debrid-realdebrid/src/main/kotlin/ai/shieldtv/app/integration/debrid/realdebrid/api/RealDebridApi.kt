package ai.shieldtv.app.integration.debrid.realdebrid.api

interface RealDebridApi {
    suspend fun startDeviceFlow(): DeviceFlowResponse
}

data class DeviceFlowResponse(
    val verificationUrl: String,
    val userCode: String,
    val expiresInSeconds: Int,
    val pollIntervalSeconds: Int
)
