package ai.shieldtv.app.integration.debrid.realdebrid.repository

import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import ai.shieldtv.app.core.model.source.ResolvedStream
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.domain.repository.DebridRepository
import ai.shieldtv.app.integration.debrid.realdebrid.api.RealDebridApi
import ai.shieldtv.app.integration.debrid.realdebrid.mapper.RealDebridAuthMapper

class RealDebridRepositoryImpl(
    private val realDebridApi: RealDebridApi,
    private val realDebridAuthMapper: RealDebridAuthMapper
) : DebridRepository {
    override suspend fun getAuthState(): RealDebridAuthState {
        return RealDebridAuthState(isLinked = false)
    }

    override suspend fun startDeviceFlow(): DeviceCodeFlow {
        return realDebridAuthMapper.toDeviceCodeFlow(realDebridApi.startDeviceFlow())
    }

    override suspend fun pollDeviceFlow(flow: DeviceCodeFlow): RealDebridAuthState {
        return RealDebridAuthState(isLinked = false, authInProgress = true)
    }

    override suspend fun resolve(source: SourceResult): ResolvedStream {
        return ResolvedStream(url = source.url, source = source)
    }
}
