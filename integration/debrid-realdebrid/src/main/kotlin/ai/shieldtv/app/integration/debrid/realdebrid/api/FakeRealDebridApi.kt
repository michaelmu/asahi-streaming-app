package ai.shieldtv.app.integration.debrid.realdebrid.api

import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridCredentialResponse
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenResponse

class FakeRealDebridApi : RealDebridApi {
    override suspend fun startDeviceFlow(): DeviceFlowResponse {
        return DeviceFlowResponse(
            deviceCode = "device-code-asahi",
            verificationUrl = "https://real-debrid.example/device",
            directVerificationUrl = "https://real-debrid.example/device/direct?code=ASAHI1",
            userCode = "ASAHI1",
            expiresInSeconds = 900,
            pollIntervalSeconds = 5
        )
    }

    override suspend fun getDeviceCredentials(deviceCode: String): RealDebridCredentialResponse? {
        return RealDebridCredentialResponse(
            clientId = "fake-client-id",
            clientSecret = "fake-client-secret"
        )
    }

    override suspend fun exchangeDeviceCredentialsForToken(
        deviceCode: String,
        clientId: String,
        clientSecret: String
    ): RealDebridTokenResponse? {
        return RealDebridTokenResponse(
            accessToken = "fake-access-token",
            refreshToken = "fake-refresh-token",
            expiresInSeconds = 3600
        )
    }

    override suspend fun instantAvailability(infoHashes: List<String>): String {
        val body = infoHashes.joinToString(prefix = "{", postfix = "}") { hash ->
            val available = hash.lowercase().takeLast(1).toIntOrNull(16)?.rem(2) == 0
            if (available) "\"$hash\": { \"rd\": [\"cached\"] }" else "\"$hash\": {}"
        }
        return body
    }

    override suspend fun addMagnet(magnet: String): RealDebridTorrentAddResponse? {
        return RealDebridTorrentAddResponse(id = "fake-torrent-id", uri = magnet)
    }

    override suspend fun getTorrentInfo(torrentId: String): RealDebridTorrentInfo? {
        return RealDebridTorrentInfo(
            id = torrentId,
            filename = "fake-file.mkv",
            status = "downloaded",
            links = listOf("https://example.invalid/fake-stream.mkv"),
            files = listOf(
                RealDebridTorrentFile(
                    id = 1,
                    path = "/fake-file.mkv",
                    bytes = 1_500_000_000,
                    selected = 1
                )
            )
        )
    }

    override suspend fun selectTorrentFiles(torrentId: String, fileIdsCsv: String): Boolean = true

    override suspend fun unrestrictLink(link: String): RealDebridUnrestrictedLink? {
        return RealDebridUnrestrictedLink(
            download = link,
            filename = "fake-file.mkv",
            mimeType = "video/x-matroska"
        )
    }
}
