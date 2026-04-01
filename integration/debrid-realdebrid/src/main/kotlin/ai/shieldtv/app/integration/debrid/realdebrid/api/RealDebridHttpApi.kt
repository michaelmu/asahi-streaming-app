package ai.shieldtv.app.integration.debrid.realdebrid.api

import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridCredentialResponse
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenResponse
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenStore
import ai.shieldtv.app.integration.debrid.realdebrid.config.RealDebridConfig
import ai.shieldtv.app.integration.debrid.realdebrid.debug.RealDebridDebugState
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import org.json.JSONObject

class RealDebridHttpApi(
    private val httpClient: HttpClient,
    private val tokenStore: RealDebridTokenStore
) : RealDebridApi {
    override suspend fun startDeviceFlow(): DeviceFlowResponse {
        return runCatching {
            val clientId = RealDebridConfig.clientId()
            val request = HttpRequest.newBuilder()
                .uri(
                    URI.create(
                        "https://api.real-debrid.com/oauth/v2/device/code?client_id=" +
                            URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                            "&new_credentials=yes"
                    )
                )
                .GET()
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body()
            RealDebridDebugState.lastStartDeviceFlowResponse = response
            RealDebridDebugState.lastStartDeviceFlowError = ""
            val json = JSONObject(response)
            RealDebridDebugState.lastDirectVerificationUrl = json.optString("direct_verification_url")
            DeviceFlowResponse(
                deviceCode = json.optString("device_code"),
                verificationUrl = json.optString("verification_url"),
                userCode = json.optString("user_code"),
                qrCodeUrl = json.optString("direct_verification_url").ifBlank { null },
                expiresInSeconds = json.optInt("expires_in"),
                pollIntervalSeconds = json.optInt("interval").takeIf { it > 0 } ?: 5
            )
        }.getOrElse { error ->
            RealDebridDebugState.lastStartDeviceFlowError = error.message ?: error::class.java.simpleName
            DeviceFlowResponse(
                deviceCode = "",
                verificationUrl = "",
                userCode = "",
                expiresInSeconds = 0,
                pollIntervalSeconds = 5
            )
        }
    }

    override suspend fun getDeviceCredentials(deviceCode: String): RealDebridCredentialResponse? {
        if (deviceCode.isBlank()) return null
        return runCatching {
            val clientId = RealDebridConfig.clientId()
            val request = HttpRequest.newBuilder()
                .uri(
                    URI.create(
                        "https://api.real-debrid.com/oauth/v2/device/credentials?client_id=" +
                            URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                            "&code=" +
                            URLEncoder.encode(deviceCode, StandardCharsets.UTF_8)
                    )
                )
                .GET()
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body()
            RealDebridDebugState.lastCredentialsResponse = response
            RealDebridDebugState.lastCredentialsError = ""
            val json = JSONObject(response)
            val realClientId = json.optString("client_id")
            val realClientSecret = json.optString("client_secret")
            if (realClientId.isBlank() || realClientSecret.isBlank()) return null
            RealDebridCredentialResponse(realClientId, realClientSecret)
        }.getOrElse { error ->
            RealDebridDebugState.lastCredentialsError = error.message ?: error::class.java.simpleName
            null
        }
    }

    override suspend fun exchangeDeviceCredentialsForToken(
        deviceCode: String,
        clientId: String,
        clientSecret: String
    ): RealDebridTokenResponse? {
        if (deviceCode.isBlank() || clientId.isBlank() || clientSecret.isBlank()) return null
        return runCatching {
            val body = buildString {
                append("client_id=")
                append(URLEncoder.encode(clientId, StandardCharsets.UTF_8))
                append("&client_secret=")
                append(URLEncoder.encode(clientSecret, StandardCharsets.UTF_8))
                append("&code=")
                append(URLEncoder.encode(deviceCode, StandardCharsets.UTF_8))
                append("&grant_type=http://oauth.net/grant_type/device/1.0")
            }
            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.real-debrid.com/oauth/v2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body()
            RealDebridDebugState.lastTokenResponse = response
            RealDebridDebugState.lastTokenError = ""
            val json = JSONObject(response)
            val accessToken = json.optString("access_token")
            if (accessToken.isBlank()) return null
            RealDebridTokenResponse(
                accessToken = accessToken,
                refreshToken = json.optString("refresh_token").ifBlank { null },
                expiresInSeconds = json.optInt("expires_in").takeIf { it > 0 }
            )
        }.getOrElse { error ->
            RealDebridDebugState.lastTokenError = error.message ?: error::class.java.simpleName
            null
        }
    }

    override suspend fun instantAvailability(infoHashes: List<String>): String {
        if (infoHashes.isEmpty()) return "{}"
        val accessToken = tokenStore.get()?.accessToken ?: RealDebridConfig.accessToken() ?: return "{}"
        val joined = infoHashes.joinToString("/") { it.lowercase() }
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.real-debrid.com/rest/1.0/torrents/instantAvailability/$joined"))
            .header("Authorization", "Bearer $accessToken")
            .GET()
            .build()
        return runCatching {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body()
        }.getOrElse { "{}" }
    }
}
