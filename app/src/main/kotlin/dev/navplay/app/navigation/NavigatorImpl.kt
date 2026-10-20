package dev.navplay.app.navigation

import androidx.compose.runtime.mutableStateListOf
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.core.navigation.NavigationMiddleware
import dev.navplay.core.navigation.NavigationPipeline
import dev.navplay.core.navigation.NavigationResultException
import dev.navplay.core.navigation.Navigator
import dev.navplay.core.navigation.ResultKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Реализация [Navigator]. Живёт в `:app` и **владеет бэкстеком** — это модель Navigation 3:
 * стек — обычный `SnapshotStateList`, а не внутренности библиотеки. Отсюда бесплатно:
 * персист (все ключи сериализуемы), вложенные стеки для bottom navigation, multi-pane.
 *
 * Как это работает:
 * 1. [execute] кладёт команду в очередь и возвращается сразу;
 * 2. один воркер берёт команды по одной (строгий порядок, нет гонок) и прогоняет через цепочку middleware;
 * 3. «сердцевина» цепочки — [dispatch] — единственное место, где меняется стек.
 *
 * Всё изменение стека происходит на `scope` (в приложении — `Dispatchers.Main.immediate`), поэтому
 * `SnapshotStateList` пишется с main-потока. [forResult] и [finishWithResult] тоже вызываются с него.
 */
class NavigatorImpl(
    private val stack: MutableList<NavKey>,
    middleware: Collection<NavigationMiddleware>,
    private val log: NavigationLog,
    scope: CoroutineScope,
) : Navigator {

    /** Конструктор для DI: бэкстек — Compose-состояние, чтобы NavDisplay перерисовывался сам. */
    constructor(
        middleware: Set<@JvmSuppressWildcards NavigationMiddleware>,
        log: NavigationLog,
        scope: CoroutineScope,
    ) : this(mutableStateListOf(), middleware, log, scope)

    /** Только для чтения снаружи. Читать можно из Compose — подписка на снапшоты работает. */
    val backStack: List<NavKey> get() = stack

    private class Request(
        val command: NavigationCommand? = null,
        /** Если запрос пришёл из [forResult] — ключ, чей результат ждут. */
        val resultKey: NavKey? = null,
        /** Если не null — это барьер ([awaitIdle]), а не команда. */
        val barrier: CompletableDeferred<Unit>? = null,
    )

    private val queue = Channel<Request>(Channel.UNLIMITED)
    private val pending = mutableMapOf<NavKey, CompletableDeferred<Any>>()
    private val pipeline = NavigationPipeline(middleware) { command -> dispatch(command) }

    init {
        scope.launch {
            for (request in queue) process(request)
        }
    }

    // ---------------------------------------------------------------- Navigator

    override fun execute(command: NavigationCommand) {
        queue.trySend(Request(command = command))
    }

    override suspend fun <T : Any> forResult(key: ResultKey<T>): Result<T> {
        if (key in pending) return Result.failure(NavigationResultException.AlreadyPending(key))

        val waiter = CompletableDeferred<Any>()
        pending[key] = waiter
        queue.trySend(Request(command = NavigationCommand.Forward(key), resultKey = key))
        return try {
            @Suppress("UNCHECKED_CAST")
            Result.success(waiter.await() as T)
        } catch (e: NavigationResultException) {
            Result.failure(e)
        } finally {
            // Вызывающий отменён (например, ушёл viewModelScope) — не копим «мёртвых» ожидающих.
            if (pending[key] === waiter) pending.remove(key)
        }
    }

    override fun <T : Any> finishWithResult(key: ResultKey<T>, result: T) {
        // Сначала отдаём результат, потом закрываем экран: иначе закрытие посчитается отменой.
        pending.remove(key)?.complete(result)
        execute(NavigationCommand.BackTo(key, inclusive = true))
    }

    // ---------------------------------------------------------------- Хуки для :app

    /** Если стек пуст — кладёт стартовый экран. Вызывать с main-потока (Activity.onCreate). */
    fun ensureRoot(root: NavKey) {
        if (stack.isEmpty()) stack.add(root)
    }

    /** Восстановление после смерти процесса. Работает только на пустом стеке (при повороте экрана стек жив). */
    fun restore(keys: List<NavKey>) {
        if (stack.isEmpty() && keys.isNotEmpty()) stack.addAll(keys)
    }

    /** Завершается, когда все поставленные ранее команды обработаны. Нужно тестам и отладке. */
    suspend fun awaitIdle() {
        val barrier = CompletableDeferred<Unit>()
        queue.trySend(Request(barrier = barrier))
        barrier.await()
    }

    // ---------------------------------------------------------------- Внутренности

    private suspend fun process(request: Request) {
        request.barrier?.let {
            it.complete(Unit)
            return
        }
        val command = request.command ?: return
        try {
            pipeline.run(command)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Упавший middleware не должен убивать воркер: иначе навигация «зависнет» до перезапуска.
            log.log("nav", "command failed: $command ($e)")
        }
        request.resultKey?.let { key ->
            val waiter = pending[key]
            if (waiter != null && stack.lastOrNull() != key) {
                // Команда не дошла до стека в исходном виде: её отбросили или заменили (auth, флаги…).
                pending.remove(key)
                waiter.completeExceptionally(NavigationResultException.NotNavigated(key))
            }
        }
    }

    /** «Сердцевина» цепочки: единственное место, где меняется бэкстек. */
    private fun dispatch(command: NavigationCommand) {
        when (command) {
            is NavigationCommand.Forward -> stack.add(command.key)

            is NavigationCommand.Replace -> {
                if (stack.isNotEmpty()) popTo(stack.size - 1)
                stack.add(command.key)
            }

            is NavigationCommand.NewRoot -> {
                popTo(0)
                stack.add(command.key)
            }

            NavigationCommand.Back -> {
                if (stack.size > 1) {
                    popTo(stack.size - 1)
                } else {
                    log.log("nav", "Back ignored: ${stack.lastOrNull()} is the root")
                }
            }

            is NavigationCommand.BackTo -> {
                val index = stack.lastIndexOf(command.key)
                val keep = if (command.inclusive) index else index + 1
                when {
                    index < 0 -> log.log("nav", "BackTo ignored: ${command.key} is not in the stack")
                    keep == 0 -> log.log("nav", "BackTo ignored: it would leave the stack empty")
                    else -> popTo(keep)
                }
            }

            is NavigationCommand.DeepLink ->
                log.log("nav", "unresolved ${command.uri} reached the stack (DeepLinkResolutionMiddleware missing?)")
        }
    }

    /** Оставляет в стеке первые [size] экранов; ждущие результата убранные экраны получают `Cancelled`. */
    private fun popTo(size: Int) {
        while (stack.size > size) {
            val removed = stack.removeAt(stack.lastIndex)
            pending.remove(removed)?.completeExceptionally(NavigationResultException.Cancelled(removed))
        }
    }
}
