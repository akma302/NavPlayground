package dev.navplay.app.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.navplay.core.di.AppScope
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavigationCommand
import dev.navplay.core.navigation.NavigationLog
import dev.navplay.core.navigation.NavigationMiddleware
import dev.navplay.core.navigation.NavigationPipeline
import dev.navplay.core.navigation.NavigationResultException
import dev.navplay.core.navigation.Navigator
import dev.navplay.core.navigation.RequiresAuth
import dev.navplay.core.navigation.ResultKey
import dev.navplay.core.navigation.targetKey
import dev.navplay.feature.auth.api.AuthNavKey
import dev.navplay.feature.auth.api.SessionManager
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Владеет историями разделов и текущим выбором. Если хранить только видимый стек, переход
 * в другой раздел либо уничтожит прежнюю историю, либо превратит вкладки в цепочку Back.
 *
 * Как это работает:
 * 1. [execute] кладёт команду в очередь и возвращается сразу;
 * 2. один воркер берёт команды по одной (строгий порядок, нет гонок) и прогоняет через цепочку middleware;
 * 3. [dispatch] применяет итоговую команду к нужной истории. [initialize] загружает начальное состояние.
 *
 * Всё изменение стека происходит на `scope` (в приложении — `Dispatchers.Main.immediate`), поэтому
 * `SnapshotStateList` пишется с main-потока. [forResult] и [finishWithResult] тоже вызываются с него.
 */
@SingleIn(AppScope::class)
@Inject
class NavigatorImpl(
    middleware: Set<@JvmSuppressWildcards NavigationMiddleware>,
    private val log: NavigationLog,
    @NavigationScope scope: CoroutineScope,
    private val sessions: SessionManager,
) : Navigator {

    private val home = mutableStateListOf<NavKey>()
    private val requests = mutableStateListOf<NavKey>()
    var selectedSection by mutableStateOf(AppSection.Home)
        private set

    private val stack get() = mutableStack(selectedSection)
    val backStack: List<NavKey> get() = stack

    fun backStack(section: AppSection): List<NavKey> = mutableStack(section)

    private fun mutableStack(section: AppSection): MutableList<NavKey> = when (section) {
        AppSection.Home -> home
        AppSection.Requests -> requests
    }

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

    /**
     * При повороте process-scoped навигатор уже жив. При холодном старте Bundle содержит маршруты,
     * но не доказывает право их открыть. В гостевой сессии восстанавливаем только публичный
     * префикс Home: экран после защищённого родителя тоже может зависеть от его контекста.
     */
    internal fun initialize(saved: SectionHistory? = null) {
        if (home.isNotEmpty()) return

        val authorized = sessions.isAuthorized
        val savedHome = saved?.home?.takeIf { it.firstOrNull() == AppSection.Home.root }
            ?: listOf(AppSection.Home.root)
        home.addAll(if (authorized) savedHome else savedHome.takeWhile { it !is RequiresAuth })

        if (authorized && saved != null && saved.requests.firstOrNull() == AppSection.Requests.root) {
            requests.addAll(saved.requests)
            selectedSection = saved.selected
        }
    }

    internal fun saveHistory() = SectionHistory(selectedSection, home.toList(), requests.toList())

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

    /** Очередь задаёт порядок даже между выбором раздела, Back и очисткой при logout. */
    private fun dispatch(command: NavigationCommand) {
        val targetSection = AppSection.entries.firstOrNull { it.root == command.targetKey }
        when (command) {
            is NavigationCommand.SelectSection -> {
                val section = targetSection ?: return
                if (command.fromSignIn) {
                    // Back мог уже отменить вход. Не переключаем раздел по запоздавшему продолжению.
                    val signIn = AuthNavKey(returnTo = command.key, selectSection = true)
                    if (stack.lastOrNull() != signIn) return
                    popTo(stack, stack.size - 1)
                }
                selectSection(section)
            }

            NavigationCommand.Logout -> {
                sessions.logout()
                // Requests может быть не на экране. Очистка только активного стека показала бы
                // карточку предыдущей сессии при следующем входе.
                popTo(requests, 0)
                if (home.firstOrNull() == AppSection.Home.root) {
                    popTo(home, 1)
                } else {
                    popTo(home, 0)
                    home.add(AppSection.Home.root)
                }
                selectedSection = AppSection.Home
            }

            is NavigationCommand.Forward -> {
                if (targetSection == null) stack.add(command.key) else selectSection(targetSection)
            }

            is NavigationCommand.Replace -> {
                if (targetSection == null) {
                    // Корень раздела — точка Back и повторного выбора. Replace не убирает его.
                    if (stack.size > 1) popTo(stack, stack.size - 1)
                    stack.add(command.key)
                } else {
                    selectSection(targetSection)
                }
            }

            is NavigationCommand.NewRoot -> {
                val section = targetSection ?: selectedSection
                val target = mutableStack(section)
                popTo(target, 0)
                target.add(command.key)
                selectedSection = section
            }

            NavigationCommand.Back -> {
                if (stack.size > 1) {
                    popTo(stack, stack.size - 1)
                } else if (selectedSection == AppSection.Requests) {
                    // Back от корня ведёт к стартовому разделу, а не повторяет историю тапов.
                    selectedSection = AppSection.Home
                } else {
                    log.log("nav", "Back ignored: ${stack.lastOrNull()} is the root")
                }
            }

            is NavigationCommand.BackTo -> {
                // Существующий переход из профиля к Feed работает и после внешней ссылки,
                // открывшей профиль поверх «Заявок».
                val target = if (command.key == AppSection.Home.root) home else stack
                val index = target.lastIndexOf(command.key)
                val keep = if (command.inclusive) index else index + 1
                when {
                    index < 0 -> log.log("nav", "BackTo ignored: ${command.key} is not in the stack")
                    keep == 0 -> log.log("nav", "BackTo ignored: it would leave the stack empty")
                    else -> {
                        popTo(target, keep)
                        if (target === home) selectedSection = AppSection.Home
                    }
                }
            }

            is NavigationCommand.DeepLink ->
                log.log("nav", "unresolved ${command.uri} reached the stack (DeepLinkResolutionMiddleware missing?)")
        }
    }

    private fun selectSection(section: AppSection) {
        val target = mutableStack(section)
        if (section == selectedSection) {
            // Повторный выбор — явное намерение вернуться к корню. Оставляем ту же запись,
            // чтобы её ViewModel и сохранённое состояние не пришлось создавать заново.
            popTo(target, 1)
        } else {
            if (target.isEmpty()) target.add(section.root)
            selectedSection = section
        }
    }

    /** Оставляет в стеке первые [size] экранов; ждущие результата убранные экраны получают `Cancelled`. */
    private fun popTo(stack: MutableList<NavKey>, size: Int) {
        while (stack.size > size) {
            val removed = stack.removeAt(stack.lastIndex)
            pending.remove(removed)?.completeExceptionally(NavigationResultException.Cancelled(removed))
        }
    }
}
