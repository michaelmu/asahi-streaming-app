package ai.shieldtv.app

import android.app.Activity
import android.os.Bundle
import ai.shieldtv.app.navigation.AppNavigator
import ai.shieldtv.app.ui.AppScreenRenderer
import ai.shieldtv.app.ui.DebugTextViewFactory

class MainActivity : Activity() {
    private val appNavigator = AppNavigator()
    private val appCoordinator = AppCoordinator(appNavigator)
    private val appScreenRenderer = AppScreenRenderer()
    private val debugTextViewFactory = DebugTextViewFactory()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = appCoordinator.currentState()
        val renderedScreen = appScreenRenderer.render(state)
        setContentView(debugTextViewFactory.create(this, state, renderedScreen))
    }
}
