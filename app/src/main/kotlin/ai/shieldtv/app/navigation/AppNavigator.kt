package ai.shieldtv.app.navigation

class AppNavigator(
    initialDestination: AppDestination = AppDestination.HOME
) {
    var currentDestination: AppDestination = initialDestination
        private set

    fun goTo(destination: AppDestination) {
        currentDestination = destination
    }
}
