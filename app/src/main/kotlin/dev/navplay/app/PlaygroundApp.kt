package dev.navplay.app

import android.app.Application
import dev.navplay.app.di.AppContainer

class PlaygroundApp : Application() {
    val appContainer: AppContainer by lazy { AppContainer() }
}
