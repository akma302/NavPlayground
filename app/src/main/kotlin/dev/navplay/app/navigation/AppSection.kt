package dev.navplay.app.navigation

import dev.navplay.core.navigation.NavKey
import dev.navplay.feature.feed.api.FeedNavKey
import dev.navplay.feature.requests.api.RequestsNavKey

enum class AppSection(val root: NavKey, val label: String) {
    Home(FeedNavKey, "Home"),
    Requests(RequestsNavKey, "Requests"),
}

/** Снимок для Bundle. Выбор раздела нельзя восстановить только по двум непустым историям. */
internal data class SectionHistory(
    val selected: AppSection,
    val home: List<NavKey>,
    val requests: List<NavKey>,
)
