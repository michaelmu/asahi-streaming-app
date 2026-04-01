package ai.shieldtv.app.domain.usecase.auth

import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import ai.shieldtv.app.domain.repository.DebridRepository

class GetRealDebridAuthStateUseCase(
    private val debridRepository: DebridRepository
) {
    suspend operator fun invoke(): RealDebridAuthState {
        return debridRepository.getAuthState()
    }
}
