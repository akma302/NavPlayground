package dev.navplay.core.navigation

/** Единственное, что видят фичи. Реализация живёт в `:app`. */
interface Navigator {
    /**
     * Поставить команду в очередь. Возвращается сразу; команды обрабатываются строго по порядку,
     * каждая проходит цепочку middleware.
     */
    fun execute(command: NavigationCommand)

    /**
     * Открыть [key] и дождаться результата.
     *
     * Завершается успешно, когда экран вызвал [finishWithResult]; иначе — [NavigationResultException]:
     * - `Cancelled` — экран закрыли без результата (Back, BackTo, NewRoot…);
     * - `NotNavigated` — команда не дошла до стека (middleware её отбросил или заменил, например auth-redirect);
     * - `AlreadyPending` — результат для точно такого же ключа уже ожидается.
     *
     * Ожидающие результаты живут только в памяти и не переживают смерть процесса.
     * Вызывать с main-потока (из viewModelScope — так и есть).
     */
    suspend fun <T : Any> forResult(key: ResultKey<T>): Result<T>

    /** Отдать результат вызывающему и закрыть экран [key]. */
    fun <T : Any> finishWithResult(key: ResultKey<T>, result: T)
}

sealed class NavigationResultException(message: String) : Exception(message) {
    class Cancelled(key: NavKey) : NavigationResultException("$key was closed without a result")

    class NotNavigated(key: NavKey) :
        NavigationResultException("Navigation to $key did not happen (dropped or redirected by middleware)")

    class AlreadyPending(key: NavKey) : NavigationResultException("A result for $key is already awaited")
}
