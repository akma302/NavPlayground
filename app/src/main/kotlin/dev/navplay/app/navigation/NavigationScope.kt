package dev.navplay.app.navigation

import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named

/** Qualifier for the process-level navigation CoroutineScope; not a Koin scope. */
val NavigationScope: Qualifier = named("navigation.coroutines")
