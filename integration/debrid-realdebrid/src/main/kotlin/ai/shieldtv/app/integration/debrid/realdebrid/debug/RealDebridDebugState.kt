package ai.shieldtv.app.integration.debrid.realdebrid.debug

object RealDebridDebugState {
    @Volatile
    var lastApiMode: String = "unknown"

    @Volatile
    var lastStartDeviceFlowResponse: String = ""

    @Volatile
    var lastStartDeviceFlowError: String = ""

    @Volatile
    var lastDirectVerificationUrl: String = ""

    @Volatile
    var lastCredentialsResponse: String = ""

    @Volatile
    var lastCredentialsError: String = ""

    @Volatile
    var lastTokenResponse: String = ""

    @Volatile
    var lastTokenError: String = ""

    @Volatile
    var lastInstantAvailabilityRequest: String = ""

    @Volatile
    var lastInstantAvailabilityResponse: String = ""

    @Volatile
    var lastInstantAvailabilityError: String = ""

    @Volatile
    var lastCacheMarkerHashCount: String = ""

    @Volatile
    var lastCacheMarkerCachedCount: String = ""

    @Volatile
    var lastSourceRepositorySeen: String = ""

    @Volatile
    var lastSourceRepositoryMarkerPresent: String = ""

    @Volatile
    var lastTokenStoreSaveCalled: String = ""

    @Volatile
    var lastTokenStoreWritePath: String = ""

    @Volatile
    var lastTokenStoreWriteExists: String = ""
}
