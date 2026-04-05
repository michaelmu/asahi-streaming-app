package ai.shieldtv.app.debug

import android.content.Context
import java.io.File
import java.time.Instant

class PlaybackCrashLogStore(context: Context) {
    private val crashFile = File(context.filesDir, "debug/last-playback-crash.txt")
    private val contextFile = File(context.filesDir, "debug/last-playback-context.txt")

    fun savePlaybackContext(text: String) {
        contextFile.parentFile?.mkdirs()
        contextFile.writeText(text)
    }

    fun readPlaybackContext(): String? =
        contextFile.takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }

    fun recordFatalThrowable(threadName: String, throwable: Throwable) {
        crashFile.parentFile?.mkdirs()
        val stack = throwable.stackTraceToString().trim()
        crashFile.writeText(
            buildString {
                appendLine("timestamp=${Instant.now()}")
                appendLine("thread=$threadName")
                appendLine("type=${throwable::class.java.name}")
                appendLine("message=${throwable.message ?: "none"}")
                appendLine("stacktrace_start")
                appendLine(stack)
                appendLine("stacktrace_end")
            }.trim()
        )
    }

    fun readFatalThrowable(): String? =
        crashFile.takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }

    fun consumeFatalReport(): String? {
        val crash = readFatalThrowable() ?: return null
        val context = readPlaybackContext()
        crashFile.delete()
        return buildString {
            appendLine(crash)
            context?.let {
                appendLine()
                appendLine("playback_context_start")
                appendLine(it)
                appendLine("playback_context_end")
            }
        }.trim()
    }
}
