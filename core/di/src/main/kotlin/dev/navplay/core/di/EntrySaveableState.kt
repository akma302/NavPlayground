package dev.navplay.core.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope

/**
 * Decompose может показать одну запись в разных местах дерева анимации. Позиционный ключ Compose
 * в этих местах может отличаться. Сохраняем значение под именем в реестре конкретной записи,
 * чтобы, например, скрытый MediaPicker восстановил выбор, став первым видимым экраном после запуска.
 */
@Composable
fun <T : Any, S : Any> rememberEntrySaveable(name: String, saver: Saver<T, S>, init: () -> T): T {
    val registry = LocalSaveableStateRegistry.current
    val value = remember(registry, name) {
        @Suppress("UNCHECKED_CAST")
        val saved = registry?.consumeRestored(name) as S?
        saved?.let(saver::restore) ?: init()
    }
    DisposableEffect(registry, name, value) {
        val registration = registry?.registerProvider(name) {
            val scope = SaverScope(registry::canBeSaved)
            with(saver) { scope.save(value) }
        }
        onDispose { registration?.unregister() }
    }
    return value
}

@Composable
fun <T : Any> rememberEntryState(name: String, initial: T): MutableState<T> =
    rememberEntrySaveable(name, Saver(save = { it.value }, restore = { mutableStateOf(it) })) {
        mutableStateOf(initial)
    }
