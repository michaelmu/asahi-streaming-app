package ai.shieldtv.app

import android.app.Activity
import android.os.Bundle
import ai.shieldtv.app.navigation.AppNavigator

class MainActivity : Activity() {
    private val appNavigator = AppNavigator()
    private val appCoordinator = AppCoordinator(appNavigator)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appCoordinator.currentState()
    }
}
