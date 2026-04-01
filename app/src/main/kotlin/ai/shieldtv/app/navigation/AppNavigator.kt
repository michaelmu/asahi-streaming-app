package ai.shieldtv.app.navigation

class AppNavigator(
    initialDestination: AppDestination = AppDestination.SEARCH
) {
    var currentDestination: AppDestination = initialDestination
        private set

    fun goTo(destination: AppDestination) {
        currentDestination = destination
    }
}
