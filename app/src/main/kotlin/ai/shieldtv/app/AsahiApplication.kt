package ai.shieldtv.app

import android.app.Application
import ai.shieldtv.app.debug.PlaybackCrashLogStore
import ai.shieldtv.app.di.AppContainer

class AsahiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.initialize(this)
        installCrashLogging()
    }

    private fun installCrashLogging() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        val crashLogStore = PlaybackCrashLogStore(this)
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                crashLogStore.recordFatalThrowable(
                    threadName = thread.name,
                    throwable = throwable
                )
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
