package dev.navplay.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface FeatureFlags {
    fun isEnabled(featureId: String): Boolean
}

/** Для playground: флаги можно переключать в рантайме (экран Feed). */
interface MutableFeatureFlags : FeatureFlags {
    /** Выключенные фичи. Всё, чего тут нет, считается включённым. */
    val disabled: StateFlow<Set<String>>

    fun setEnabled(featureId: String, enabled: Boolean)
}

class InMemoryFeatureFlags(initiallyDisabled: Set<String> = emptySet()) : MutableFeatureFlags {
    private val _disabled = MutableStateFlow(initiallyDisabled)
    override val disabled: StateFlow<Set<String>> = _disabled.asStateFlow()

    override fun isEnabled(featureId: String): Boolean = featureId !in _disabled.value

    override fun setEnabled(featureId: String, enabled: Boolean) {
        _disabled.update { if (enabled) it - featureId else it + featureId }
    }
}
