package ai.shieldtv.app.sources

import ai.shieldtv.app.domain.repository.SourceFetchProgress

data class ProviderHealthSnapshot(
    val providerId: String,
    val providerDisplayName: String,
    val successes: Int,
    val failures: Int,
    val lastLatencyMs: Long?,
    val lastResultCount: Int?,
    val lastErrorType: String?,
    val lastMessage: String?
)

class ProviderHealthTracker {
    private val snapshots = linkedMapOf<String, ProviderHealthSnapshot>()

    fun record(progress: SourceFetchProgress) {
        val current = snapshots[progress.providerId]
        snapshots[progress.providerId] = when (progress.state) {
            SourceFetchProgress.State.STARTED -> current ?: ProviderHealthSnapshot(
                providerId = progress.providerId,
                providerDisplayName = progress.providerDisplayName,
                successes = 0,
                failures = 0,
                lastLatencyMs = null,
                lastResultCount = null,
                lastErrorType = null,
                lastMessage = null
            )
            SourceFetchProgress.State.COMPLETED -> ProviderHealthSnapshot(
                providerId = progress.providerId,
                providerDisplayName = progress.providerDisplayName,
                successes = (current?.successes ?: 0) + 1,
                failures = current?.failures ?: 0,
                lastLatencyMs = progress.latencyMs,
                lastResultCount = progress.resultCount,
                lastErrorType = null,
                lastMessage = null
            )
            SourceFetchProgress.State.FAILED -> ProviderHealthSnapshot(
                providerId = progress.providerId,
                providerDisplayName = progress.providerDisplayName,
                successes = current?.successes ?: 0,
                failures = (current?.failures ?: 0) + 1,
                lastLatencyMs = progress.latencyMs,
                lastResultCount = progress.resultCount,
                lastErrorType = progress.errorType,
                lastMessage = progress.message
            )
        }
    }

    fun reset() {
        snapshots.clear()
    }

    fun snapshot(): List<ProviderHealthSnapshot> = snapshots.values.toList()

    fun summary(): String {
        if (snapshots.isEmpty()) return "providerHealth=none"
        return snapshots.values.joinToString("; ") { snapshot ->
            buildString {
                append(snapshot.providerDisplayName)
                append(" ok=")
                append(snapshot.successes)
                append(" fail=")
                append(snapshot.failures)
                snapshot.lastLatencyMs?.let {
                    append(" latency=")
                    append(it)
                    append("ms")
                }
                snapshot.lastResultCount?.let {
                    append(" results=")
                    append(it)
                }
                snapshot.lastErrorType?.let {
                    append(" error=")
                    append(it)
                }
            }
        }
    }
}
