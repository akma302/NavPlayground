package dev.navplay.feature.profile.impl

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
import dev.navplay.feature.profile.api.ProfileNavKey
import javax.inject.Inject

class ProfileEntryContributor @Inject constructor() : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<ProfileNavKey> { key -> ProfileScreen(userId = key.userId) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileNavigationModule {
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
