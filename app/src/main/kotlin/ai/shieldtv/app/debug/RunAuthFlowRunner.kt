package ai.shieldtv.app.debug

import ai.shieldtv.app.feature.SettingsFeatureFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

object RunAuthFlowRunner {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val settingsViewModel = SettingsFeatureFactory.createViewModel()
        val startedState = settingsViewModel.startLinking()
        val flow = startedState.deviceCodeFlow

        if (flow == null) {
            println("No device flow returned")
            return@runBlocking
        }

        println("Verification URL: ${flow.verificationUrl}")
        println("User Code: ${flow.userCode}")
        println("Waiting for authorization...")
        println()

        val maxAttempts = 24
        repeat(maxAttempts) { attempt ->
            val state = settingsViewModel.pollLinking(flow)
            println("Attempt ${attempt + 1}/$maxAttempts")
            println("Linked: ${state.authState.isLinked}")
            println("Auth in progress: ${state.authState.authInProgress}")
            println("Error: ${state.error ?: "none"}")
            println()

            if (state.authState.isLinked) {
                println("Auth completed successfully.")
                return@runBlocking
            }

            delay(flow.pollIntervalSeconds.coerceAtLeast(1).toLong() * 1000L)
        }

        println("Auth flow timed out without linking.")
    }
}
