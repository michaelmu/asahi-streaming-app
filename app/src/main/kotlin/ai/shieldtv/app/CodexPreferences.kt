package ai.codexa.app

import android.content.Context

private const val PREFS_NAME = "codex_client"
private const val PREF_WS_URL = "ws_url"
private const val PREF_BEARER_TOKEN = "***"
private const val DEFAULT_WS_URL = "ws://10.0.2.2:8765"

data class CodexSavedConnection(
    val websocketUrl: String,
    val bearerToken: String,
)

class CodexPreferences(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadConnection(): CodexSavedConnection = CodexSavedConnection(
        websocketUrl = prefs.getString(PREF_WS_URL, DEFAULT_WS_URL) ?: DEFAULT_WS_URL,
        bearerToken = prefs.getString(PREF_BEARER_TOKEN, "") ?: "",
    )

    fun saveConnection(connection: CodexSavedConnection) {
        prefs.edit()
            .putString(PREF_WS_URL, connection.websocketUrl)
            .putString(PREF_BEARER_TOKEN, connection.bearerToken)
            .apply()
    }
}
