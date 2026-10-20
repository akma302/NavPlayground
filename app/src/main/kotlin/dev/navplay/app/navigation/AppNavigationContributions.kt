package dev.navplay.app.navigation

import androidx.navigation3.runtime.EntryProviderScope
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavigationAnalytics
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.core.navigation.Navigator
import dev.navplay.core.navigation.StubNavKey
import dev.zacsweers.metro.Inject

/** Экран-заглушка выключенной фичи. Его регистрирует `:app`: StubNavKey живёт в core и не знает про UI. */
@Inject
class StubEntryContributor(
    private val navigator: Navigator,
) : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<StubNavKey> { key ->
            PlaceholderScreen(
                title = "Feature unavailable",
                text = "'${key.feature}': ${key.reason}",
                onBack = { navigator.execute(NavigationCommand.Back) },
            )
        }
    }
}

/** Аналитика «в одном месте»: в playground просто пишет в журнал, который виден на экране Feed. */
class LogNavigationAnalytics(private val log: NavigationLog) : NavigationAnalytics {
    override fun trackNavigation(destination: String, command: String) {
        log.log("analytics", "screen_view destination=$destination via=$command")
    }
}
