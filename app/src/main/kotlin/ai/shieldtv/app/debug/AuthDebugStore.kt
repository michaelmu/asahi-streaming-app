package ai.shieldtv.app.debug

import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import java.io.File

object AuthDebugStore {
    private val file = File("debug/rd-device-flow.txt")

    fun save(flow: DeviceCodeFlow) {
        file.parentFile?.mkdirs()
        file.writeText(
            listOf(
                flow.deviceCode,
                flow.verificationUrl,
                flow.userCode,
                flow.qrCodeUrl.orEmpty(),
                flow.expiresInSeconds.toString(),
                flow.pollIntervalSeconds.toString()
            ).joinToString("\n")
        )
    }

    fun load(): DeviceCodeFlow? {
        if (!file.exists()) return null
        val lines = file.readLines()
        return when (lines.size) {
            6 -> DeviceCodeFlow(
                deviceCode = lines[0],
                verificationUrl = lines[1],
                userCode = lines[2],
                qrCodeUrl = lines[3].ifBlank { null },
                expiresInSeconds = lines[4].toIntOrNull() ?: return null,
                pollIntervalSeconds = lines[5].toIntOrNull() ?: return null
            )
            5 -> DeviceCodeFlow(
                deviceCode = "",
                verificationUrl = lines[0],
                userCode = lines[1],
                qrCodeUrl = lines[2].ifBlank { null },
                expiresInSeconds = lines[3].toIntOrNull() ?: return null,
                pollIntervalSeconds = lines[4].toIntOrNull() ?: return null
            )
            else -> null
        }
    }

    fun clear() {
        if (file.exists()) file.delete()
    }
}
