package dev.navplay.core.navigation

import kotlinx.serialization.Serializable

/**
 * Экран-заглушка: «фича недоступна». Им FeatureFlagMiddleware заменяет ключ выключенной фичи.
 * Сам экран и сериализатор регистрирует `:app`.
 */
@Serializable
data class StubNavKey(val feature: String, val reason: String) : NavKey
