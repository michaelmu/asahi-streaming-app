package ai.shieldtv.app.debug

import ai.shieldtv.app.feature.SettingsFeatureFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

object PollAuthRunner {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val flow = AuthDebugStore.load()
        if (flow == null) {
            println("No stored device flow found")
            return@runBlocking
        }

        val maxAttempts = args.firstOrNull()?.toIntOrNull() ?: 12
        val settingsViewModel = SettingsFeatureFactory.createViewModel()

        repeat(maxAttempts) { attempt ->
            val state = settingsViewModel.pollLinking(flow)
            println("Attempt ${attempt + 1}/$maxAttempts")
            println("Linked: ${state.authState.isLinked}")
            println("Auth in progress: ${state.authState.authInProgress}")
            println("Error: ${state.error ?: "none"}")
            println()

            if (state.authState.isLinked) {
                println("Auth completed successfully.")
                AuthDebugStore.clear()
                return@runBlocking
            }

            delay(flow.pollIntervalSeconds.coerceAtLeast(1).toLong() * 1000L)
        }

        println("Auth polling finished without linking.")
    }
}
