package dev.navplay.core.navigation.middleware

import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.core.navigation.NavigationMiddleware

/** Самый внешний слой: видит исходную команду до того, как её изменит любой другой middleware. */
class LoggingMiddleware(private val log: NavigationLog) : NavigationMiddleware {
    override val order = NavigationMiddleware.Order.LOGGING

    override suspend fun intercept(
        command: NavigationCommand,
        proceed: suspend (NavigationCommand) -> Unit,
    ) {
        log.log("nav", "-> $command")
        proceed(command)
    }
}
