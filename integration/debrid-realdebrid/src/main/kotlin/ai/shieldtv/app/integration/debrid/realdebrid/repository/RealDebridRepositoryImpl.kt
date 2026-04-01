package ai.shieldtv.app.integration.debrid.realdebrid.repository

import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import ai.shieldtv.app.core.model.source.ResolvedStream
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.repository.DebridRepository

class RealDebridRepositoryImpl : DebridRepository {
    override suspend fun getAuthState(): RealDebridAuthState {
        return RealDebridAuthState(isLinked = false)
    }

    override suspend fun startDeviceFlow(): DeviceCodeFlow {
        return DeviceCodeFlow(
            verificationUrl = "",
            userCode = "",
            expiresInSeconds = 0,
            pollIntervalSeconds = 0
        )
    }

    override suspend fun pollDeviceFlow(flow: DeviceCodeFlow): RealDebridAuthState {
        return RealDebridAuthState(isLinked = false, authInProgress = true)
    }

    override suspend fun resolve(source: SourceResult): ResolvedStream {
        return ResolvedStream(url = source.url, source = source)
    }
}
