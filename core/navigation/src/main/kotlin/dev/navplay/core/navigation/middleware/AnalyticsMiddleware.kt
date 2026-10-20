package dev.navplay.core.navigation.middleware

import dev.navplay.core.navigation.NavigationAnalytics
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationMiddleware
import dev.navplay.core.navigation.targetKey

/**
 * Один middleware вместо вызовов аналитики во всех фичах.
 * Стоит после auth/flags, поэтому видит финальный пункт назначения (а не то, что просили изначально).
 */
class AnalyticsMiddleware(private val analytics: NavigationAnalytics) : NavigationMiddleware {
    override val order = NavigationMiddleware.Order.ANALYTICS

    override suspend fun intercept(
        command: NavigationCommand,
        proceed: suspend (NavigationCommand) -> Unit,
    ) {
        command.targetKey?.let { key ->
            analytics.trackNavigation(
                destination = key::class.simpleName ?: "unknown",
                command = command::class.simpleName ?: "unknown",
            )
        }
        proceed(command)
    }
}
