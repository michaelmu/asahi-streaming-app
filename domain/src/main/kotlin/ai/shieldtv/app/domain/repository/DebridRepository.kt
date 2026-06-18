package ai.codexa.app.domain.repository

import ai.codexa.app.core.model.auth.DeviceCodeFlow
import ai.codexa.app.core.model.auth.RealDebridAuthState
import ai.codexa.app.core.model.source.ResolvedStream
import ai.codexa.app.core.model.source.SourceResult

interface DebridRepository {
    suspend fun getAuthState(): RealDebridAuthState
    suspend fun startDeviceFlow(): DeviceCodeFlow
    suspend fun pollDeviceFlow(flow: DeviceCodeFlow): RealDebridAuthState
    suspend fun resolve(source: SourceResult): ResolvedStream
}
