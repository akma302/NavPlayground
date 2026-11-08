package dev.navplay.feature.feed.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import dev.navplay.core.navigation.DeepLinkHandler
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.feed.api.FeedNavKey
import javax.inject.Inject

class FeedEntryContributor @Inject constructor(
    private val factory: ViewModelProvider.Factory,
) : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<FeedNavKey> { FeedScreen(viewModel(factory = factory)) }
    }
}

@Module
abstract class FeedNavigationModule {
    @Binds
    @IntoMap
    @ClassKey(FeedViewModel::class)
    abstract fun viewModel(impl: FeedViewModel): ViewModel

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
