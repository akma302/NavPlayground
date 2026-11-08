package dev.navplay.feature.feed.impl

import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.ScreenRegistry
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.feed.api.FeedNavKey

class FeedEntryContributor() : NavEntryContributor {
    override fun register(scope: ScreenRegistry) {
        scope.entry(FeedNavKey::class) { FeedScreen() }
    }
}

/** Feature-owned serialization; AppContainer explicitly registers the feature. */
object FeedNavigationModule {
    fun navKeySerializers(): NavKeySerializers =
        navKeys { subclass(FeedNavKey::class, FeedNavKey.serializer()) }
}
