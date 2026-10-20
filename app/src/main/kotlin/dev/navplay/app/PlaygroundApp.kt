package dev.navplay.app

import android.app.Application
import dev.navplay.app.di.AppGraph
import dev.zacsweers.metro.createGraph

class PlaygroundApp : Application() {
    // One graph per process: navigation and session survive Activity recreation.
    val appGraph: AppGraph by lazy { createGraph<AppGraph>() }
}
