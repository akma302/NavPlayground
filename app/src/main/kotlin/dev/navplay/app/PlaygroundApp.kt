package dev.navplay.app

import android.app.Application
import dev.navplay.app.di.AppComponent
import dev.navplay.app.di.DaggerAppComponent

class PlaygroundApp : Application() {
    // Activity пересоздаётся при повороте, а навигатор и сессия должны пережить её.
    val component: AppComponent by lazy { DaggerAppComponent.create() }
}
