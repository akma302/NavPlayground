package dev.navplay.app

import android.app.Application
import dev.navplay.app.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PlaygroundApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PlaygroundApp)
            allowOverride(false)
            modules(appModules)
        }
    }
}
