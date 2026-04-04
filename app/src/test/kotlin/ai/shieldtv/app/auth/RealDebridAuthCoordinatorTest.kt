package ai.shieldtv.app.auth

import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RealDebridAuthCoordinatorTest {
    private val dispatcher = StandardTestDispatcher()

    @Test
    fun startLink_returns_success_and_flow() = runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
            val coordinator = RealDebridAuthCoordinator(
                scope = this,
                startDeviceFlow = { deviceFlow() },
                pollDeviceFlow = { RealDebridAuthState(isLinked = true, username = "mike") },
                clearAuth = {},
                buildAuthUrl = { it.verificationUrl },
                buildStartFailureMessage = { "boom" }
            )

            val result = coordinator.startLink()
            assertTrue(result is RealDebridLinkStartResult.Success)
            val success = (result as RealDebridLinkStartResult.Success).value
            assertEquals("ABCD-1234", success.flow.userCode)
            assertFalse(success.authState.isLinked)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun startLink_returns_typed_failure() = runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
            val coordinator = RealDebridAuthCoordinator(
                scope = this,
                startDeviceFlow = { error("nope") },
                pollDeviceFlow = { RealDebridAuthState(isLinked = true, username = "mike") },
                clearAuth = {},
                buildAuthUrl = { it.verificationUrl },
                buildStartFailureMessage = { "boom" }
            )

            val result = coordinator.startLink()
            assertTrue(result is RealDebridLinkStartResult.Failure)
            val failure = (result as RealDebridLinkStartResult.Failure).value
            assertEquals("StartFailed", failure.errorType)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun startAutoPolling_reports_link_success() = runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
            val coordinator = RealDebridAuthCoordinator(
                scope = this,
                startDeviceFlow = { deviceFlow() },
                pollDeviceFlow = { RealDebridAuthState(isLinked = true, username = "mike") },
                clearAuth = {},
                buildAuthUrl = { it.verificationUrl },
                buildStartFailureMessage = { "boom" }
            )

            var authState = RealDebridAuthState(isLinked = false, authInProgress = true)
            var linkedMessage: String? = null
            coordinator.startAutoPolling(
                flow = deviceFlow(),
                currentAuthState = { authState },
                onStateUpdated = {
                    authState = it.authState
                    linkedMessage = it.linkedMessage
                },
                onTimeout = {}
            )

            advanceTimeBy(2_100)
            advanceUntilIdle()

            assertTrue(authState.isLinked)
            assertNotNull(linkedMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun deviceFlow() = DeviceCodeFlow(
        deviceCode = "device-code",
        userCode = "ABCD-1234",
        verificationUrl = "https://real-debrid.com/device",
        directVerificationUrl = null,
        qrCodeUrl = null,
        expiresInSeconds = 600,
        pollIntervalSeconds = 2
    )
}
