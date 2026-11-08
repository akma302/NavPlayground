package dev.navplay.app.navigation

import com.arkivanov.decompose.ComponentContext
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
 * 3. [dispatch] публикует следующее состояние в Decompose. Изменение раздела и очистка обеих
 *    историй при выходе применяются одной транзакцией.
 *
 * Очередь и ожидания результата живут в приложении, а Decompose-роутер принадлежит Activity.
 * Между хостами хранится только снимок маршрутов, без ссылки на уничтоженную Activity.
 */
class NavigatorImpl(
    middleware: Set<NavigationMiddleware>,
    private val log: NavigationLog,
    scope: CoroutineScope,
    private val sessions: SessionManager,
) : Navigator {

    private var router: DecomposeRouter? = null
    private var detachedState: SectionNavState? = null
    private val state get() = checkNotNull(router?.state ?: detachedState) { "Attach the navigator before use" }

    val selectedSection: AppSection get() = state.selected
    val backStack: List<NavKey> get() = backStack(selectedSection)

    fun backStack(section: AppSection): List<NavKey> = state.stack(section).map(RouteEntry::key)

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
     * Bundle содержит маршруты, но не доказывает право их открыть. При холодном гостевом старте
     * оставляем публичный префикс Home до того, как Decompose создаст дочерние компоненты.
     */
    internal fun createRouter(
        context: ComponentContext,
        saved: SectionHistory? = null,
        entryIds: LongArray? = null,
    ): DecomposeRouter = DecomposeRouter(context, detachedState ?: restoredState(saved, entryIds))

    internal fun attach(host: DecomposeRouter) {
        detachedState?.takeIf { it != host.state }?.let(host::navigate)
        router = host
        detachedState = null
    }

    internal fun detach(host: DecomposeRouter) {
        if (router !== host) return
        detachedState = host.state
        router = null
    }

    private fun restoredState(saved: SectionHistory?, entryIds: LongArray?): SectionNavState {
        val authorized = sessions.isAuthorized
        val savedHome = saved?.home?.takeIf { it.firstOrNull() == AppSection.Home.root }
        val home = savedHome ?: listOf(AppSection.Home.root)
        val requests = saved?.requests.orEmpty().takeIf {
            authorized && it.firstOrNull() == AppSection.Requests.root
        }.orEmpty()
        // ID сохраняется вместе с ключом, чтобы Compose нашёл rememberSaveable после смерти процесса.
        // Старый Bundle без ID всё ещё восстанавливает маршруты.
        val ids = entryIds?.takeIf { values ->
            savedHome != null && values.size == 1 + savedHome.size + saved?.requests.orEmpty().size &&
                values.drop(1).all { it > 0 && it < values.first() } &&
                values.toSet().size == values.size
        }
        var nextId = ids?.first() ?: 1L
        val visibleHome = if (authorized) home else home.takeWhile { it !is RequiresAuth }
        return SectionNavState(
            selected = if (requests.isNotEmpty()) saved?.selected ?: AppSection.Home else AppSection.Home,
            home = visibleHome.mapIndexed { i, key -> RouteEntry(ids?.get(1 + i) ?: nextId++, key) },
            requests = requests.mapIndexed { i, key ->
                RouteEntry(ids?.get(1 + home.size + i) ?: nextId++, key)
            },
            nextId = nextId,
        )
    }

    internal fun saveHistory() = SectionHistory(selectedSection, backStack(AppSection.Home), backStack(AppSection.Requests))

    internal fun saveEntryIds(): LongArray =
        longArrayOf(state.nextId) + (state.home + state.requests).map(RouteEntry::id).toLongArray()

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
            if (waiter != null && backStack.lastOrNull() != key) {
                // Команда не дошла до стека в исходном виде: её отбросили или заменили (auth, флаги…).
                pending.remove(key)
                waiter.completeExceptionally(NavigationResultException.NotNavigated(key))
            }
        }
    }

    /** Очередь задаёт порядок даже между выбором раздела, Back и очисткой при logout. */
    private fun dispatch(command: NavigationCommand) {
        val current = state
        var selected = current.selected
        var nextId = current.nextId
        val home = current.home.toMutableList()
        val requests = current.requests.toMutableList()
        val removed = mutableListOf<RouteEntry>()
        fun sectionStack(section: AppSection) = if (section == AppSection.Home) home else requests
        fun popTo(stack: MutableList<RouteEntry>, size: Int) {
            while (stack.size > size) removed += stack.removeAt(stack.lastIndex)
        }
        fun push(stack: MutableList<RouteEntry>, key: NavKey) {
            stack += RouteEntry(nextId++, key)
        }
        fun selectSection(section: AppSection) {
            val target = sectionStack(section)
            if (section == selected) {
                popTo(target, 1)
            } else {
                if (target.isEmpty()) push(target, section.root)
                selected = section
            }
        }

        val stack = sectionStack(selected)
        val targetSection = AppSection.entries.firstOrNull { it.root == command.targetKey }
        when (command) {
            is NavigationCommand.SelectSection -> {
                val section = targetSection ?: return
                if (command.fromSignIn) {
                    // Back мог уже отменить вход. Не переключаем раздел по запоздавшему продолжению.
                    val signIn = AuthNavKey(returnTo = command.key, selectSection = true)
                    if (stack.lastOrNull()?.key != signIn) return
                    popTo(stack, stack.size - 1)
                }
                selectSection(section)
            }

            NavigationCommand.Logout -> {
                sessions.logout()
                // Requests может быть не на экране. Очистка только активного стека показала бы
                // карточку предыдущей сессии при следующем входе.
                popTo(requests, 0)
                if (home.firstOrNull()?.key == AppSection.Home.root) {
                    popTo(home, 1)
                } else {
                    popTo(home, 0)
                    push(home, AppSection.Home.root)
                }
                selected = AppSection.Home
            }

            is NavigationCommand.Forward -> {
                if (targetSection == null) push(stack, command.key) else selectSection(targetSection)
            }

            is NavigationCommand.Replace -> {
                if (targetSection == null) {
                    // Корень раздела — точка Back и повторного выбора. Replace не убирает его.
                    if (stack.size > 1) popTo(stack, stack.size - 1)
                    push(stack, command.key)
                } else {
                    selectSection(targetSection)
                }
            }

            is NavigationCommand.NewRoot -> {
                val section = targetSection ?: selected
                val target = sectionStack(section)
                popTo(target, 0)
                push(target, command.key)
                selected = section
            }

            NavigationCommand.Back -> {
                if (stack.size > 1) {
                    popTo(stack, stack.size - 1)
                } else if (selected == AppSection.Requests) {
                    // Back от корня ведёт к стартовому разделу, а не повторяет историю тапов.
                    selected = AppSection.Home
                } else {
                    log.log("nav", "Back ignored: ${stack.lastOrNull()?.key} is the root")
                }
            }

            is NavigationCommand.BackTo -> {
                // Существующий переход из профиля к Feed работает и после внешней ссылки,
                // открывшей профиль поверх «Заявок».
                val target = if (command.key == AppSection.Home.root) home else stack
                val index = target.indexOfLast { it.key == command.key }
                val keep = if (command.inclusive) index else index + 1
                when {
                    index < 0 -> log.log("nav", "BackTo ignored: ${command.key} is not in the stack")
                    keep == 0 -> log.log("nav", "BackTo ignored: it would leave the stack empty")
                    else -> {
                        popTo(target, keep)
                        if (target === home) selected = AppSection.Home
                    }
                }
            }

            is NavigationCommand.DeepLink ->
                log.log("nav", "unresolved ${command.uri} reached the stack (DeepLinkResolutionMiddleware missing?)")
        }
        val next = SectionNavState(selected, home, requests, nextId)
        if (next == current) return
        removed.forEach { entry ->
            pending.remove(entry.key)?.completeExceptionally(NavigationResultException.Cancelled(entry.key))
        }
        val host = router
        if (host == null) detachedState = next else host.navigate(next)
    }
}
