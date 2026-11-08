package dev.navplay.app.navigation

import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.ScreenRegistry
import dev.navplay.core.navigation.NavigationAnalytics
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.core.navigation.Navigator
import dev.navplay.core.navigation.StubNavKey

/** Экран-заглушка выключенной фичи. Его регистрирует `:app`: StubNavKey живёт в core и не знает про UI. */
class StubEntryContributor(
    private val navigator: Navigator,
) : NavEntryContributor {
    override fun register(scope: ScreenRegistry) {
        scope.entry(StubNavKey::class) { key ->
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
