package dev.navplay.app.navigation

import dev.zacsweers.metro.Qualifier

/** Корутин-скоуп навигатора: живёт столько же, сколько процесс, работает на Dispatchers.Main.immediate. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NavigationScope
