package dev.navplay.core.navigation

/** Абстракция аналитики; реализация — в `:app`. Нужна, чтобы все переходы логировались в одном месте. */
interface NavigationAnalytics {
    fun trackNavigation(destination: String, command: String)
}
