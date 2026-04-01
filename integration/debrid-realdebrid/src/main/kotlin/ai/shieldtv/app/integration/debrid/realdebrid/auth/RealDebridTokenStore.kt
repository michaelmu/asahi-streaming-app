package ai.shieldtv.app.integration.debrid.realdebrid.auth

import java.io.File

data class RealDebridTokens(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresInSeconds: Int? = null
)

class RealDebridTokenStore(
    private val file: File = File("debug/rd-tokens.txt")
) {
    private var tokens: RealDebridTokens? = loadFromDisk()

    fun save(tokens: RealDebridTokens) {
        this.tokens = tokens
        file.parentFile?.mkdirs()
        file.writeText(
            listOf(
                tokens.accessToken,
                tokens.refreshToken.orEmpty(),
                tokens.expiresInSeconds?.toString().orEmpty()
            ).joinToString("\n")
        )
    }

    fun get(): RealDebridTokens? = tokens ?: loadFromDisk()?.also { tokens = it }

    fun clear() {
        tokens = null
        if (file.exists()) file.delete()
    }

    private fun loadFromDisk(): RealDebridTokens? {
        if (!file.exists()) return null
        val lines = file.readLines()
        if (lines.isEmpty() || lines[0].isBlank()) return null
        return RealDebridTokens(
            accessToken = lines[0],
            refreshToken = lines.getOrNull(1)?.ifBlank { null },
            expiresInSeconds = lines.getOrNull(2)?.toIntOrNull()
        )
    }
}
