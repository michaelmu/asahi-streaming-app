package ai.shieldtv.app

import android.app.Application
import ai.shieldtv.app.di.AppContainer

class AsahiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.initialize(this)
    }
}
