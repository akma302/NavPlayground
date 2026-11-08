package dev.navplay.feature.profile.impl

import dev.navplay.core.navigation.NavEntryContributor
import dev.navplay.core.navigation.ScreenRegistry
import dev.navplay.core.navigation.NavKeySerializers
import dev.navplay.core.navigation.navKeys
import dev.navplay.feature.profile.api.ProfileNavKey

class ProfileEntryContributor() : NavEntryContributor {
    override fun register(scope: ScreenRegistry) {
        scope.entry(ProfileNavKey::class) { key -> ProfileScreen(userId = key.userId) }
    }
}

/** Feature-owned serialization; AppContainer explicitly registers the feature. */
object ProfileNavigationModule {
    fun navKeySerializers(): NavKeySerializers =
        navKeys { subclass(ProfileNavKey::class, ProfileNavKey.serializer()) }
}
