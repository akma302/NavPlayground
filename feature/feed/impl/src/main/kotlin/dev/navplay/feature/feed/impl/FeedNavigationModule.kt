package dev.navplay.feature.feed.impl

import androidx.navigation3.runtime.EntryProviderScope
import dev.navplay.core.navigation.DeepLinkHandler
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.feed.api.FeedNavKey
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

class FeedEntryContributor() : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<FeedNavKey> { FeedScreen() }
    }
}

/** Feature-owned registrations. Qualifiers keep collection contributions distinct. */
object FeedNavigationModule {
    val module: Module = module {
        factory<NavEntryContributor>(named("feed.entries")) { FeedEntryContributor() }
        factory<NavKeySerializers>(named("feed.serializers")) { navKeySerializers() }
        factory<DeepLinkHandler>(named("feed.deepLink")) { FeedDeepLinkHandler() }
        viewModelOf(::FeedViewModel)
    }

    fun navKeySerializers(): NavKeySerializers =
        navKeys { subclass(FeedNavKey::class, FeedNavKey.serializer()) }
}
