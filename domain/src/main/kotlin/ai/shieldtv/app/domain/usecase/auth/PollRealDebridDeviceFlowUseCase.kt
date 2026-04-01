package ai.shieldtv.app.domain.usecase.auth

import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import ai.shieldtv.app.domain.repository.DebridRepository

class PollRealDebridDeviceFlowUseCase(
    private val debridRepository: DebridRepository
) {
    suspend operator fun invoke(flow: DeviceCodeFlow): RealDebridAuthState {
        return debridRepository.pollDeviceFlow(flow)
    }
}
