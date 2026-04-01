package ai.shieldtv.app.integration.debrid.realdebrid.auth

import ai.shieldtv.app.integration.debrid.realdebrid.debug.RealDebridDebugState
import java.io.File

data class RealDebridTokens(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresInSeconds: Int? = null
)

class RealDebridTokenStore(
    private val fileProvider: () -> File = { defaultTokenFile() }
) {
    private var tokens: RealDebridTokens? = null

    fun save(tokens: RealDebridTokens) {
        this.tokens = tokens
        val file = fileProvider()
        RealDebridDebugState.lastTokenStoreSaveCalled = "yes"
        RealDebridDebugState.lastTokenStoreWritePath = file.absolutePath
        file.parentFile?.mkdirs()
        file.writeText(
            listOf(
                tokens.accessToken,
                tokens.refreshToken.orEmpty(),
                tokens.expiresInSeconds?.toString().orEmpty()
            ).joinToString("\n")
        )
        RealDebridDebugState.lastTokenStoreWriteExists = if (file.exists()) "yes" else "no"
    }

    fun get(): RealDebridTokens? = tokens ?: loadFromDisk()?.also { tokens = it }

    fun clear() {
        tokens = null
        val file = fileProvider()
        if (file.exists()) file.delete()
    }

    private fun loadFromDisk(): RealDebridTokens? {
        val file = fileProvider()
        if (!file.exists()) return null
        val lines = file.readLines()
        if (lines.isEmpty() || lines[0].isBlank()) return null
        return RealDebridTokens(
            accessToken = lines[0],
            refreshToken = lines.getOrNull(1)?.ifBlank { null },
            expiresInSeconds = lines.getOrNull(2)?.toIntOrNull()
        )
    }

    fun debugFilePath(): String = fileProvider().absolutePath

    companion object {
        private fun defaultTokenFile(): File {
            val cwd = File("").absoluteFile
            return when {
                cwd.name == "app" -> File(cwd, "debug/rd-tokens.txt")
                File(cwd, "app").isDirectory -> File(cwd, "app/debug/rd-tokens.txt")
                else -> File(cwd, "debug/rd-tokens.txt")
            }
        }
    }
}
