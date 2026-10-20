package dev.navplay.feature.profile.impl

import androidx.navigation3.runtime.EntryProviderScope
import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.NavKey
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.profile.api.ProfileNavKey

class ProfileEntryContributor() : NavEntryContributor {
    override fun register(scope: EntryProviderScope<NavKey>) {
        scope.entry<ProfileNavKey> { key -> ProfileScreen(userId = key.userId) }
    }
}

/** Feature-owned serialization; AppContainer explicitly registers the feature. */
object ProfileNavigationModule {
    fun navKeySerializers(): NavKeySerializers =
        navKeys { subclass(ProfileNavKey::class, ProfileNavKey.serializer()) }
}
