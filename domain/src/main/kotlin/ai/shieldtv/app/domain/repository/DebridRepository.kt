package ai.shieldtv.app.domain.repository

import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import ai.shieldtv.app.core.model.source.ResolvedStream
import ai.shieldtv.app.core.model.source.SourceResult

interface DebridRepository {
    suspend fun getAuthState(): RealDebridAuthState
    suspend fun startDeviceFlow(): DeviceCodeFlow
    suspend fun pollDeviceFlow(flow: DeviceCodeFlow): RealDebridAuthState
    suspend fun resolve(source: SourceResult): ResolvedStream
}
