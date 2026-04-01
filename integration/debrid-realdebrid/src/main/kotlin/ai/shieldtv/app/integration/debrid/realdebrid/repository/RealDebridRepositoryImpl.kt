package ai.shieldtv.app.integration.debrid.realdebrid.repository

import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import ai.shieldtv.app.core.model.source.ResolvedStream
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.repository.DebridRepository
import ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridApi
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenStore
import ai.shieldtv.app.integration.debrid.realdebrid.mapper.RealDebridAuthMapper
import ai.shieldtv.app.integration.debrid.realdebrid.resolver.RealDebridResolver

class RealDebridRepositoryImpl(
    private val realDebridApi: RealDebridApi,
    private val realDebridAuthMapper: RealDebridAuthMapper,
    private val tokenStore: RealDebridTokenStore
) : DebridRepository {
    private val resolver = RealDebridResolver(realDebridApi)
    override suspend fun getAuthState(): RealDebridAuthState {
        val tokens = tokenStore.get()
        return RealDebridAuthState(
            isLinked = tokens != null,
            username = if (tokens != null) "linked" else null,
            authInProgress = false,
            lastError = null
        )
    }

    override suspend fun startDeviceFlow(): DeviceCodeFlow {
        return realDebridAuthMapper.toDeviceCodeFlow(realDebridApi.startDeviceFlow())
    }

    override suspend fun pollDeviceFlow(flow: DeviceCodeFlow): RealDebridAuthState {
        val credentials = realDebridApi.getDeviceCredentials(flow.deviceCode) ?: return RealDebridAuthState(
            isLinked = false,
            authInProgress = true,
            lastError = null
        )
        val tokenResponse = realDebridApi.exchangeDeviceCredentialsForToken(
            deviceCode = flow.deviceCode,
            clientId = credentials.clientId,
            clientSecret = credentials.clientSecret
        ) ?: return RealDebridAuthState(
            isLinked = false,
            authInProgress = true,
            lastError = null
        )
        tokenStore.save(
            ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokens(
                accessToken = tokenResponse.accessToken,
                refreshToken = tokenResponse.refreshToken,
                expiresInSeconds = tokenResponse.expiresInSeconds
            )
        )
        return RealDebridAuthState(
            isLinked = true,
            username = "linked",
            authInProgress = false,
            lastError = null
        )
    }

    override suspend fun resolve(source: SourceResult): ResolvedStream {
        val url = source.url.trim()
        return when {
            url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true) -> {
                ResolvedStream(url = url, source = source)
            }
            url.startsWith("magnet:", ignoreCase = true) -> {
                val resolved = resolver.resolveMagnet(url, source.mediaRef)
                ResolvedStream(
                    url = resolved.streamUrl,
                    mimeType = resolved.mimeType,
                    source = source
                )
            }
            else -> {
                throw IllegalStateException("Unsupported source URL for playback: $url")
            }
        }
    }
}
