package ai.shieldtv.app.ui

import android.content.Context
import android.widget.TextView
import ai.shieldtv.app.feature.SettingsFeatureFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class AuthDebugTextViewFactory {
    fun create(context: Context): TextView {
        val text = runBlocking {
            val settingsViewModel = SettingsFeatureFactory.createViewModel()
            val startedState = settingsViewModel.startLinking()
            val flow = startedState.deviceCodeFlow

            val pollAttempts = 24
            val polledState = if (flow != null) {
                var latest = startedState
                repeat(pollAttempts) {
                    delay(flow.pollIntervalSeconds.coerceAtLeast(1).toLong() * 1000L)
                    latest = settingsViewModel.pollLinking(flow)
                    if (latest.authState.isLinked) return@repeat
                }
                latest
            } else {
                startedState
            }

            buildString {
                appendLine("Asahi Real-Debrid Auth Test")
                appendLine()
                appendLine("FINAL STATE")
                appendLine("Linked: ${polledState.authState.isLinked}")
                appendLine("Auth in progress: ${polledState.authState.authInProgress}")
                appendLine("Error: ${polledState.error ?: startedState.error ?: "none"}")
                appendLine()
                appendLine("AUTH CODE")
                appendLine("Verification URL: ${flow?.verificationUrl ?: "none"}")
                appendLine("User Code: ${flow?.userCode ?: "none"}")
                appendLine()
                appendLine("TEST NOTES")
                appendLine("- This build starts RD auth immediately.")
                appendLine("- It then polls for up to $pollAttempts attempts in the same app session.")
                appendLine("- Complete the auth in your browser as fast as possible after launch.")
                appendLine("- Relaunch if the code expires before authorization completes.")
            }
        }

        return TextView(context).apply {
            this.text = text
            textSize = 18f
            setPadding(48, 48, 48, 48)
        }
    }
}
