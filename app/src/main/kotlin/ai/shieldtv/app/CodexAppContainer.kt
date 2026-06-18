package ai.codexa.app

import ai.codexa.app.core.data.codex.CodexConversationStore
import ai.codexa.app.core.network.codex.CodexRealtimeRpcClient
import ai.codexa.app.domain.codex.CodexConversationViewModel
import android.content.Context

class CodexAppContainer(context: Context) {
    private val preferences = CodexPreferences(context)
    private val repository = CodexConversationStore(
        rpcClient = CodexRealtimeRpcClient(),
    )

    val viewModel = CodexConversationViewModel(repository)

    fun loadSavedConnection(): CodexSavedConnection = preferences.loadConnection()

    fun saveConnection(connection: CodexSavedConnection) {
        preferences.saveConnection(connection)
    }
}
