package ai.shieldtv.app.debug

import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import java.io.File

object AuthDebugStore {
    private val file = File("debug/rd-device-flow.txt")

    fun save(flow: DeviceCodeFlow) {
        file.parentFile?.mkdirs()
        file.writeText(
            listOf(
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
        if (lines.size < 5) return null
        return DeviceCodeFlow(
            verificationUrl = lines[0],
            userCode = lines[1],
            qrCodeUrl = lines[2].ifBlank { null },
            expiresInSeconds = lines[3].toIntOrNull() ?: return null,
            pollIntervalSeconds = lines[4].toIntOrNull() ?: return null
        )
    }

    fun clear() {
        if (file.exists()) file.delete()
    }
}
