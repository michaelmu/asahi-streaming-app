package ai.shieldtv.app.integration.debrid.realdebrid.repository

import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import ai.shieldtv.app.core.model.source.ResolvedStream
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.repository.DebridRepository
import ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridApi
import ai.shieldtv.app.integration.debrid.realdebrid.auth.RealDebridTokenStore
import ai.shieldtv.app.integration.debrid.realdebrid.mapper.RealDebridAuthMapper

class RealDebridRepositoryImpl(
    private val realDebridApi: RealDebridApi,
    private val realDebridAuthMapper: RealDebridAuthMapper,
    private val tokenStore: RealDebridTokenStore
) : DebridRepository {
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
        val credentials = realDebridApi.getDeviceCredentials(flow.userCode) ?: return RealDebridAuthState(
            isLinked = false,
            authInProgress = true,
            lastError = null
        )
        val tokenResponse = realDebridApi.exchangeDeviceCredentialsForToken(
            deviceCode = flow.userCode,
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
        return ResolvedStream(url = source.url, source = source)
    }
}
