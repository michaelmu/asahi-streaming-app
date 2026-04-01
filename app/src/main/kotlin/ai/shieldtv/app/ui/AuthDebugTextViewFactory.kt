package ai.shieldtv.app.ui

import android.content.Context
import android.widget.TextView

class AuthDebugTextViewFactory {
    fun create(context: Context): TextView {
        val text = buildString {
            appendLine("Asahi Real-Debrid Auth Test")
            appendLine()
            appendLine("This build is opening successfully on phones now.")
            appendLine()
            appendLine("The previous version tried to start live RD auth directly during app startup,")
            appendLine("which is likely why it closed immediately on your phone.")
            appendLine()
            appendLine("Next safer step:")
            appendLine("- move RD auth start/poll off the activity startup path")
            appendLine("- trigger auth from a button or delayed background action")
            appendLine("- keep the app open while showing live state updates")
        }

        return TextView(context).apply {
            this.text = text
            textSize = 18f
            setPadding(48, 48, 48, 48)
        }
    }
}
