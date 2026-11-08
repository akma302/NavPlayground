package dev.navplay.core.navigation

/**
 * Команды навигации — обобщённые данные. Модуль навигации не знает ни одной фичи:
 * семантика («открыть профиль») живёт в фиче, которая кладёт в команду свой [NavKey].
 */
sealed interface NavigationCommand {
    data class Forward(val key: NavKey) : NavigationCommand
    data class Replace(val key: NavKey) : NavigationCommand
    data class NewRoot(val key: NavKey) : NavigationCommand

    /** Выбирает историю раздела. Возобновление после входа также закрывает экран входа. */
    data class SelectSection(val key: NavKey, val fromSignIn: Boolean = false) : NavigationCommand

    /** Завершает сессию и очищает закрытую историю в одной операции навигатора. */
    data object Logout : NavigationCommand

    data object Back : NavigationCommand

    /** Вернуться к ближайшему (с конца) экрану [key]. [inclusive] = true — убрать и сам [key]. */
    data class BackTo(val key: NavKey, val inclusive: Boolean = false) : NavigationCommand

    data class DeepLink(
        val uri: String,
        val source: DeepLinkSource = DeepLinkSource.External,
    ) : NavigationCommand
}

enum class DeepLinkSource {
    /** Пуш, внешняя ссылка, intent из другого приложения — недоверенный источник. */
    External,

    /** Ссылка, сформированная самим приложением. */
    Internal,
}

/** Цель для middleware: запись экрана или корень выбираемого раздела. */
val NavigationCommand.targetKey: NavKey?
    get() = when (this) {
        is NavigationCommand.Forward -> key
        is NavigationCommand.Replace -> key
        is NavigationCommand.NewRoot -> key
        is NavigationCommand.SelectSection -> key
        else -> null
    }

/** Подмена цели выбора раздела открывает экран в текущем разделе, не переключая историю. */
fun NavigationCommand.withTargetKey(newKey: NavKey): NavigationCommand = when (this) {
    is NavigationCommand.Forward -> copy(key = newKey)
    is NavigationCommand.Replace -> copy(key = newKey)
    is NavigationCommand.NewRoot -> copy(key = newKey)
    is NavigationCommand.SelectSection -> NavigationCommand.Forward(newKey)
    else -> this
}
