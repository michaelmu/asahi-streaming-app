package ai.shieldtv.app

import android.app.Activity
import android.os.Bundle
import ai.shieldtv.app.navigation.AppDestination
import ai.shieldtv.app.navigation.AppNavigator
import ai.shieldtv.app.ui.AppScreenRenderer
import ai.shieldtv.app.ui.AuthDebugTextViewFactory
import ai.shieldtv.app.ui.DebugTextViewFactory

class MainActivity : Activity() {
    private val appNavigator = AppNavigator(AppDestination.AUTH_DEBUG)
    private val appCoordinator = AppCoordinator(appNavigator)
    private val appScreenRenderer = AppScreenRenderer()
    private val debugTextViewFactory = DebugTextViewFactory()
    private val authDebugTextViewFactory = AuthDebugTextViewFactory()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = appCoordinator.currentState()
        val renderedScreen = appScreenRenderer.render(state)
        val view = when (state.destination) {
            AppDestination.AUTH_DEBUG -> authDebugTextViewFactory.create(this)
            else -> debugTextViewFactory.create(this, state, renderedScreen)
        }
        setContentView(view)
    }
}
