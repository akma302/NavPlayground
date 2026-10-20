package dev.navplay.feature.feed.impl

import androidx.navigation3.runtime.EntryProviderScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.navplay.core.navigation.DeepLinkHandler
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.feed.api.FeedNavKey
import javax.inject.Inject

class FeedEntryContributor @Inject constructor() : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<FeedNavKey> { FeedScreen() }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FeedNavigationModule {
    @Binds
    @IntoSet
    abstract fun entries(impl: FeedEntryContributor): NavEntryContributor

    @Binds
    @IntoSet
    abstract fun deepLink(impl: FeedDeepLinkHandler): DeepLinkHandler

    companion object {
        @Provides
        @IntoSet
        fun navKeySerializers(): NavKeySerializers =
            navKeys { subclass(FeedNavKey::class, FeedNavKey.serializer()) }
    }
}
