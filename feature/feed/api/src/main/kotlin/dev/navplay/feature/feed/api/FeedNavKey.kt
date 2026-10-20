package dev.navplay.feature.feed.api

import dev.navplay.core.navigation.NavKey
import kotlinx.serialization.Serializable

/** Стартовый экран и точка возврата «домой». */
@Serializable
data object FeedNavKey : NavKey
