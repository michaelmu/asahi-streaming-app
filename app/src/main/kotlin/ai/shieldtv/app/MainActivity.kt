package ai.shieldtv.app

import android.app.Activity
import android.os.Bundle
import ai.shieldtv.app.navigation.AppNavigator
import ai.shieldtv.app.ui.AppScreenRenderer

class MainActivity : Activity() {
    private val appNavigator = AppNavigator()
    private val appCoordinator = AppCoordinator(appNavigator)
    private val appScreenRenderer = AppScreenRenderer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = appCoordinator.currentState()
        appScreenRenderer.render(state)
    }
}
