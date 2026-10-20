package dev.navplay.core.navigation

/**
 * Перехватчик команд. Решает кучу «неудобных» кейсов в одном месте: auth-guard, фича-флаги,
 * аналитика, защита от двойного клика, валидация deep link.
 *
 * Контракт: middleware либо вызывает [proceed] (с исходной или заменённой командой), либо НЕ вызывает
 * его — тогда команда отменена. Вызывать [proceed] можно максимум один раз.
 *
 * Порядок задаёт [order] (меньше — ближе к вызывающему). DI-мультибайндинг `Set` порядка не гарантирует,
 * поэтому порядок — явный. Одинаковые [order] -> порядок между ними не определён.
 */
interface NavigationMiddleware {
    val order: Int get() = Order.DEFAULT

    suspend fun intercept(
        command: NavigationCommand,
        proceed: suspend (NavigationCommand) -> Unit,
    )

    /** Принятые в проекте слои; между ними есть зазоры под новые middleware. */
    object Order {
        const val LOGGING = -100
        const val THROTTLE = 0
        const val DEFAULT = 0
        const val DEEP_LINK_VALIDATION = 10
        const val DEEP_LINK_RESOLUTION = 15
        const val FEATURE_FLAGS = 20
        const val AUTH = 30
        const val ANALYTICS = 40
    }
}
