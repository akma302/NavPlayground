package dev.navplay.app.di

import dev.navplay.app.navigation.NavigatorImpl
import dev.navplay.core.di.AppScope
import dev.navplay.core.navigation.BackStackCodec
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavigationLog
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

/** Contributions are discovered from all feature implementations on the classpath. */
@DependencyGraph(AppScope::class)
interface AppGraph : ViewModelGraph {
    val navigator: NavigatorImpl
    val contributors: Set<NavEntryContributor>
    val codec: BackStackCodec
    val log: NavigationLog
}
