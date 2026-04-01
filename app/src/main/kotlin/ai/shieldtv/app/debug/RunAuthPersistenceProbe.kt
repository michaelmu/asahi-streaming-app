package ai.shieldtv.app.debug

import ai.shieldtv.app.feature.SettingsFeatureFactory
import ai.shieldtv.app.integration.debrid.realdebrid.debug.RealDebridDebugState
import java.io.File
import kotlinx.coroutines.runBlocking

object RunAuthPersistenceProbe {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val flow = AuthDebugStore.load()
        if (flow == null) {
            println("No stored device flow found")
            return@runBlocking
        }

        val settingsViewModel = SettingsFeatureFactory.createViewModel()
        val state = settingsViewModel.pollLinking(flow)

        println("Auth Persistence Probe")
        println("Linked: ${state.authState.isLinked}")
        println("Auth in progress: ${state.authState.authInProgress}")
        println("Error: ${state.error ?: "none"}")
        println("Token store save called: ${RealDebridDebugState.lastTokenStoreSaveCalled.ifBlank { "none" }}")
        println("Token store write path: ${RealDebridDebugState.lastTokenStoreWritePath.ifBlank { "none" }}")
        println("Token store write exists: ${RealDebridDebugState.lastTokenStoreWriteExists.ifBlank { "none" }}")

        val path = RealDebridDebugState.lastTokenStoreWritePath
        if (path.isNotBlank()) {
            val file = File(path)
            println("Disk exists at reported path: ${file.exists()}")
            if (file.exists()) {
                println("Disk contents:")
                println(file.readText())
            }
        }
    }
}
