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
}
