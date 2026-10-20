package dev.navplay.feature.feed.impl

import dev.navplay.core.navigation.DeepLinkHandler
import dev.navplay.core.navigation.NavKey
import dev.navplay.feature.feed.api.FeedNavKey
import javax.inject.Inject

/** `navplay://feed` — «домой». Безопасно открывать из любого источника. */
class FeedDeepLinkHandler @Inject constructor() : DeepLinkHandler {
    override val pattern = "navplay://feed"

    override fun build(args: Map<String, String>): NavKey = FeedNavKey
}
