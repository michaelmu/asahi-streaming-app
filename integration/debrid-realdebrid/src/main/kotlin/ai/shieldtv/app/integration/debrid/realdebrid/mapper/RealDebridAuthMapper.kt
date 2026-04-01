package ai.shieldtv.app.integration.debrid.realdebrid.mapper

import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.integration.debrid.realdebrid.api.DeviceFlowResponse

class RealDebridAuthMapper {
    fun toDeviceCodeFlow(response: DeviceFlowResponse): DeviceCodeFlow {
        return DeviceCodeFlow(
            deviceCode = response.deviceCode,
            verificationUrl = response.verificationUrl,
            userCode = response.userCode,
            qrCodeUrl = response.verificationUrl,
            expiresInSeconds = response.expiresInSeconds,
            pollIntervalSeconds = response.pollIntervalSeconds
        )
    }
}
