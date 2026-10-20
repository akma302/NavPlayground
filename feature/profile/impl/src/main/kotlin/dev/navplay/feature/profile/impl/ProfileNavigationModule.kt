package dev.navplay.feature.profile.impl

import androidx.navigation3.runtime.EntryProviderScope
import dev.navplay.core.di.AppScope
import dev.navplay.core.navigation.DeepLinkHandler
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.profile.api.ProfileNavKey
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@Inject
class ProfileEntryContributor() : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<ProfileNavKey> { key -> ProfileScreen(userId = key.userId) }
    }
}

@BindingContainer
@ContributesTo(AppScope::class)
abstract class ProfileNavigationModule {
    @Binds
    @IntoSet
    abstract val ProfileEntryContributor.entries: NavEntryContributor

    @Binds
    @IntoSet
    abstract val ProfileDeepLinkHandler.deepLink: DeepLinkHandler

    companion object {
        @Provides
        @IntoSet
        fun navKeySerializers(): NavKeySerializers =
            navKeys { subclass(ProfileNavKey::class, ProfileNavKey.serializer()) }
    }
}
