package ai.shieldtv.app.integration.debrid.realdebrid.api

import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridCredentialResponse
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenResponse

class FakeRealDebridApi : RealDebridApi {
    override suspend fun startDeviceFlow(): DeviceFlowResponse {
        return DeviceFlowResponse(
            deviceCode = "device-code-asahi",
            verificationUrl = "https://real-debrid.example/device",
            userCode = "ASAHI1",
            expiresInSeconds = 900,
            pollIntervalSeconds = 5
        )
    }

    override suspend fun getDeviceCredentials(deviceCode: String): RealDebridCredentialResponse? {
        return RealDebridCredentialResponse(
            clientId = "fake-client-id",
            clientSecret = "fake-client-secret"
        )
    }

    override suspend fun exchangeDeviceCredentialsForToken(
        deviceCode: String,
        clientId: String,
        clientSecret: String
    ): RealDebridTokenResponse? {
        return RealDebridTokenResponse(
            accessToken = "fake-access-token",
            refreshToken = "fake-refresh-token",
            expiresInSeconds = 3600
        )
    }

    override suspend fun instantAvailability(infoHashes: List<String>): String {
        val body = infoHashes.joinToString(prefix = "{", postfix = "}") { hash ->
            val available = hash.lowercase().takeLast(1).toIntOrNull(16)?.rem(2) == 0
            if (available) "\"$hash\": { \"rd\": [\"cached\"] }" else "\"$hash\": {}"
        }
        return body
    }
}
