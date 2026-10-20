package dev.navplay.feature.feed.impl

import androidx.navigation3.runtime.EntryProviderScope
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.feed.api.FeedNavKey

class FeedEntryContributor() : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<FeedNavKey> { FeedScreen() }
    }
}

/** Feature-owned serialization; AppContainer explicitly registers the feature. */
object FeedNavigationModule {
    fun navKeySerializers(): NavKeySerializers =
        navKeys { subclass(FeedNavKey::class, FeedNavKey.serializer()) }
}
