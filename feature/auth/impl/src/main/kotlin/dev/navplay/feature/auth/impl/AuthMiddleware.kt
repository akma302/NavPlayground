package dev.navplay.feature.auth.impl

import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.core.navigation.NavigationMiddleware
import dev.navplay.core.navigation.RequiresAuth
import dev.navplay.core.navigation.targetKey
import dev.navplay.core.navigation.withTargetKey
import dev.navplay.feature.auth.api.AuthNavKey
import dev.navplay.feature.auth.api.SessionManager
import javax.inject.Inject

/**
 * Auth-guard: «как открыть экран, требующий авторизации?».
 *
 * Команду с ключом [RequiresAuth] от неавторизованного пользователя заменяем на открытие экрана входа,
 * а исходный ключ кладём в `returnTo`. После логина экран входа делает `Replace(returnTo)`.
 * Тип команды сохраняется (Forward остаётся Forward, Replace — Replace).
 */
class AuthMiddleware @Inject constructor(
    private val sessions: SessionManager,
    private val log: NavigationLog,
) : NavigationMiddleware {
    override val order = NavigationMiddleware.Order.AUTH

    override suspend fun intercept(
        command: NavigationCommand,
        proceed: suspend (NavigationCommand) -> Unit,
    ) {
        val key = command.targetKey
        if (key is RequiresAuth && !sessions.isAuthorized) {
            log.log("auth", "guard: ${key::class.simpleName} requires auth -> redirect to sign-in")
            proceed(command.withTargetKey(AuthNavKey(returnTo = key)))
        } else {
            proceed(command)
        }
    }
}
