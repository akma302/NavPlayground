package dev.navplay.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Наблюдаемый журнал навигации: в него пишут навигатор и middleware («throttle: dropped duplicate»,
 * «auth: redirect to login»…). В playground его показывает экран Feed — видно, как сработала цепочка.
 */
class NavigationLog(
    private val capacity: Int = 80,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    data class Entry(val timeMillis: Long, val source: String, val message: String)

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    fun log(source: String, message: String) {
        val entry = Entry(clock(), source, message)
        _entries.update { (it + entry).takeLast(capacity) }
    }

    fun clear() {
        _entries.value = emptyList()
    }
}
