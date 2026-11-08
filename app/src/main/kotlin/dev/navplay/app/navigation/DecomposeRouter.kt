package dev.navplay.app.navigation

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.children.ChildNavState
import com.arkivanov.decompose.router.children.NavState
import com.arkivanov.decompose.router.children.SimpleChildNavState
import com.arkivanov.decompose.router.children.SimpleNavigation
import com.arkivanov.decompose.router.children.children
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import dev.navplay.core.navigation.NavKey

/** Одинаковый маршрут можно открыть повторно. ID различает записи для Decompose и Compose. */
internal data class RouteEntry(val id: Long, val key: NavKey)

internal data class SectionNavState(
    val selected: AppSection,
    val home: List<RouteEntry>,
    val requests: List<RouteEntry>,
    val nextId: Long,
) : NavState<RouteEntry> {
    val active: RouteEntry get() = stack(selected).last()

    fun stack(section: AppSection): List<RouteEntry> = when (section) {
        AppSection.Home -> home
        AppSection.Requests -> requests
    }

    // Только видимая вершина возобновлена. Скрытые компоненты живут и держат свои ViewModelStore.
    override val children: List<ChildNavState<RouteEntry>>
        get() = (home + requests).map { entry ->
            val status = if (entry == active) ChildNavState.Status.RESUMED else ChildNavState.Status.CREATED
            SimpleChildNavState(configuration = entry, status = status)
        }
}

internal class ScreenComponent(context: ComponentContext) {
    val owner: ViewModelStoreOwner = context.instanceKeeper.getOrCreate(::ScreenModels)

    private class ScreenModels : InstanceKeeper.Instance, ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()

        override fun onDestroy() = viewModelStore.clear()
    }
}

internal data class SectionScreens(
    val state: SectionNavState,
    val children: List<Child.Created<RouteEntry, ScreenComponent>>,
) {
    /** Home остаётся предыдущей сценой для жеста Back на корне Requests. */
    fun displayStack(): ChildStack<RouteEntry, ScreenComponent> {
        val visibleHistory = if (state.selected == AppSection.Home) children.take(state.home.size) else children
        return ChildStack(active = visibleHistory.last(), backStack = visibleHistory.dropLast(1))
    }
}

/**
 * Decompose выполняет diff обеих историй как одной операции. Обычный ChildStack требует корень,
 * тогда как Requests должен быть пустым у гостя, а logout должен удалить его вместе со сменой вкладки.
 * Bundle читается с проверкой сессии до запуска роутера, поэтому автоматическое восстановление
 * конфигураций через StateKeeper здесь отключено.
 */
internal class DecomposeRouter(
    context: ComponentContext,
    initial: SectionNavState,
) {
    private val navigation = SimpleNavigation<SectionNavState>()
    val backHandler = context.backHandler
    val screens = context.children(
        source = navigation,
        stateSerializer = null,
        initialState = { initial },
        key = "sections",
        navTransformer = { _, state -> state },
        stateMapper = { state, children ->
            SectionScreens(state, children.map { child ->
                check(child is Child.Created)
                child
            })
        },
        childFactory = { _, childContext -> ScreenComponent(childContext) },
    )
    val state: SectionNavState get() = screens.value.state

    fun navigate(next: SectionNavState) = navigation.navigate(next)
}
