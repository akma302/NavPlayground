package dev.navplay.core.navigation.testing

import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationResultException
import dev.navplay.core.navigation.Navigator
import dev.navplay.core.navigation.ResultKey

/**
 * Фейк для юнит-тестов ViewModel: ничего не навигирует, а записывает, что у него просили.
 * «Тест VM = fake Navigator + assert на эмитнутые команды», без Android.
 */
class FakeNavigator : Navigator {
    val commands = mutableListOf<NavigationCommand>()

    /** Ключи, для которых вызывали [forResult]. */
    val requestedResults = mutableListOf<NavKey>()

    /** Пары (ключ, результат), с которыми вызывали [finishWithResult]. */
    val finished = mutableListOf<Pair<NavKey, Any>>()

    /** Что вернуть из [forResult]. По умолчанию — «экран закрыли без результата». */
    var resultProvider: (NavKey) -> Result<Any> = { key ->
        Result.failure(NavigationResultException.Cancelled(key))
    }

    override fun execute(command: NavigationCommand) {
        commands += command
    }

    override suspend fun <T : Any> forResult(key: ResultKey<T>): Result<T> {
        requestedResults += key
        @Suppress("UNCHECKED_CAST")
        return resultProvider(key).map { it as T }
    }

    override fun <T : Any> finishWithResult(key: ResultKey<T>, result: T) {
        finished += key to result
    }
}
