package dev.navplay.core.navigation

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

/**
 * Фичи вносят связь ключа с UI. Навигационный движок и управление временем жизни остаются в app.
 */
fun interface NavEntryContributor {
    fun register(scope: ScreenRegistry)
}

class ScreenRegistry(contributors: Iterable<NavEntryContributor>) {
    private val screens = mutableMapOf<KClass<out NavKey>, @Composable (NavKey) -> Unit>()

    init {
        contributors.forEach { it.register(this) }
    }

    fun <K : NavKey> entry(type: KClass<K>, content: @Composable (K) -> Unit) {
        check(type !in screens) { "Screen already registered for ${type.qualifiedName}" }
        screens[type] = { key ->
            // get выбирает функцию строго по классу ключа, поэтому приведение безопасно.
            @Suppress("UNCHECKED_CAST")
            content(key as K)
        }
    }

    operator fun get(key: NavKey): (@Composable (NavKey) -> Unit)? = screens[key::class]
}
