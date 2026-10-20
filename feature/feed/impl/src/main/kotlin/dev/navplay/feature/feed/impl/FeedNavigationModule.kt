package dev.navplay.feature.feed.impl

import androidx.navigation3.runtime.EntryProviderScope
import dev.navplay.core.di.AppScope
import dev.navplay.core.navigation.DeepLinkHandler
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.feed.api.FeedNavKey
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@Inject
class FeedEntryContributor() : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<FeedNavKey> { FeedScreen() }
    }
}

@BindingContainer
@ContributesTo(AppScope::class)
abstract class FeedNavigationModule {
    @Binds
    @IntoSet
    abstract val FeedEntryContributor.entries: NavEntryContributor

    @Binds
    @IntoSet
    abstract val FeedDeepLinkHandler.deepLink: DeepLinkHandler

    companion object {
        @Provides
        @IntoSet
        fun navKeySerializers(): NavKeySerializers =
            navKeys { subclass(FeedNavKey::class, FeedNavKey.serializer()) }
    }
}
