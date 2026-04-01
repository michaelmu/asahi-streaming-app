package ai.shieldtv.app.ui

import android.content.Context
import android.widget.TextView
import ai.shieldtv.app.AppState

class DebugTextViewFactory {
    private val debugAppPreviewBuilder = DebugAppPreviewBuilder()

    fun create(context: Context, state: AppState, renderedScreen: String): TextView {
        return TextView(context).apply {
            text = buildString {
                appendLine("Asahi")
                appendLine()
                appendLine("Current screen: $renderedScreen")
                appendLine("Selected media: ${state.selectedMedia?.title ?: "none"}")
                appendLine("Selected source: ${state.selectedSource?.displayName ?: "none"}")
                appendLine()
                append(debugAppPreviewBuilder.build())
            }
            textSize = 18f
            setPadding(48, 48, 48, 48)
        }
    }
}
