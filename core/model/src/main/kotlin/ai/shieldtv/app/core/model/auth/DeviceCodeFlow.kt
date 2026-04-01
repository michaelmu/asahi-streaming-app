package ai.shieldtv.app.core.model.auth

data class DeviceCodeFlow(
    val verificationUrl: String,
    val userCode: String,
    val qrCodeUrl: String? = null,
    val expiresInSeconds: Int,
    val pollIntervalSeconds: Int
)
