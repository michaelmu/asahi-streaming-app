package ai.shieldtv.app.integration.debrid.realdebrid.api

class FakeRealDebridApi : RealDebridApi {
    override suspend fun startDeviceFlow(): DeviceFlowResponse {
        return DeviceFlowResponse(
            verificationUrl = "https://real-debrid.example/device",
            userCode = "ASAHI1",
            expiresInSeconds = 900,
            pollIntervalSeconds = 5
        )
    }
}
