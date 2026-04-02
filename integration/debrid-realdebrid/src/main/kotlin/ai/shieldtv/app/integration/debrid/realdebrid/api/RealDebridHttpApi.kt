package ai.shieldtv.app.integration.debrid.realdebrid.api

import ai.shieldtv.app.core.network.http.HttpClient
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridCredentialResponse
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenResponse
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenStore
import ai.shieldtv.app.integration.debrid.realdebrid.config.RealDebridConfig
import ai.shieldtv.app.integration.debrid.realdebrid.debug.RealDebridDebugState
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.json.JSONArray
import org.json.JSONObject

class RealDebridHttpApi(
    private val httpClient: HttpClient,
    private val tokenStore: RealDebridTokenStore
) : RealDebridApi {
    override suspend fun startDeviceFlow(): DeviceFlowResponse {
        return runCatching {
            val clientId = RealDebridConfig.clientId()
            val response = httpClient.get(
                url =
                    "https://api.real-debrid.com/oauth/v2/device/code?client_id=" +
                        URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                        "&new_credentials=yes"
            )
            RealDebridDebugState.lastStartDeviceFlowResponse = response
            RealDebridDebugState.lastStartDeviceFlowError = ""
            val json = JSONObject(response)
            RealDebridDebugState.lastDirectVerificationUrl = json.optString("direct_verification_url")
            DeviceFlowResponse(
                deviceCode = json.optString("device_code"),
                verificationUrl = json.optString("verification_url"),
                directVerificationUrl = json.optString("direct_verification_url").ifBlank { null },
                userCode = json.optString("user_code"),
                expiresInSeconds = json.optInt("expires_in"),
                pollIntervalSeconds = json.optInt("interval").takeIf { it > 0 } ?: 5
            )
        }.getOrElse { error ->
            RealDebridDebugState.lastStartDeviceFlowError = error.message ?: error::class.java.simpleName
            DeviceFlowResponse(
                deviceCode = "",
                verificationUrl = "",
                directVerificationUrl = null,
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
            val response = httpClient.get(
                url =
                    "https://api.real-debrid.com/oauth/v2/device/credentials?client_id=" +
                        URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                        "&code=" +
                        URLEncoder.encode(deviceCode, StandardCharsets.UTF_8)
            )
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
            val response = httpClient.post(
                url = "https://api.real-debrid.com/oauth/v2/token",
                body = body,
                headers = mapOf("Content-Type" to "application/x-www-form-urlencoded")
            )
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
        val url = "https://api.real-debrid.com/rest/1.0/torrents/instantAvailability/$joined"
        RealDebridDebugState.lastInstantAvailabilityRequest = joined
        return runCatching {
            httpClient.get(
                url = url,
                headers = mapOf("Authorization" to "Bearer $accessToken")
            ).also {
                RealDebridDebugState.lastInstantAvailabilityResponse = it
                RealDebridDebugState.lastInstantAvailabilityError = ""
            }
        }.getOrElse { error ->
            RealDebridDebugState.lastInstantAvailabilityResponse = ""
            RealDebridDebugState.lastInstantAvailabilityError = error.message ?: error::class.java.simpleName
            "{}"
        }
    }

    override suspend fun addMagnet(magnet: String): RealDebridTorrentAddResponse? {
        val accessToken = tokenStore.get()?.accessToken ?: RealDebridConfig.accessToken() ?: return null
        val body = "magnet=${URLEncoder.encode(magnet, StandardCharsets.UTF_8)}"
        return runCatching {
            val response = httpClient.post(
                url = "https://api.real-debrid.com/rest/1.0/torrents/addMagnet",
                body = body,
                headers = mapOf(
                    "Authorization" to "Bearer $accessToken",
                    "Content-Type" to "application/x-www-form-urlencoded"
                )
            )
            val json = JSONObject(response)
            RealDebridTorrentAddResponse(
                id = json.optString("id"),
                uri = json.optString("uri").ifBlank { null }
            )
        }.getOrNull()
    }

    override suspend fun getTorrentInfo(torrentId: String): RealDebridTorrentInfo? {
        val accessToken = tokenStore.get()?.accessToken ?: RealDebridConfig.accessToken() ?: return null
        return runCatching {
            val response = httpClient.get(
                url = "https://api.real-debrid.com/rest/1.0/torrents/info/$torrentId",
                headers = mapOf("Authorization" to "Bearer $accessToken")
            )
            val json = JSONObject(response)
            RealDebridTorrentInfo(
                id = json.optString("id"),
                filename = json.optString("filename"),
                status = json.optString("status"),
                links = json.optJSONArray("links").toStringList(),
                files = json.optJSONArray("files").toTorrentFiles()
            )
        }.getOrNull()
    }

    override suspend fun selectTorrentFiles(torrentId: String, fileIdsCsv: String): Boolean {
        val accessToken = tokenStore.get()?.accessToken ?: RealDebridConfig.accessToken() ?: return false
        val body = "files=${URLEncoder.encode(fileIdsCsv, StandardCharsets.UTF_8)}"
        return runCatching {
            httpClient.post(
                url = "https://api.real-debrid.com/rest/1.0/torrents/selectFiles/$torrentId",
                body = body,
                headers = mapOf(
                    "Authorization" to "Bearer $accessToken",
                    "Content-Type" to "application/x-www-form-urlencoded"
                )
            )
            true
        }.getOrDefault(false)
    }

    override suspend fun unrestrictLink(link: String): RealDebridUnrestrictedLink? {
        val accessToken = tokenStore.get()?.accessToken ?: RealDebridConfig.accessToken() ?: return null
        val body = "link=${URLEncoder.encode(link, StandardCharsets.UTF_8)}"
        return runCatching {
            val response = httpClient.post(
                url = "https://api.real-debrid.com/rest/1.0/unrestrict/link",
                body = body,
                headers = mapOf(
                    "Authorization" to "Bearer $accessToken",
                    "Content-Type" to "application/x-www-form-urlencoded"
                )
            )
            val json = JSONObject(response)
            RealDebridUnrestrictedLink(
                download = json.optString("download"),
                filename = json.optString("filename").ifBlank { null },
                mimeType = json.optString("mimeType").ifBlank { null }
            )
        }.getOrNull()
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    private fun JSONArray?.toTorrentFiles(): List<RealDebridTorrentFile> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(
                    RealDebridTorrentFile(
                        id = item.optInt("id"),
                        path = item.optString("path"),
                        bytes = item.optLong("bytes"),
                        selected = item.optInt("selected")
                    )
                )
            }
        }
    }
}
