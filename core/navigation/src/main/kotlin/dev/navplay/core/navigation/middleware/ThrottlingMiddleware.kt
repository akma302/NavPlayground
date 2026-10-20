package dev.navplay.core.navigation.middleware

import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.core.navigation.NavigationMiddleware

/**
 * Защита от двойного клика: такая же команда в пределах [windowMillis] отбрасывается.
 * Касается и Back — двойной тап по стрелке «назад» не должен закрывать два экрана.
 *
 * Состояние не потокобезопасно намеренно: навигатор обрабатывает команды строго последовательно.
 */
class ThrottlingMiddleware(
    private val log: NavigationLog,
    private val windowMillis: Long = 300,
    private val clock: () -> Long = System::currentTimeMillis,
) : NavigationMiddleware {
    override val order = NavigationMiddleware.Order.THROTTLE

    private var lastCommand: NavigationCommand? = null
    private var lastAcceptedAt = 0L

    override suspend fun intercept(
        command: NavigationCommand,
        proceed: suspend (NavigationCommand) -> Unit,
    ) {
        val now = clock()
        if (command == lastCommand && now - lastAcceptedAt < windowMillis) {
            log.log("throttle", "dropped duplicate $command")
            return
        }
        lastCommand = command
        lastAcceptedAt = now
        proceed(command)
    }
}
