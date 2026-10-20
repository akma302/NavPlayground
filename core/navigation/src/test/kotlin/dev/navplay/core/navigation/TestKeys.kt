package dev.navplay.core.navigation

/** Ключи для тестов core: модуль не знает ни одной реальной фичи, поэтому тестируем на своих. */
data class TestKey(val id: String) : NavKey

data class AuthOnlyKey(val id: String) : NavKey, RequiresAuth

data class GatedKey(val id: String, override val featureId: String) : NavKey, FeatureGated

/** Записывает команды, дошедшие до «сердцевины» цепочки. */
class Recorder {
    val commands = mutableListOf<NavigationCommand>()
    val terminal: suspend (NavigationCommand) -> Unit = { commands += it }
}

/** Middleware, который пишет в общий журнал, когда вызван, — чтобы проверять порядок. */
class TracingMiddleware(
    private val name: String,
    override val order: Int,
    private val trace: MutableList<String>,
) : NavigationMiddleware {
    override suspend fun intercept(
        command: NavigationCommand,
        proceed: suspend (NavigationCommand) -> Unit,
    ) {
        trace += "$name:in"
        proceed(command)
        trace += "$name:out"
    }
}
