package dev.navplay.core.navigation.middleware

import dev.navplay.core.navigation.DeepLinkResolver
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.core.navigation.NavigationMiddleware

/**
 * Превращает `DeepLink(uri)` в обычный `Forward(key)` и отдаёт его ДАЛЬШЕ по цепочке.
 *
 * Именно поэтому deep link — «тот же пайплайн»: после этого шага команда неотличима от внутреннего
 * перехода и проходит фича-флаги, auth-guard и аналитику. Стоит после валидации (она смотрит на исходный
 * URI) и до флагов/auth (они смотрят на целевой ключ).
 */
class DeepLinkResolutionMiddleware(
    private val resolver: DeepLinkResolver,
    private val log: NavigationLog,
) : NavigationMiddleware {
    override val order = NavigationMiddleware.Order.DEEP_LINK_RESOLUTION

    override suspend fun intercept(
        command: NavigationCommand,
        proceed: suspend (NavigationCommand) -> Unit,
    ) {
        if (command !is NavigationCommand.DeepLink) {
            proceed(command)
            return
        }
        val key = resolver.resolve(command.uri)
        if (key == null) {
            log.log("deeplink", "no handler for ${command.uri}")
            return
        }
        log.log("deeplink", "${command.uri} -> ${key::class.simpleName}")
        proceed(NavigationCommand.Forward(key))
    }
}
