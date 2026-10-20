package dev.navplay.core.navigation.middleware

import dev.navplay.core.navigation.DeepLinkResolver
import dev.navplay.core.navigation.DeepLinkSource
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.core.navigation.NavigationMiddleware

/**
 * Allowlist для внешних ссылок: схема должна быть разрешена, обработчик — найден и `exported`.
 * Защищает внутренние экраны от открытия извне. Внутренние ссылки не проверяются.
 */
class DeepLinkValidationMiddleware(
    private val resolver: DeepLinkResolver,
    private val log: NavigationLog,
    private val allowedSchemes: Set<String>,
) : NavigationMiddleware {
    override val order = NavigationMiddleware.Order.DEEP_LINK_VALIDATION

    override suspend fun intercept(
        command: NavigationCommand,
        proceed: suspend (NavigationCommand) -> Unit,
    ) {
        if (command is NavigationCommand.DeepLink && command.source == DeepLinkSource.External) {
            val scheme = command.uri.substringBefore("://", missingDelimiterValue = "").lowercase()
            if (scheme !in allowedSchemes) {
                log.log("deeplink", "rejected: scheme '$scheme' is not allowed (${command.uri})")
                return
            }
            val match = resolver.match(command.uri)
            if (match == null) {
                log.log("deeplink", "rejected: no handler for ${command.uri}")
                return
            }
            if (!match.handler.exported) {
                log.log("deeplink", "rejected: ${command.uri} is not exported (internal-only screen)")
                return
            }
        }
        proceed(command)
    }
}
