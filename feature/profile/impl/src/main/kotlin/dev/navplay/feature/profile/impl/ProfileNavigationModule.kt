package dev.navplay.feature.profile.impl

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
import dev.navplay.feature.profile.api.ProfileNavKey
import javax.inject.Inject

class ProfileEntryContributor @Inject constructor(
    private val factory: ViewModelProvider.Factory,
) : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<ProfileNavKey> { key -> ProfileScreen(key.userId, viewModel(factory = factory)) }
    }
}

@Module
abstract class ProfileNavigationModule {
    @Binds
    @IntoMap
    @ClassKey(ProfileViewModel::class)
    abstract fun viewModel(impl: ProfileViewModel): ViewModel

    @Binds
    @IntoSet
    abstract fun entries(impl: ProfileEntryContributor): NavEntryContributor

    @Binds
    @IntoSet
    abstract fun deepLink(impl: ProfileDeepLinkHandler): DeepLinkHandler

    companion object {
        @Provides
        @IntoSet
        fun navKeySerializers(): NavKeySerializers =
            navKeys { subclass(ProfileNavKey::class, ProfileNavKey.serializer()) }
    }
}
