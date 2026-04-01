package ai.shieldtv.app.domain.usecase.auth

import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.domain.repository.DebridRepository

class StartRealDebridDeviceFlowUseCase(
    private val debridRepository: DebridRepository
) {
    suspend operator fun invoke(): DeviceCodeFlow {
        return debridRepository.startDeviceFlow()
    }
}
