package dev.navplay.core.navigation

/**
 * Собирает middleware в «луковицу»: первый по [NavigationMiddleware.order] — самый внешний слой,
 * [terminal] — самая сердцевина (реальное изменение бэкстека).
 *
 * Чистый Kotlin, без Android — поэтому цепочка тестируется обычными JVM-тестами.
 */
class NavigationPipeline(
    middleware: Collection<NavigationMiddleware>,
    terminal: suspend (NavigationCommand) -> Unit,
) {
    /** Middleware в порядке исполнения (для отладки и тестов). */
    val ordered: List<NavigationMiddleware> = middleware.sortedBy { it.order }

    private val chain: suspend (NavigationCommand) -> Unit =
        ordered.foldRight(terminal) { mw, next ->
            val step: suspend (NavigationCommand) -> Unit = { command -> mw.intercept(command, next) }
            step
        }

    suspend fun run(command: NavigationCommand) {
        chain(command)
    }
}
