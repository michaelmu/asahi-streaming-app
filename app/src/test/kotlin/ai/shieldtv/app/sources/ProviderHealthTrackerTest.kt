package ai.shieldtv.app.sources

import ai.shieldtv.app.domain.repository.SourceFetchProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderHealthTrackerTest {
    @Test
    fun record_tracks_success_failure_latency_and_error_type() {
        val tracker = ProviderHealthTracker()

        tracker.record(
            SourceFetchProgress(
                providerId = "torrentio",
                providerDisplayName = "Torrentio",
                state = SourceFetchProgress.State.COMPLETED,
                resultCount = 12,
                latencyMs = 420
            )
        )
        tracker.record(
            SourceFetchProgress(
                providerId = "torrentio",
                providerDisplayName = "Torrentio",
                state = SourceFetchProgress.State.FAILED,
                latencyMs = 800,
                errorType = "Timeout",
                message = "took too long"
            )
        )

        val snapshot = tracker.snapshot().single()
        assertEquals(1, snapshot.successes)
        assertEquals(1, snapshot.failures)
        assertEquals(800L, snapshot.lastLatencyMs)
        assertEquals("Timeout", snapshot.lastErrorType)
        val summary = tracker.summary()
        assertTrue(summary.contains("Torrentio"))
        assertTrue(summary.contains("ok=1"))
        assertTrue(summary.contains("fail=1"))
        assertTrue(summary.contains("error=Timeout"))
    }

    @Test
    fun reset_clears_health_state() {
        val tracker = ProviderHealthTracker()
        tracker.record(
            SourceFetchProgress(
                providerId = "comet",
                providerDisplayName = "Comet",
                state = SourceFetchProgress.State.COMPLETED,
                resultCount = 4,
                latencyMs = 200
            )
        )

        tracker.reset()

        assertTrue(tracker.snapshot().isEmpty())
        assertEquals("providerHealth=none", tracker.summary())
    }
}
